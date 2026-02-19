package de.xenexes.gameserverbot.infrastructure.discord

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.right
import de.xenexes.gameserverbot.config.DiscordProperties
import de.xenexes.gameserverbot.domain.gameserver.GameServerEvent.GameServerCreatedEvent
import de.xenexes.gameserverbot.domain.gameserver.GameServerEvent.GameServerDeletedEvent
import de.xenexes.gameserverbot.domain.gameserver.GameServerEvent.GameServerStatusChangedEvent
import de.xenexes.gameserverbot.domain.player.PlayerEvent
import de.xenexes.gameserverbot.domain.player.PlayerListType
import de.xenexes.gameserverbot.ports.outbound.DiscordClientGateway
import de.xenexes.gameserverbot.ports.outbound.EmbedField
import de.xenexes.gameserverbot.ports.outbound.NotificationGateway
import de.xenexes.gameserverbot.ports.outbound.failure.DiscordFailure
import de.xenexes.gameserverbot.ports.outbound.failure.NotificationFailure
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Instant

@Component
@ConditionalOnProperty(
    name = ["gameserverbot.discord.enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
class DiscordNotificationAdapter(
    private val discordClientGateway: DiscordClientGateway,
    private val properties: DiscordProperties,
) : NotificationGateway {
    private val logger = KotlinLogging.logger {}

    override suspend fun notifyStatusChanged(event: GameServerStatusChangedEvent): Either<NotificationFailure, Unit> =
        either {
            val channelId = requireChannelId()

            logger.info {
                "Sending Discord notification for server ${event.aggregateId.value}: " +
                    "${event.previousStatus} -> ${event.newStatus}"
            }

            val notification = buildStatusNotification(event)
            sendEmbed(channelId, notification).bind()
        }

    override suspend fun notifyServerCreated(event: GameServerCreatedEvent): Either<NotificationFailure, Unit> =
        either {
            val channelId = requireChannelId()

            logger.info { "Sending Discord notification for server created: ${event.name}" }

            val notification =
                NotificationData(
                    title = "📋 Server Registered",
                    description = "A new game server has been registered for monitoring.",
                    colorInt = StatusColors.NEUTRAL.rgb,
                    fields =
                        listOf(
                            EmbedField(name = "Name", value = event.name, inline = true),
                            EmbedField(name = "Nitrado ID", value = event.nitradoId.value.toString(), inline = true),
                            EmbedField(name = "Registered By", value = event.createdBy.value, inline = true),
                        ),
                    timestamp = event.occurredAt,
                )
            sendEmbed(channelId, notification).bind()
        }

    override suspend fun notifyServerDeleted(event: GameServerDeletedEvent): Either<NotificationFailure, Unit> =
        either {
            val channelId = requireChannelId()

            logger.info { "Sending Discord notification for server deleted: ${event.aggregateId.value}" }

            val notification =
                NotificationData(
                    title = "🗑️ Server Removed",
                    description = "A game server has been removed from monitoring.",
                    colorInt = StatusColors.UNHEALTHY.rgb,
                    fields =
                        listOf(
                            EmbedField(name = "Server Name", value = event.serverName, inline = true),
                            EmbedField(name = "Removed By", value = event.deletedBy.value, inline = true),
                        ),
                    timestamp = event.occurredAt,
                )
            sendEmbed(channelId, notification).bind()
        }

    override suspend fun notifyPlayerListChanged(event: PlayerEvent): Either<NotificationFailure, Unit> =
        either {
            val channelId = requireChannelId()

            logger.info {
                "Sending Discord notification for player event on server ${event.aggregateId.serverId.value}"
            }

            val notification = buildPlayerNotification(event)
            sendEmbed(channelId, notification).bind()
        }

    private fun buildPlayerNotification(event: PlayerEvent): NotificationData =
        when (event) {
            is PlayerEvent.PlayerAddedToList ->
                buildListChangeNotification(
                    listType = event.listType,
                    added = true,
                    playerName = event.playerName,
                    identifier = event.identifier.value,
                    occurredAt = event.occurredAt,
                )

            is PlayerEvent.PlayerRemovedFromList ->
                buildListChangeNotification(
                    listType = event.listType,
                    added = false,
                    playerName = event.playerName,
                    identifier = event.identifier.value,
                    occurredAt = event.occurredAt,
                )
        }

    private fun buildListChangeNotification(
        listType: PlayerListType,
        added: Boolean,
        playerName: String,
        identifier: String,
        occurredAt: Instant,
    ): NotificationData =
        NotificationData(
            title = playerListTitle(listType, added),
            description = playerListDescription(playerName, listType, added),
            colorInt = playerListColor(listType, added),
            fields =
                listOf(
                    EmbedField(name = "Player", value = playerName, inline = true),
                    EmbedField(name = "List", value = formatListType(listType), inline = true),
                    EmbedField(name = "Identifier", value = identifier, inline = true),
                ),
            timestamp = occurredAt,
        )

    private fun playerListTitle(
        listType: PlayerListType,
        added: Boolean,
    ): String =
        when (listType) {
            PlayerListType.WHITELIST -> if (added) "✅ Player Whitelisted" else "➖ Player Removed from Whitelist"
            PlayerListType.BANLIST -> if (added) "🔨 Player Banned" else "🔓 Player Unbanned"
        }

    private fun playerListDescription(
        playerName: String,
        listType: PlayerListType,
        added: Boolean,
    ): String =
        when (listType) {
            PlayerListType.WHITELIST ->
                if (added) {
                    "Player **$playerName** has been added to the whitelist."
                } else {
                    "Player **$playerName** has been removed from the whitelist."
                }

            PlayerListType.BANLIST ->
                if (added) {
                    "Player **$playerName** has been banned."
                } else {
                    "Player **$playerName** has been unbanned."
                }
        }

    private fun playerListColor(
        listType: PlayerListType,
        added: Boolean,
    ): Int =
        when (listType) {
            PlayerListType.WHITELIST -> if (added) StatusColors.HEALTHY.rgb else StatusColors.MAINTENANCE.rgb
            PlayerListType.BANLIST -> if (added) StatusColors.UNHEALTHY.rgb else StatusColors.HEALTHY.rgb
        }

    private fun formatListType(listType: PlayerListType): String =
        when (listType) {
            PlayerListType.WHITELIST -> "Whitelist"
            PlayerListType.BANLIST -> "Banlist"
        }

    private fun buildStatusNotification(event: GameServerStatusChangedEvent): NotificationData {
        val title = determineTitle(event)
        val description = buildDescription(event)
        val color = determineColor(event)

        val fields =
            listOf(
                EmbedField(
                    name = "Server",
                    value = event.serverName,
                    inline = true,
                ),
                EmbedField(
                    name = "Previous Status",
                    value = formatStatus(event.previousStatus.name),
                    inline = true,
                ),
                EmbedField(
                    name = "New Status",
                    value = formatStatus(event.newStatus.name),
                    inline = true,
                ),
                EmbedField(
                    name = "Triggered By",
                    value = event.triggeredBy.value,
                    inline = true,
                ),
            )

        return NotificationData(
            title = title,
            description = description,
            colorInt = color,
            fields = fields,
            timestamp = event.occurredAt,
        )
    }

    private fun determineColor(event: GameServerStatusChangedEvent): Int =
        when {
            event.isStatusImprovement() -> StatusColors.HEALTHY.rgb
            event.isStatusDegradation() -> StatusColors.UNHEALTHY.rgb
            event.newStatus.isInMaintenance() -> StatusColors.MAINTENANCE.rgb
            else -> StatusColors.NEUTRAL.rgb
        }

    private fun determineTitle(event: GameServerStatusChangedEvent): String =
        when {
            event.isStatusImprovement() -> "🟢 Server Health Restored"
            event.isStatusDegradation() -> "🔴 Server Health Degraded"
            event.newStatus.isInMaintenance() -> "🔧 Server Maintenance"
            else -> "📊 Server Status Changed"
        }

    private fun buildDescription(event: GameServerStatusChangedEvent): String {
        val action =
            when {
                event.isStatusImprovement() -> "is now back online"
                event.isStatusDegradation() -> "has gone offline or entered an unhealthy state"
                event.newStatus.isInMaintenance() -> "is undergoing maintenance"
                else -> "has changed status"
            }
        return "The game server $action."
    }

    private fun formatStatus(status: String): String =
        status
            .replace("_", " ")
            .lowercase()
            .replaceFirstChar { it.uppercase() }

    private fun arrow.core.raise.Raise<NotificationFailure>.requireChannelId(): String {
        val channelId = properties.notificationChannelId?.takeIf { it.isNotBlank() }
        ensureNotNull(channelId) {
            logger.warn { "Discord notification channel ID not configured" }
            NotificationFailure.SendFailed("Notification channel ID not configured")
        }
        return channelId
    }

    private suspend fun sendEmbed(
        channelId: String,
        notification: NotificationData,
    ): Either<NotificationFailure, Unit> =
        discordClientGateway
            .sendEmbeddedMessage(
                channelId = channelId,
                title = notification.title,
                description = notification.description,
                color = notification.colorInt,
                fields = notification.fields,
                timestamp = notification.timestamp,
                footer = notification.footer,
            ).fold(
                ifLeft = { failure ->
                    when (failure) {
                        is DiscordFailure.NotInitialized -> {
                            logger.warn { "Discord not ready, notification skipped: ${failure.reason}" }
                            Unit.right()
                        }
                        else -> NotificationFailure.SendFailed("Discord error: $failure").left()
                    }
                },
                ifRight = { Unit.right() },
            )

    private data class NotificationData(
        val title: String,
        val description: String,
        val colorInt: Int,
        val fields: List<EmbedField>,
        val timestamp: Instant? = null,
        val footer: String? = "GameServerBot",
    )
}

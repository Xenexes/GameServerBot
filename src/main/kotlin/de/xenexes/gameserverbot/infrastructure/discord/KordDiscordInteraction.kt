package de.xenexes.gameserverbot.infrastructure.discord

import de.xenexes.gameserverbot.config.DiscordProperties
import de.xenexes.gameserverbot.domain.discord.DiscordInteraction
import de.xenexes.gameserverbot.domain.discord.DiscordRole
import de.xenexes.gameserverbot.domain.discord.DiscordRoleId
import de.xenexes.gameserverbot.domain.discord.DiscordUser
import de.xenexes.gameserverbot.domain.discord.DiscordUserId
import de.xenexes.gameserverbot.domain.discord.Permission
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.behavior.interaction.response.DeferredPublicMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import io.github.oshai.kotlinlogging.KotlinLogging

class KordDiscordInteraction(
    private val event: GuildChatInputCommandInteractionCreateEvent,
    private val properties: DiscordProperties,
) : DiscordInteraction {
    private val logger = KotlinLogging.logger {}
    private var deferredResponse: DeferredPublicMessageInteractionResponseBehavior? = null
    private var isResponseSent = false

    override val commandName: String = event.interaction.invokedCommandName

    override suspend fun getUser(): DiscordUser {
        val member = event.interaction.user.asMember()
        val roles =
            member.roleIds
                .map { roleId ->
                    val permissions = mapRoleToPermissions(roleId.toString())
                    DiscordRole(DiscordRoleId(roleId.toString()), "role", permissions)
                }.toSet()

        return DiscordUser(
            id = DiscordUserId(member.id.toString()),
            username = member.username,
            roles = roles,
        )
    }

    override suspend fun defer() {
        if (!isResponseSent) {
            deferredResponse = event.interaction.deferPublicResponse()
            isResponseSent = true
        }
    }

    override suspend fun editResponse(message: String) {
        val content = if (message.length > 2000) message.take(1997) + "..." else message
        deferredResponse?.respond { this.content = content }
            ?: logger.warn { "Cannot edit response: interaction was not deferred" }
    }

    override suspend fun respondImmediately(message: String) {
        if (!isResponseSent) {
            event.interaction.respondPublic { content = message }
            isResponseSent = true
        }
    }

    override fun getSubcommandName(): String? =
        try {
            val command = event.interaction.command
            if (command is dev.kord.core.entity.interaction.SubCommand) {
                command.name
            } else {
                null
            }
        } catch (e: Exception) {
            logger.debug(e) { "No subcommand found" }
            null
        }

    override fun getStringParameter(name: String): String? =
        try {
            val command = event.interaction.command
            if (command is dev.kord.core.entity.interaction.SubCommand) {
                command.strings[name]
            } else {
                event.interaction.command.strings[name]
            }
        } catch (e: Exception) {
            null
        }

    override fun getIntParameter(name: String): Int? =
        try {
            val command = event.interaction.command
            if (command is dev.kord.core.entity.interaction.SubCommand) {
                command.integers[name]?.toInt()
            } else {
                event.interaction.command.integers[name]
                    ?.toInt()
            }
        } catch (e: Exception) {
            null
        }

    private fun mapRoleToPermissions(roleId: String): Set<Permission> =
        when (roleId) {
            properties.adminRoleId ->
                setOf(
                    Permission.ADMIN,
                    Permission.SERVER_MANAGEMENT,
                    Permission.PLAYER_MANAGEMENT,
                )

            properties.allowedRoleId ->
                setOf(
                    Permission.SERVER_MANAGEMENT,
                    Permission.PLAYER_MANAGEMENT,
                )

            else -> emptySet()
        }
}

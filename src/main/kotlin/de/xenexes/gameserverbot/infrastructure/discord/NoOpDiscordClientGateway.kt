package de.xenexes.gameserverbot.infrastructure.discord

import arrow.core.Either
import arrow.core.right
import de.xenexes.gameserverbot.domain.discord.DiscordAutocompleteContext
import de.xenexes.gameserverbot.domain.discord.DiscordInteraction
import de.xenexes.gameserverbot.ports.outbound.DiscordClientGateway
import de.xenexes.gameserverbot.ports.outbound.EmbedField
import de.xenexes.gameserverbot.ports.outbound.failure.DiscordFailure
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant

class NoOpDiscordClientGateway : DiscordClientGateway {
    private val logger = KotlinLogging.logger {}

    override suspend fun start(): Either<DiscordFailure, Unit> {
        logger.debug { "NoOp: Discord client start (disabled)" }
        return Unit.right()
    }

    override suspend fun stop(): Either<DiscordFailure, Unit> {
        logger.debug { "NoOp: Discord client stop (disabled)" }
        return Unit.right()
    }

    override suspend fun isReady(): Boolean = false

    override suspend fun registerSlashCommands(guildId: String): Either<DiscordFailure, Unit> {
        logger.debug { "NoOp: Register slash commands (disabled)" }
        return Unit.right()
    }

    override suspend fun updatePresence(
        status: String,
        activityText: String,
    ): Either<DiscordFailure, Unit> {
        logger.debug { "NoOp: Update presence to $status - $activityText (disabled)" }
        return Unit.right()
    }

    override suspend fun sendMessage(
        channelId: String,
        message: String,
    ): Either<DiscordFailure, Unit> {
        logger.debug { "NoOp: Send message to $channelId: $message (disabled)" }
        return Unit.right()
    }

    override suspend fun sendEmbeddedMessage(
        channelId: String,
        title: String,
        description: String,
        color: Int?,
        fields: List<EmbedField>,
        timestamp: Instant?,
        footer: String?,
    ): Either<DiscordFailure, Unit> {
        logger.debug { "NoOp: Send embed to $channelId: $title (disabled)" }
        return Unit.right()
    }

    override suspend fun login(): Either<DiscordFailure, Unit> {
        logger.debug { "NoOp: Discord login (disabled)" }
        return Unit.right()
    }

    override suspend fun onCommandInteraction(handler: suspend (DiscordInteraction) -> Unit) {
        logger.debug { "NoOp: Register command interaction handler (disabled)" }
    }

    override suspend fun onAutocompleteInteraction(handler: suspend (DiscordAutocompleteContext) -> Unit) {
        logger.debug { "NoOp: Register autocomplete interaction handler (disabled)" }
    }
}

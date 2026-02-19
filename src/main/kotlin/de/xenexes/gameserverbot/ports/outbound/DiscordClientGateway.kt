package de.xenexes.gameserverbot.ports.outbound

import arrow.core.Either
import de.xenexes.gameserverbot.domain.discord.DiscordAutocompleteContext
import de.xenexes.gameserverbot.domain.discord.DiscordInteraction
import de.xenexes.gameserverbot.ports.outbound.failure.DiscordFailure
import java.time.Instant

interface DiscordClientGateway {
    suspend fun start(): Either<DiscordFailure, Unit>

    suspend fun stop(): Either<DiscordFailure, Unit>

    suspend fun isReady(): Boolean

    suspend fun registerSlashCommands(guildId: String): Either<DiscordFailure, Unit>

    suspend fun updatePresence(
        status: String,
        activityText: String,
    ): Either<DiscordFailure, Unit>

    suspend fun sendMessage(
        channelId: String,
        message: String,
    ): Either<DiscordFailure, Unit>

    suspend fun sendEmbeddedMessage(
        channelId: String,
        title: String,
        description: String,
        color: Int? = null,
        fields: List<EmbedField> = emptyList(),
        timestamp: Instant? = null,
        footer: String? = null,
    ): Either<DiscordFailure, Unit>

    suspend fun login(): Either<DiscordFailure, Unit>

    suspend fun onCommandInteraction(handler: suspend (DiscordInteraction) -> Unit)

    suspend fun onAutocompleteInteraction(handler: suspend (DiscordAutocompleteContext) -> Unit)
}

data class EmbedField(
    val name: String,
    val value: String,
    val inline: Boolean = false,
)

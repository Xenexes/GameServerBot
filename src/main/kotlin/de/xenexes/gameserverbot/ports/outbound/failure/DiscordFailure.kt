package de.xenexes.gameserverbot.ports.outbound.failure

sealed interface DiscordFailure {
    data class NotInitialized(
        val reason: String,
    ) : DiscordFailure

    data class ConnectionFailed(
        val reason: String,
    ) : DiscordFailure

    data class CommandFailed(
        val reason: String,
    ) : DiscordFailure

    data class PermissionDenied(
        val reason: String,
    ) : DiscordFailure

    data class ChannelNotFound(
        val channelId: String,
    ) : DiscordFailure

    data class GuildNotFound(
        val guildId: String,
    ) : DiscordFailure
}

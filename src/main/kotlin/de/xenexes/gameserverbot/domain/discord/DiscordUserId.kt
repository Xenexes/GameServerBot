package de.xenexes.gameserverbot.domain.discord

@JvmInline
value class DiscordUserId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "DiscordUserId cannot be blank" }
    }
}

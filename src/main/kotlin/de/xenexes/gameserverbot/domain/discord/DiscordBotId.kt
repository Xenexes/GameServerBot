package de.xenexes.gameserverbot.domain.discord

@JvmInline
value class DiscordBotId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "DiscordBotId cannot be blank" }
    }
}

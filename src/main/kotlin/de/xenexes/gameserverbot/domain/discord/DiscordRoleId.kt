package de.xenexes.gameserverbot.domain.discord

@JvmInline
value class DiscordRoleId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "DiscordRoleId cannot be blank" }
    }
}

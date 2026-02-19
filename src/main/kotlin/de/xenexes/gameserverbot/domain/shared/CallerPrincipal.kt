package de.xenexes.gameserverbot.domain.shared

import de.xenexes.gameserverbot.domain.discord.DiscordUserId

sealed interface CallerPrincipal {
    data class ApiUser(
        val username: String,
    ) : CallerPrincipal

    data class DiscordUser(
        val id: DiscordUserId,
    ) : CallerPrincipal

    data object System : CallerPrincipal

    data object CronJob : CallerPrincipal
}

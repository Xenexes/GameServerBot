package de.xenexes.gameserverbot.domain.shared

import de.xenexes.gameserverbot.domain.discord.DiscordUserId

data class UserContext(
    val userId: UserId,
    val principal: CallerPrincipal,
) {
    companion object {
        fun api(username: String) = UserContext(UserId(username), CallerPrincipal.ApiUser(username))

        fun discord(id: DiscordUserId) = UserContext(UserId(id.value), CallerPrincipal.DiscordUser(id))

        val system = UserContext(UserId.SYSTEM, CallerPrincipal.System)

        val cron = UserContext(UserId.CRON_JOB, CallerPrincipal.CronJob)
    }
}

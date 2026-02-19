package de.xenexes.gameserverbot.usecases

import arrow.core.Either
import de.xenexes.gameserverbot.ports.outbound.DiscordClientGateway
import de.xenexes.gameserverbot.ports.outbound.failure.DiscordFailure
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    name = ["gameserverbot.discord.enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
class DiscordPresenceUseCases(
    private val discordClient: DiscordClientGateway,
) {
    suspend fun isDiscordReady(): Boolean = discordClient.isReady()

    suspend fun updatePresence(
        statusType: String,
        activityText: String,
    ): Either<DiscordFailure, Unit> = discordClient.updatePresence(statusType, activityText)
}

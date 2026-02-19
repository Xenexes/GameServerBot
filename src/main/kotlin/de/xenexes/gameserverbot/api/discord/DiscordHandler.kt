package de.xenexes.gameserverbot.api.discord

import arrow.core.Either
import de.xenexes.gameserverbot.domain.discord.DiscordUserId
import de.xenexes.gameserverbot.domain.shared.UserContext
import de.xenexes.gameserverbot.usecases.UseCaseError
import de.xenexes.gameserverbot.usecases.UseCaseRaise
import de.xenexes.gameserverbot.usecases.useCase
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

@Component
class DiscordHandler {
    private val logger = KotlinLogging.logger {}

    suspend operator fun <Response> invoke(
        discordUserId: DiscordUserId,
        handler: suspend context(UserContext) UseCaseRaise.() -> Response,
    ): Either<UseCaseError, Response> {
        val ctx = UserContext.discord(discordUserId)
        return with(ctx) {
            useCase { handler() }
        }.onLeft { error ->
            logger.warn { "Discord operation failed for user ${discordUserId.value}: $error" }
        }
    }
}

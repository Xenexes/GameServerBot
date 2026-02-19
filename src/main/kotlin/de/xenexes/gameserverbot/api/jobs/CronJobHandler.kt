package de.xenexes.gameserverbot.api.jobs

import arrow.core.Either
import de.xenexes.gameserverbot.domain.shared.UserContext
import de.xenexes.gameserverbot.usecases.UseCaseError
import de.xenexes.gameserverbot.usecases.UseCaseRaise
import de.xenexes.gameserverbot.usecases.useCase
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

@Component
class CronJobHandler {
    private val logger = KotlinLogging.logger {}

    suspend operator fun <Response> invoke(
        handler: suspend context(UserContext) UseCaseRaise.() -> Response,
    ): Either<UseCaseError, Response> {
        val ctx = UserContext.cron
        return with(ctx) {
            useCase { handler() }
        }.onLeft { error ->
            logger.error { "Cron job failed: $error" }
        }
    }
}

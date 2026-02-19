package de.xenexes.gameserverbot.api.rest

import arrow.core.getOrElse
import de.xenexes.gameserverbot.api.FailureException
import de.xenexes.gameserverbot.domain.shared.UserContext
import de.xenexes.gameserverbot.usecases.UseCaseRaise
import de.xenexes.gameserverbot.usecases.useCase
import org.springframework.stereotype.Component

@Component
class ControllerHandler(
    private val userContextProvider: UserContextProvider,
) {
    suspend operator fun <Response> invoke(
        handler: suspend context(UserContext) UseCaseRaise.() -> Response,
    ): Response {
        val ctx = userContextProvider.getContext()
        return with(ctx) {
            useCase { handler() }
        }.getOrElse {
            throw FailureException(it)
        }
    }
}

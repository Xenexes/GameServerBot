package de.xenexes.gameserverbot.api.rest

import de.xenexes.gameserverbot.domain.shared.UserContext
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component

interface UserContextProvider {
    suspend fun getContext(): UserContext
}

@Component
class SecurityUserContextProvider : UserContextProvider {
    override suspend fun getContext(): UserContext {
        val name =
            ReactiveSecurityContextHolder
                .getContext()
                .awaitSingleOrNull()
                ?.authentication
                ?.name
        return if (name != null) UserContext.api(name) else UserContext.api("anonymous")
    }
}

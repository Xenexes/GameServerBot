package de.xenexes.gameserverbot.ports.outbound.failure

import de.xenexes.gameserverbot.domain.gameserver.NitradoServerId

sealed interface NitradoFailure {
    data class ApiError(
        val message: String,
        val statusCode: Int?,
    ) : NitradoFailure

    data class NetworkError(
        val cause: Throwable,
    ) : NitradoFailure

    data class ServerNotFound(
        val serverId: NitradoServerId,
    ) : NitradoFailure

    data class RateLimitExceeded(
        val retryAfter: Long? = null,
    ) : NitradoFailure

    data class InvalidToken(
        val message: String,
    ) : NitradoFailure

    data class ServiceUnavailable(
        val message: String,
    ) : NitradoFailure
}

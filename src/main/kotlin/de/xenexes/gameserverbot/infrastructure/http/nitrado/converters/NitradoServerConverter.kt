package de.xenexes.gameserverbot.infrastructure.http.nitrado.converters

import de.xenexes.gameserverbot.domain.gameserver.GameServerStatus
import de.xenexes.gameserverbot.ports.outbound.failure.NitradoFailure
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.ratelimiter.RequestNotPermitted

object NitradoServerConverter {
    fun mapStatus(nitradoStatus: String): GameServerStatus =
        when (nitradoStatus.lowercase()) {
            "started" -> GameServerStatus.STARTED
            "stopped" -> GameServerStatus.STOPPED
            "suspended" -> GameServerStatus.SUSPENDED
            "stopping" -> GameServerStatus.STOPPING
            "restarting" -> GameServerStatus.RESTARTING
            "guardian_locked" -> GameServerStatus.GUARDIAN_LOCKED
            "backup_restore" -> GameServerStatus.BACKUP_RESTORE
            "backup_creation" -> GameServerStatus.BACKUP_CREATION
            "chunkfix" -> GameServerStatus.CHUNKFIX
            "overviewmap_render" -> GameServerStatus.OVERVIEWMAP_RENDER
            "gs_installation" -> GameServerStatus.GS_INSTALLATION
            else -> GameServerStatus.UNKNOWN
        }

    fun mapException(e: Throwable): NitradoFailure =
        when (e) {
            is CallNotPermittedException -> NitradoFailure.ServiceUnavailable("Circuit breaker is open")
            is RequestNotPermitted -> NitradoFailure.RateLimitExceeded()
            else -> NitradoFailure.NetworkError(e)
        }
}

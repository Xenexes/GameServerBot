package de.xenexes.gameserverbot.api.jobs

import arrow.core.getOrElse
import de.xenexes.gameserverbot.api.FailureException
import de.xenexes.gameserverbot.usecases.GameServerUseCases
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/jobs")
class GameServerSyncJobController(
    private val handler: CronJobHandler,
    private val useCases: GameServerUseCases,
) {
    private val logger = KotlinLogging.logger {}

    @PostMapping("/sync-servers")
    suspend fun syncServers(): ResponseEntity<SyncJobResult> =
        handler {
            logger.info { "Starting server sync cycle" }
            val results = useCases.syncAllServers()
            val successes = results.count { it.isRight() }
            val failures = results.count { it.isLeft() }
            logger.info { "Server sync completed: $successes succeeded, $failures failed" }
            ResponseEntity.ok(SyncJobResult(successes, failures))
        }.getOrElse { throw FailureException(it) }
}

data class SyncJobResult(
    val succeeded: Int,
    val failed: Int,
)

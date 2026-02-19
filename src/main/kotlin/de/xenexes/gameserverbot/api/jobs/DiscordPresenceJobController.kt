package de.xenexes.gameserverbot.api.jobs

import arrow.core.getOrElse
import de.xenexes.gameserverbot.api.FailureException
import de.xenexes.gameserverbot.domain.discord.DiscordPresenceFormatter
import de.xenexes.gameserverbot.domain.gameserver.GameServer
import de.xenexes.gameserverbot.domain.gameserver.GameServerStatus
import de.xenexes.gameserverbot.usecases.DiscordPresenceUseCases
import de.xenexes.gameserverbot.usecases.GameServerUseCases
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.atomic.AtomicReference

@RestController
@RequestMapping("/api/jobs")
@ConditionalOnProperty(name = ["gameserverbot.discord.enabled"], havingValue = "true")
class DiscordPresenceJobController(
    private val handler: CronJobHandler,
    private val discordPresenceUseCases: DiscordPresenceUseCases,
    private val gameServerUseCases: GameServerUseCases,
) {
    private val logger = KotlinLogging.logger {}
    private val lastPresenceState = AtomicReference<PresenceState?>(null)

    @PostMapping("/update-discord-presence")
    suspend fun updatePresence(): ResponseEntity<Unit> {
        if (!discordPresenceUseCases.isDiscordReady()) {
            logger.debug { "Discord client not ready, skipping presence update" }
            return ResponseEntity.ok().build()
        }

        return handler {
            val serversResult = gameServerUseCases.findAll()
            serversResult.fold(
                { error ->
                    logger.warn { "Failed to fetch servers for presence update: $error" }
                },
                { servers ->
                    if (servers.isEmpty()) {
                        updatePresenceIfChanged(PresenceState.NoServers)
                    } else {
                        updatePresenceForServer(servers.first())
                    }
                },
            )
            ResponseEntity.ok<Unit>(null)
        }.getOrElse { throw FailureException(it) }
    }

    private suspend fun updatePresenceForServer(server: GameServer) {
        val newState =
            PresenceState.ServerStatus(
                serverName = server.name,
                status = server.status,
                gameName = server.gameName,
            )
        updatePresenceIfChanged(newState)
    }

    private suspend fun updatePresenceIfChanged(newState: PresenceState) {
        val previousState = lastPresenceState.get()

        if (previousState == newState) {
            logger.debug { "Presence unchanged, skipping update" }
            return
        }

        val (statusType, activityText) =
            when (newState) {
                is PresenceState.ServerStatus -> {
                    val presenceStatus = mapStatusToPresence(newState.status)
                    val activityText =
                        DiscordPresenceFormatter.format(
                            status = newState.status,
                            gameName = newState.gameName,
                        )
                    presenceStatus to activityText
                }
                is PresenceState.NoServers -> "idle" to "No servers configured"
            }

        discordPresenceUseCases.updatePresence(statusType, activityText).fold(
            { error ->
                logger.warn { "Failed to update Discord presence: $error" }
            },
            {
                logger.info { "Updated Discord presence: $activityText" }
                lastPresenceState.set(newState)
            },
        )
    }

    private fun mapStatusToPresence(status: GameServerStatus): String =
        when (status) {
            GameServerStatus.STARTED -> "online"
            GameServerStatus.STOPPED -> "idle"
            GameServerStatus.RESTARTING,
            GameServerStatus.STOPPING,
            -> "dnd"
            else -> "idle"
        }

    private sealed interface PresenceState {
        data class ServerStatus(
            val serverName: String,
            val status: GameServerStatus,
            val gameName: String?,
        ) : PresenceState

        data object NoServers : PresenceState
    }
}

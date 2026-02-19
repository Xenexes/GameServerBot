package de.xenexes.gameserverbot.api.events

import de.xenexes.gameserverbot.domain.player.PlayerEvent
import de.xenexes.gameserverbot.usecases.NotificationUseCases
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class PlayerEventListener(
    private val notificationUseCases: NotificationUseCases,
) {
    private val logger = KotlinLogging.logger {}

    @Async
    @EventListener
    fun onPlayerEvent(event: PlayerEvent) {
        logger.info { "Player event on server ${event.aggregateId.serverId.value}: ${event::class.simpleName}" }
        runBlocking {
            notificationUseCases
                .notifyPlayerListChanged(event)
                .onLeft { error ->
                    logger.error {
                        "Failed to send player notification for server ${event.aggregateId.serverId.value}: $error"
                    }
                }
        }
    }
}

package de.xenexes.gameserverbot.api.events

import arrow.core.Either
import de.xenexes.gameserverbot.domain.gameserver.GameServerEvent.GameServerCreatedEvent
import de.xenexes.gameserverbot.domain.gameserver.GameServerEvent.GameServerDeletedEvent
import de.xenexes.gameserverbot.domain.gameserver.GameServerEvent.GameServerStatusChangedEvent
import de.xenexes.gameserverbot.usecases.NotificationUseCases
import de.xenexes.gameserverbot.usecases.UseCaseError
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class GameServerEventListener(
    private val notificationUseCases: NotificationUseCases,
) {
    private val logger = KotlinLogging.logger {}

    @Async
    @EventListener
    fun onStatusChanged(event: GameServerStatusChangedEvent) {
        logger.info {
            "Server ${event.aggregateId.value} status changed: ${event.previousStatus} -> ${event.newStatus}"
        }

        if (event.isStatusDegradation() || event.isStatusImprovement()) {
            sendNotification("status change") { notificationUseCases.notifyStatusChanged(event) }
        }
    }

    @Async
    @EventListener
    fun onServerCreated(event: GameServerCreatedEvent) {
        logger.info { "Server ${event.name} created (${event.aggregateId.value})" }
        sendNotification("server created") { notificationUseCases.notifyServerCreated(event) }
    }

    @Async
    @EventListener
    fun onServerDeleted(event: GameServerDeletedEvent) {
        logger.info { "Server ${event.aggregateId.value} deleted" }
        sendNotification("server deleted") { notificationUseCases.notifyServerDeleted(event) }
    }

    private fun sendNotification(
        context: String,
        block: suspend () -> Either<UseCaseError, Unit>,
    ) {
        runBlocking {
            block()
                .onLeft { error -> logger.error { "Failed to send $context notification: $error" } }
        }
    }
}

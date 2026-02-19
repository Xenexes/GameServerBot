package de.xenexes.gameserverbot.usecases

import arrow.core.Either
import de.xenexes.gameserverbot.domain.gameserver.GameServerEvent.GameServerCreatedEvent
import de.xenexes.gameserverbot.domain.gameserver.GameServerEvent.GameServerDeletedEvent
import de.xenexes.gameserverbot.domain.gameserver.GameServerEvent.GameServerStatusChangedEvent
import de.xenexes.gameserverbot.domain.player.PlayerEvent
import de.xenexes.gameserverbot.ports.outbound.NotificationGateway
import org.springframework.stereotype.Component

@Component
class NotificationUseCases(
    private val notificationGateway: NotificationGateway,
) {
    suspend fun notifyStatusChanged(event: GameServerStatusChangedEvent): Either<UseCaseError, Unit> =
        useCase {
            notificationGateway.notifyStatusChanged(event).bind()
        }

    suspend fun notifyServerCreated(event: GameServerCreatedEvent): Either<UseCaseError, Unit> =
        useCase {
            notificationGateway.notifyServerCreated(event).bind()
        }

    suspend fun notifyServerDeleted(event: GameServerDeletedEvent): Either<UseCaseError, Unit> =
        useCase {
            notificationGateway.notifyServerDeleted(event).bind()
        }

    suspend fun notifyPlayerListChanged(event: PlayerEvent): Either<UseCaseError, Unit> =
        useCase {
            notificationGateway.notifyPlayerListChanged(event).bind()
        }
}

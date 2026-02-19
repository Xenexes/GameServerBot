package de.xenexes.gameserverbot.ports.outbound

import arrow.core.Either
import de.xenexes.gameserverbot.domain.gameserver.GameServerEvent.GameServerCreatedEvent
import de.xenexes.gameserverbot.domain.gameserver.GameServerEvent.GameServerDeletedEvent
import de.xenexes.gameserverbot.domain.gameserver.GameServerEvent.GameServerStatusChangedEvent
import de.xenexes.gameserverbot.domain.player.PlayerEvent
import de.xenexes.gameserverbot.ports.outbound.failure.NotificationFailure

interface NotificationGateway {
    suspend fun notifyStatusChanged(event: GameServerStatusChangedEvent): Either<NotificationFailure, Unit>

    suspend fun notifyServerCreated(event: GameServerCreatedEvent): Either<NotificationFailure, Unit>

    suspend fun notifyServerDeleted(event: GameServerDeletedEvent): Either<NotificationFailure, Unit>

    suspend fun notifyPlayerListChanged(event: PlayerEvent): Either<NotificationFailure, Unit>
}

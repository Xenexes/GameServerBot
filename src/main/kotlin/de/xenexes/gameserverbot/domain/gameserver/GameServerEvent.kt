package de.xenexes.gameserverbot.domain.gameserver

import de.xenexes.gameserverbot.domain.shared.DomainEvent
import de.xenexes.gameserverbot.domain.shared.UserId
import java.time.Instant
import java.util.UUID

sealed interface GameServerEvent : DomainEvent<GameServerId> {
    data class GameServerCreatedEvent(
        override val aggregateId: GameServerId,
        val name: String,
        val nitradoId: NitradoServerId,
        val createdBy: UserId,
        override val occurredAt: Instant,
        override val eventId: String = UUID.randomUUID().toString(),
        override val eventType: String = "GameServerCreated",
    ) : GameServerEvent

    data class GameServerStatusChangedEvent(
        override val aggregateId: GameServerId,
        val serverName: String,
        val previousStatus: GameServerStatus,
        val newStatus: GameServerStatus,
        val triggeredBy: UserId,
        override val occurredAt: Instant,
        override val eventId: String = UUID.randomUUID().toString(),
        override val eventType: String = "GameServerStatusChanged",
    ) : GameServerEvent {
        fun isStatusImprovement(): Boolean = !previousStatus.isHealthy() && newStatus.isHealthy()

        fun isStatusDegradation(): Boolean = previousStatus.isHealthy() && !newStatus.isHealthy()
    }

    data class GameServerDeletedEvent(
        override val aggregateId: GameServerId,
        val serverName: String,
        val deletedBy: UserId,
        override val occurredAt: Instant,
        override val eventId: String = UUID.randomUUID().toString(),
        override val eventType: String = "GameServerDeleted",
    ) : GameServerEvent
}

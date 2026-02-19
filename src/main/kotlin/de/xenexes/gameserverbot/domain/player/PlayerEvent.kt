package de.xenexes.gameserverbot.domain.player

import de.xenexes.gameserverbot.domain.shared.DomainEvent
import java.time.Instant
import java.util.UUID

sealed interface PlayerEvent : DomainEvent<PlayerListKey> {
    data class PlayerAddedToList(
        override val aggregateId: PlayerListKey,
        val listType: PlayerListType,
        val playerName: String,
        val identifier: PlayerIdentifier,
        override val occurredAt: Instant,
        override val eventId: String = UUID.randomUUID().toString(),
        override val eventType: String = "PlayerAddedToList",
    ) : PlayerEvent

    data class PlayerRemovedFromList(
        override val aggregateId: PlayerListKey,
        val listType: PlayerListType,
        val playerName: String,
        val identifier: PlayerIdentifier,
        override val occurredAt: Instant,
        override val eventId: String = UUID.randomUUID().toString(),
        override val eventType: String = "PlayerRemovedFromList",
    ) : PlayerEvent
}

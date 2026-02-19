package de.xenexes.gameserverbot.domain.player

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import de.xenexes.gameserverbot.domain.gameserver.GameServerId
import de.xenexes.gameserverbot.domain.shared.Aggregate
import java.time.Clock
import java.time.Instant

@ConsistentCopyVisibility
data class PlayerList private constructor(
    val serverId: GameServerId,
    val listType: PlayerListType,
    val entries: List<PlayerListEntry>,
    val lastUpdated: Instant,
    private val pendingEvents: MutableList<PlayerEvent> = mutableListOf(),
) : Aggregate<PlayerListKey, PlayerEvent> {
    override val id: PlayerListKey get() = PlayerListKey(serverId, listType)

    fun contains(identifier: PlayerIdentifier): Boolean = entries.any { it.identifier == identifier }

    fun add(
        entry: PlayerListEntry,
        clock: Clock,
    ): Either<PlayerFailure, PlayerList> =
        either {
            ensure(!contains(entry.identifier)) { PlayerFailure.AlreadyInList(entry.identifier, listType) }
            val now = clock.instant()
            val event =
                PlayerEvent.PlayerAddedToList(
                    aggregateId = PlayerListKey(serverId, listType),
                    listType = listType,
                    playerName = entry.name,
                    identifier = entry.identifier,
                    occurredAt = now,
                )
            copy(
                entries = entries + entry,
                lastUpdated = now,
                pendingEvents = (pendingEvents + event).toMutableList(),
            )
        }

    fun remove(
        identifier: PlayerIdentifier,
        clock: Clock,
    ): Either<PlayerFailure, PlayerList> =
        either {
            val entry =
                ensureNotNull(entries.find { it.identifier == identifier }) {
                    PlayerFailure.NotInList(identifier, listType)
                }
            val now = clock.instant()
            val event =
                PlayerEvent.PlayerRemovedFromList(
                    aggregateId = PlayerListKey(serverId, listType),
                    listType = listType,
                    playerName = entry.name,
                    identifier = identifier,
                    occurredAt = now,
                )
            copy(
                entries = entries.filter { it.identifier != identifier },
                lastUpdated = now,
                pendingEvents = (pendingEvents + event).toMutableList(),
            )
        }

    override fun consumeEvents(): List<PlayerEvent> {
        val events = pendingEvents.toList()
        pendingEvents.clear()
        return events
    }

    companion object {
        fun create(
            serverId: GameServerId,
            listType: PlayerListType,
            entries: List<PlayerListEntry> = emptyList(),
            clock: Clock,
        ): PlayerList =
            PlayerList(
                serverId = serverId,
                listType = listType,
                entries = entries,
                lastUpdated = clock.instant(),
            )

        fun empty(
            serverId: GameServerId,
            listType: PlayerListType,
            clock: Clock,
        ): PlayerList = create(serverId, listType, emptyList(), clock)
    }
}

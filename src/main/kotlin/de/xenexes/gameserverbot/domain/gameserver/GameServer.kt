package de.xenexes.gameserverbot.domain.gameserver

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import de.xenexes.gameserverbot.domain.shared.Aggregate
import de.xenexes.gameserverbot.domain.shared.UserId
import java.time.Clock
import java.time.Instant

@ConsistentCopyVisibility
data class GameServer private constructor(
    override val id: GameServerId,
    val name: String,
    val status: GameServerStatus,
    val nitradoId: NitradoServerId,
    val ip: String?,
    val port: Int?,
    val gameCode: String?,
    val gameName: String?,
    val playerSlots: Int?,
    val location: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    private val pendingEvents: MutableList<GameServerEvent> = mutableListOf(),
) : Aggregate<GameServerId, GameServerEvent> {
    fun updateStatus(
        newStatus: GameServerStatus,
        triggeredBy: UserId,
        clock: Clock,
    ): GameServer {
        if (status == newStatus) return this
        val event =
            GameServerEvent.GameServerStatusChangedEvent(
                aggregateId = id,
                serverName = name,
                previousStatus = status,
                newStatus = newStatus,
                triggeredBy = triggeredBy,
                occurredAt = clock.instant(),
            )
        return copy(
            status = newStatus,
            updatedAt = clock.instant(),
            pendingEvents = (pendingEvents + event).toMutableList(),
        )
    }

    fun updateFromExternal(
        externalStatus: GameServerStatus,
        ip: String?,
        port: Int?,
        gameCode: String?,
        gameName: String?,
        playerSlots: Int?,
        location: String?,
        clock: Clock,
    ): GameServer {
        val updated =
            copy(
                ip = ip,
                port = port,
                gameCode = gameCode,
                gameName = gameName,
                playerSlots = playerSlots,
                location = location,
                updatedAt = clock.instant(),
                pendingEvents = mutableListOf(),
            )
        return if (status != externalStatus) {
            updated.updateStatus(externalStatus, UserId.CRON_JOB, clock)
        } else {
            updated
        }
    }

    fun delete(
        deletedBy: UserId,
        clock: Clock,
    ): GameServer {
        val event =
            GameServerEvent.GameServerDeletedEvent(
                aggregateId = id,
                serverName = name,
                deletedBy = deletedBy,
                occurredAt = clock.instant(),
            )
        return copy(pendingEvents = (pendingEvents + event).toMutableList())
    }

    override fun consumeEvents(): List<GameServerEvent> {
        val events = pendingEvents.toList()
        pendingEvents.clear()
        return events
    }

    companion object {
        fun create(
            name: String,
            nitradoId: NitradoServerId,
            createdBy: UserId,
            clock: Clock,
        ): Either<GameServerFailure, GameServer> =
            either {
                ensure(name.isNotBlank()) { GameServerFailure.InvalidName("Name cannot be blank") }

                val id = GameServerId.create()
                val now = clock.instant()
                val server =
                    GameServer(
                        id = id,
                        name = name,
                        status = GameServerStatus.UNKNOWN,
                        nitradoId = nitradoId,
                        ip = null,
                        port = null,
                        gameCode = null,
                        gameName = null,
                        playerSlots = null,
                        location = null,
                        createdAt = now,
                        updatedAt = now,
                    )
                server.pendingEvents.add(GameServerEvent.GameServerCreatedEvent(id, name, nitradoId, createdBy, now))
                server
            }

        fun restore(
            id: GameServerId,
            name: String,
            status: GameServerStatus,
            nitradoId: NitradoServerId,
            ip: String?,
            port: Int?,
            gameCode: String?,
            gameName: String?,
            playerSlots: Int?,
            location: String?,
            createdAt: Instant,
            updatedAt: Instant,
        ): GameServer =
            GameServer(
                id = id,
                name = name,
                status = status,
                nitradoId = nitradoId,
                ip = ip,
                port = port,
                gameCode = gameCode,
                gameName = gameName,
                playerSlots = playerSlots,
                location = location,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
    }
}

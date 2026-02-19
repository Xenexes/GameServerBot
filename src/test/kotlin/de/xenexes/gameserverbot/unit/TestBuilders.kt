package de.xenexes.gameserverbot.unit

import arrow.core.Either
import arrow.core.getOrElse
import assertk.Assert
import assertk.assertions.support.expected
import de.xenexes.gameserverbot.domain.gameserver.GameServer
import de.xenexes.gameserverbot.domain.gameserver.GameServerId
import de.xenexes.gameserverbot.domain.gameserver.GameServerStatus
import de.xenexes.gameserverbot.domain.gameserver.NitradoServerId
import java.time.Instant

class GameServerBuilder(
    private var id: GameServerId = GameServerId.create(),
    private var name: String = "Test Server",
    private var status: GameServerStatus = GameServerStatus.STARTED,
    private var nitradoId: NitradoServerId = NitradoServerId(12345L),
    private var ip: String? = "127.0.0.1",
    private var port: Int? = 25565,
    private var gameCode: String? = "minecraft",
    private var gameName: String? = "Minecraft",
    private var playerSlots: Int? = 20,
    private var location: String? = "EU",
    private var createdAt: Instant = Instant.now(),
    private var updatedAt: Instant = Instant.now(),
) {
    fun withId(id: GameServerId) = apply { this.id = id }

    fun withName(name: String) = apply { this.name = name }

    fun withStatus(status: GameServerStatus) = apply { this.status = status }

    fun withNitradoId(nitradoId: Long) = apply { this.nitradoId = NitradoServerId(nitradoId) }

    fun withNitradoId(nitradoId: NitradoServerId) = apply { this.nitradoId = nitradoId }

    fun withIp(ip: String?) = apply { this.ip = ip }

    fun withPort(port: Int?) = apply { this.port = port }

    fun withGameCode(gameCode: String?) = apply { this.gameCode = gameCode }

    fun withGameName(gameName: String?) = apply { this.gameName = gameName }

    fun withPlayerSlots(playerSlots: Int?) = apply { this.playerSlots = playerSlots }

    fun withLocation(location: String?) = apply { this.location = location }

    fun withCreatedAt(createdAt: Instant) = apply { this.createdAt = createdAt }

    fun withUpdatedAt(updatedAt: Instant) = apply { this.updatedAt = updatedAt }

    fun build(): GameServer =
        GameServer.restore(
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

fun <L, R> Assert<Either<L, R>>.isRight(): Assert<R> =
    transform { actual ->
        actual.getOrElse { expected("to be Right but was Left: $it") }
    }

fun <L, R> Assert<Either<L, R>>.isLeft(): Assert<L> =
    transform { actual ->
        actual.swap().getOrElse { expected("to be Left but was Right: $it") }
    }

fun aGameServer(): GameServerBuilder = GameServerBuilder()

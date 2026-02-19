package de.xenexes.gameserverbot.api.rest.dto

import de.xenexes.gameserverbot.domain.gameserver.GameServer
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class GameServerDto(
    val id: String,
    val name: String,
    val status: String,
    val nitradoId: Long,
    val ip: String?,
    val port: Int?,
    val gameCode: String?,
    val gameName: String?,
    val playerSlots: Int?,
    val location: String?,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant,
)

fun GameServer.toDto() =
    GameServerDto(
        id = id.value,
        name = name,
        status = status.name,
        nitradoId = nitradoId.value,
        ip = ip,
        port = port,
        gameCode = gameCode,
        gameName = gameName,
        playerSlots = playerSlots,
        location = location,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

@Serializable
data class CreateGameServerRequest(
    val name: String,
    val nitradoId: Long,
)

@Serializable
data class StopServerRequest(
    val message: String? = null,
)

@Serializable
data class RestartServerRequest(
    val message: String? = null,
)

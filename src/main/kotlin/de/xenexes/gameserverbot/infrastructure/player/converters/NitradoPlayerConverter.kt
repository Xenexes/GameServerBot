package de.xenexes.gameserverbot.infrastructure.player.converters

import de.xenexes.gameserverbot.domain.player.IdentifierType
import de.xenexes.gameserverbot.domain.player.PlayerFailure
import de.xenexes.gameserverbot.domain.player.PlayerListEntry
import de.xenexes.gameserverbot.ports.outbound.NitradoPlayerListEntry
import de.xenexes.gameserverbot.ports.outbound.failure.NitradoFailure

object NitradoPlayerConverter {
    fun NitradoFailure.toPlayerFailure(): PlayerFailure =
        when (this) {
            is NitradoFailure.ServerNotFound -> PlayerFailure.OperationFailed("Server not found")
            is NitradoFailure.InvalidToken -> PlayerFailure.OperationFailed("Invalid API token")
            is NitradoFailure.RateLimitExceeded -> PlayerFailure.OperationFailed("Rate limit exceeded")
            is NitradoFailure.ServiceUnavailable -> PlayerFailure.OperationFailed("Service unavailable")
            is NitradoFailure.ApiError -> PlayerFailure.OperationFailed(message)
            is NitradoFailure.NetworkError -> PlayerFailure.OperationFailed("Network error: ${cause.message}")
        }

    fun NitradoPlayerListEntry.toDomain(): PlayerListEntry =
        PlayerListEntry.create(
            name = name,
            identifier = id,
            identifierType = parseIdentifierType(idType),
        )

    private fun parseIdentifierType(type: String): IdentifierType =
        when (type.lowercase()) {
            "steam_id", "steamid" -> IdentifierType.STEAM_ID
            "uuid", "minecraft_uuid" -> IdentifierType.MINECRAFT_UUID
            else -> IdentifierType.USERNAME
        }
}

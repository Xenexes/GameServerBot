package de.xenexes.gameserverbot.domain.player

import de.xenexes.gameserverbot.domain.gameserver.GameServerId

sealed interface PlayerFailure {
    data class InvalidIdentifier(
        val reason: String,
    ) : PlayerFailure

    data class AlreadyInList(
        val identifier: PlayerIdentifier,
        val listType: PlayerListType,
    ) : PlayerFailure

    data class NotInList(
        val identifier: PlayerIdentifier,
        val listType: PlayerListType,
    ) : PlayerFailure

    data class ServerNotFound(
        val serverId: GameServerId,
    ) : PlayerFailure

    data class OperationFailed(
        val reason: String,
    ) : PlayerFailure
}

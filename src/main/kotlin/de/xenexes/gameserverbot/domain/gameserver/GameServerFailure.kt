package de.xenexes.gameserverbot.domain.gameserver

sealed interface GameServerFailure {
    data class InvalidName(
        val reason: String,
    ) : GameServerFailure

    data class InvalidState(
        val current: GameServerStatus,
        val action: String,
    ) : GameServerFailure

    data class AlreadyExists(
        val nitradoId: NitradoServerId,
    ) : GameServerFailure
}

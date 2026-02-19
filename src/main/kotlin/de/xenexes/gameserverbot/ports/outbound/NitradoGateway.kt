package de.xenexes.gameserverbot.ports.outbound

import arrow.core.Either
import de.xenexes.gameserverbot.domain.gameserver.GameServerStatus
import de.xenexes.gameserverbot.domain.gameserver.NitradoServerId
import de.xenexes.gameserverbot.ports.outbound.failure.NitradoFailure

interface NitradoGateway {
    suspend fun startServer(nitradoId: NitradoServerId): Either<NitradoFailure, Unit>

    suspend fun stopServer(
        nitradoId: NitradoServerId,
        message: String? = null,
    ): Either<NitradoFailure, Unit>

    suspend fun restartServer(
        nitradoId: NitradoServerId,
        message: String? = null,
    ): Either<NitradoFailure, Unit>

    suspend fun fetchServerInfo(nitradoId: NitradoServerId): Either<NitradoFailure, NitradoServerInfo>

    suspend fun fetchServices(): Either<NitradoFailure, List<NitradoServiceInfo>>

    suspend fun fetchGameList(nitradoId: NitradoServerId): Either<NitradoFailure, List<NitradoGameInfo>>

    suspend fun fetchPlayers(nitradoId: NitradoServerId): Either<NitradoFailure, List<NitradoPlayer>>

    suspend fun switchGame(
        nitradoId: NitradoServerId,
        gameId: String,
    ): Either<NitradoFailure, Unit>

    suspend fun fetchWhitelist(nitradoId: NitradoServerId): Either<NitradoFailure, List<NitradoPlayerListEntry>>

    suspend fun addToWhitelist(
        nitradoId: NitradoServerId,
        identifier: String,
    ): Either<NitradoFailure, Unit>

    suspend fun removeFromWhitelist(
        nitradoId: NitradoServerId,
        identifier: String,
    ): Either<NitradoFailure, Unit>

    suspend fun fetchBanlist(nitradoId: NitradoServerId): Either<NitradoFailure, List<NitradoPlayerListEntry>>

    suspend fun addToBanlist(
        nitradoId: NitradoServerId,
        identifier: String,
    ): Either<NitradoFailure, Unit>

    suspend fun removeFromBanlist(
        nitradoId: NitradoServerId,
        identifier: String,
    ): Either<NitradoFailure, Unit>
}

data class NitradoServerInfo(
    val nitradoId: NitradoServerId,
    val status: GameServerStatus,
    val ip: String,
    val port: Int,
    val gameCode: String,
    val gameName: String,
    val slots: Int,
    val location: String,
    val query: NitradoServerQuery? = null,
)

data class NitradoServerQuery(
    val serverName: String?,
    val connectIp: String?,
    val map: String?,
    val version: String?,
    val playersCurrent: Int,
    val playersMax: Int,
)

data class NitradoServiceInfo(
    val serviceId: Long,
    val status: String,
    val typeHuman: String,
    val details: NitradoServiceDetails,
)

data class NitradoServiceDetails(
    val address: String,
    val name: String,
    val game: String,
    val slots: Int,
)

data class NitradoPlayer(
    val name: String,
    val online: Boolean,
)

data class NitradoGameInfo(
    val gameId: String,
    val folderShort: String,
    val gameHuman: String,
    val installed: Boolean,
    val active: Boolean,
)

data class NitradoPlayerListEntry(
    val name: String,
    val id: String,
    val idType: String,
)

@file:Suppress("ktlint:standard:max-line-length")

package de.xenexes.gameserverbot.unit.usecases

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import de.xenexes.gameserverbot.domain.gameserver.GameServer
import de.xenexes.gameserverbot.domain.gameserver.GameServerId
import de.xenexes.gameserverbot.domain.gameserver.NitradoServerId
import de.xenexes.gameserverbot.domain.shared.DomainEvent
import de.xenexes.gameserverbot.domain.shared.UserContext
import de.xenexes.gameserverbot.ports.outbound.DomainEventPublisher
import de.xenexes.gameserverbot.ports.outbound.GameServerRepository
import de.xenexes.gameserverbot.ports.outbound.NitradoGameInfo
import de.xenexes.gameserverbot.ports.outbound.NitradoGateway
import de.xenexes.gameserverbot.ports.outbound.NitradoPlayer
import de.xenexes.gameserverbot.ports.outbound.NitradoPlayerListEntry
import de.xenexes.gameserverbot.ports.outbound.NitradoServerInfo
import de.xenexes.gameserverbot.ports.outbound.NitradoServiceInfo
import de.xenexes.gameserverbot.ports.outbound.failure.NitradoFailure
import de.xenexes.gameserverbot.ports.outbound.failure.RepositoryFailure
import java.util.concurrent.ConcurrentHashMap

class InMemoryGameServerRepositoryDouble : GameServerRepository {
    private val servers = ConcurrentHashMap<GameServerId, GameServer>()
    var findByIdError: RepositoryFailure? = null
    var findAllError: RepositoryFailure? = null
    var saveError: RepositoryFailure? = null

    fun addServer(server: GameServer) {
        servers[server.id] = server
    }

    fun clear() {
        servers.clear()
        findByIdError = null
        findAllError = null
        saveError = null
    }

    context(_: UserContext) override suspend fun findById(id: GameServerId): Either<RepositoryFailure, GameServer> {
        findByIdError?.let { return it.left() }
        return servers[id]?.right()
            ?: RepositoryFailure.NotFound("GameServer", id.value).left()
    }

    context(_: UserContext) override suspend fun findByNitradoId(nitradoId: NitradoServerId): Either<RepositoryFailure, GameServer> =
        servers.values.find { it.nitradoId == nitradoId }?.right()
            ?: RepositoryFailure.NotFound("GameServer", "nitradoId=${nitradoId.value}").left()

    context(_: UserContext) override suspend fun findAll(): Either<RepositoryFailure, List<GameServer>> {
        findAllError?.let { return it.left() }
        return servers.values.toList().right()
    }

    context(_: UserContext) override suspend fun save(gameServer: GameServer): Either<RepositoryFailure, GameServer> {
        saveError?.let { return it.left() }
        servers[gameServer.id] = gameServer
        return gameServer.right()
    }

    context(_: UserContext) override suspend fun delete(id: GameServerId): Either<RepositoryFailure, Unit> {
        servers.remove(id)
        return Unit.right()
    }
}

class FakeNitradoGateway : NitradoGateway {
    var startServerResult: Either<NitradoFailure, Unit> = Unit.right()
    var stopServerResult: Either<NitradoFailure, Unit> = Unit.right()
    var restartServerResult: Either<NitradoFailure, Unit> = Unit.right()
    var fetchServerInfoResult: Either<NitradoFailure, NitradoServerInfo>? = null

    val startServerCalls = mutableListOf<NitradoServerId>()
    val stopServerCalls = mutableListOf<Pair<NitradoServerId, String?>>()
    val restartServerCalls = mutableListOf<Pair<NitradoServerId, String?>>()

    fun clear() {
        startServerCalls.clear()
        stopServerCalls.clear()
        restartServerCalls.clear()
        startServerResult = Unit.right()
        stopServerResult = Unit.right()
        restartServerResult = Unit.right()
        fetchServerInfoResult = null
    }

    override suspend fun startServer(nitradoId: NitradoServerId): Either<NitradoFailure, Unit> {
        startServerCalls.add(nitradoId)
        return startServerResult
    }

    override suspend fun stopServer(
        nitradoId: NitradoServerId,
        message: String?,
    ): Either<NitradoFailure, Unit> {
        stopServerCalls.add(nitradoId to message)
        return stopServerResult
    }

    override suspend fun restartServer(
        nitradoId: NitradoServerId,
        message: String?,
    ): Either<NitradoFailure, Unit> {
        restartServerCalls.add(nitradoId to message)
        return restartServerResult
    }

    override suspend fun fetchServerInfo(nitradoId: NitradoServerId): Either<NitradoFailure, NitradoServerInfo> =
        fetchServerInfoResult
            ?: NitradoFailure.ServerNotFound(nitradoId).left()

    override suspend fun fetchServices(): Either<NitradoFailure, List<NitradoServiceInfo>> =
        emptyList<NitradoServiceInfo>().right()

    override suspend fun fetchGameList(nitradoId: NitradoServerId): Either<NitradoFailure, List<NitradoGameInfo>> =
        emptyList<NitradoGameInfo>().right()

    override suspend fun fetchPlayers(nitradoId: NitradoServerId): Either<NitradoFailure, List<NitradoPlayer>> =
        emptyList<NitradoPlayer>().right()

    override suspend fun switchGame(
        nitradoId: NitradoServerId,
        gameId: String,
    ): Either<NitradoFailure, Unit> = Unit.right()

    override suspend fun fetchWhitelist(
        nitradoId: NitradoServerId,
    ): Either<NitradoFailure, List<NitradoPlayerListEntry>> = emptyList<NitradoPlayerListEntry>().right()

    override suspend fun addToWhitelist(
        nitradoId: NitradoServerId,
        identifier: String,
    ): Either<NitradoFailure, Unit> = Unit.right()

    override suspend fun removeFromWhitelist(
        nitradoId: NitradoServerId,
        identifier: String,
    ): Either<NitradoFailure, Unit> = Unit.right()

    override suspend fun fetchBanlist(
        nitradoId: NitradoServerId,
    ): Either<NitradoFailure, List<NitradoPlayerListEntry>> = emptyList<NitradoPlayerListEntry>().right()

    override suspend fun addToBanlist(
        nitradoId: NitradoServerId,
        identifier: String,
    ): Either<NitradoFailure, Unit> = Unit.right()

    override suspend fun removeFromBanlist(
        nitradoId: NitradoServerId,
        identifier: String,
    ): Either<NitradoFailure, Unit> = Unit.right()
}

class FakeDomainEventPublisher : DomainEventPublisher {
    val publishedEvents = mutableListOf<DomainEvent<*>>()

    fun clear() {
        publishedEvents.clear()
    }

    override suspend fun publish(events: List<DomainEvent<*>>) {
        publishedEvents.addAll(events)
    }
}

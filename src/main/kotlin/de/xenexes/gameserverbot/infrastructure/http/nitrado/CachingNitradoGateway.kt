package de.xenexes.gameserverbot.infrastructure.http.nitrado

import arrow.core.Either
import arrow.core.right
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import de.xenexes.gameserverbot.domain.gameserver.NitradoServerId
import de.xenexes.gameserverbot.ports.outbound.NitradoGameInfo
import de.xenexes.gameserverbot.ports.outbound.NitradoGateway
import de.xenexes.gameserverbot.ports.outbound.NitradoPlayer
import de.xenexes.gameserverbot.ports.outbound.NitradoPlayerListEntry
import de.xenexes.gameserverbot.ports.outbound.NitradoServerInfo
import de.xenexes.gameserverbot.ports.outbound.NitradoServiceInfo
import de.xenexes.gameserverbot.ports.outbound.failure.NitradoFailure
import io.github.oshai.kotlinlogging.KotlinLogging

class CachingNitradoGateway(
    private val delegate: NitradoGateway,
    cacheConfig: CacheConfig,
) : NitradoGateway {
    private val logger = KotlinLogging.logger {}

    private val serverInfoCache: Cache<NitradoServerId, NitradoServerInfo> =
        Caffeine
            .newBuilder()
            .maximumSize(cacheConfig.maximumSize)
            .expireAfterWrite(cacheConfig.serverStatusTtl)
            .build()

    private val servicesCache: Cache<String, List<NitradoServiceInfo>> =
        Caffeine
            .newBuilder()
            .maximumSize(1)
            .expireAfterWrite(cacheConfig.servicesTtl)
            .build()

    private val gameListCache: Cache<NitradoServerId, List<NitradoGameInfo>> =
        Caffeine
            .newBuilder()
            .maximumSize(cacheConfig.maximumSize)
            .expireAfterWrite(cacheConfig.gameListTtl)
            .build()

    private val playersCache: Cache<NitradoServerId, List<NitradoPlayer>> =
        Caffeine
            .newBuilder()
            .maximumSize(cacheConfig.maximumSize)
            .expireAfterWrite(cacheConfig.playersTtl)
            .build()

    private val whitelistCache: Cache<NitradoServerId, List<NitradoPlayerListEntry>> =
        Caffeine
            .newBuilder()
            .maximumSize(cacheConfig.maximumSize)
            .expireAfterWrite(cacheConfig.playerListTtl)
            .build()

    private val banlistCache: Cache<NitradoServerId, List<NitradoPlayerListEntry>> =
        Caffeine
            .newBuilder()
            .maximumSize(cacheConfig.maximumSize)
            .expireAfterWrite(cacheConfig.playerListTtl)
            .build()

    override suspend fun fetchServerInfo(nitradoId: NitradoServerId): Either<NitradoFailure, NitradoServerInfo> {
        serverInfoCache.getIfPresent(nitradoId)?.let {
            logger.debug { "Cache hit for server info: ${nitradoId.value}" }
            return it.right()
        }
        return delegate.fetchServerInfo(nitradoId).onRight { serverInfoCache.put(nitradoId, it) }
    }

    override suspend fun fetchServices(): Either<NitradoFailure, List<NitradoServiceInfo>> {
        servicesCache.getIfPresent(SERVICES_KEY)?.let {
            logger.debug { "Cache hit for services" }
            return it.right()
        }
        return delegate.fetchServices().onRight { servicesCache.put(SERVICES_KEY, it) }
    }

    override suspend fun fetchGameList(nitradoId: NitradoServerId): Either<NitradoFailure, List<NitradoGameInfo>> {
        gameListCache.getIfPresent(nitradoId)?.let {
            logger.debug { "Cache hit for game list: ${nitradoId.value}" }
            return it.right()
        }
        return delegate.fetchGameList(nitradoId).onRight { gameListCache.put(nitradoId, it) }
    }

    override suspend fun fetchPlayers(nitradoId: NitradoServerId): Either<NitradoFailure, List<NitradoPlayer>> {
        playersCache.getIfPresent(nitradoId)?.let {
            logger.debug { "Cache hit for players: ${nitradoId.value}" }
            return it.right()
        }
        return delegate.fetchPlayers(nitradoId).onRight { playersCache.put(nitradoId, it) }
    }

    override suspend fun fetchWhitelist(
        nitradoId: NitradoServerId,
    ): Either<NitradoFailure, List<NitradoPlayerListEntry>> {
        whitelistCache.getIfPresent(nitradoId)?.let {
            logger.debug { "Cache hit for whitelist: ${nitradoId.value}" }
            return it.right()
        }
        return delegate.fetchWhitelist(nitradoId).onRight { whitelistCache.put(nitradoId, it) }
    }

    override suspend fun fetchBanlist(
        nitradoId: NitradoServerId,
    ): Either<NitradoFailure, List<NitradoPlayerListEntry>> {
        banlistCache.getIfPresent(nitradoId)?.let {
            logger.debug { "Cache hit for banlist: ${nitradoId.value}" }
            return it.right()
        }
        return delegate.fetchBanlist(nitradoId).onRight { banlistCache.put(nitradoId, it) }
    }

    // Mutation operations: delegate and evict relevant caches

    override suspend fun startServer(nitradoId: NitradoServerId): Either<NitradoFailure, Unit> =
        delegate.startServer(nitradoId).onRight { serverInfoCache.invalidate(nitradoId) }

    override suspend fun stopServer(
        nitradoId: NitradoServerId,
        message: String?,
    ): Either<NitradoFailure, Unit> =
        delegate.stopServer(nitradoId, message).onRight { serverInfoCache.invalidate(nitradoId) }

    override suspend fun restartServer(
        nitradoId: NitradoServerId,
        message: String?,
    ): Either<NitradoFailure, Unit> =
        delegate.restartServer(nitradoId, message).onRight { serverInfoCache.invalidate(nitradoId) }

    override suspend fun switchGame(
        nitradoId: NitradoServerId,
        gameId: String,
    ): Either<NitradoFailure, Unit> =
        delegate.switchGame(nitradoId, gameId).onRight {
            serverInfoCache.invalidate(nitradoId)
            gameListCache.invalidate(nitradoId)
        }

    override suspend fun addToWhitelist(
        nitradoId: NitradoServerId,
        identifier: String,
    ): Either<NitradoFailure, Unit> =
        delegate.addToWhitelist(nitradoId, identifier).onRight { whitelistCache.invalidate(nitradoId) }

    override suspend fun removeFromWhitelist(
        nitradoId: NitradoServerId,
        identifier: String,
    ): Either<NitradoFailure, Unit> =
        delegate.removeFromWhitelist(nitradoId, identifier).onRight { whitelistCache.invalidate(nitradoId) }

    override suspend fun addToBanlist(
        nitradoId: NitradoServerId,
        identifier: String,
    ): Either<NitradoFailure, Unit> =
        delegate.addToBanlist(nitradoId, identifier).onRight { banlistCache.invalidate(nitradoId) }

    override suspend fun removeFromBanlist(
        nitradoId: NitradoServerId,
        identifier: String,
    ): Either<NitradoFailure, Unit> =
        delegate.removeFromBanlist(nitradoId, identifier).onRight { banlistCache.invalidate(nitradoId) }

    companion object {
        private const val SERVICES_KEY = "all-services"
    }
}

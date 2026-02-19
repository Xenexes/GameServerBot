@file:Suppress("ktlint:standard:max-line-length")

package de.xenexes.gameserverbot.infrastructure.persistence

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.right
import de.xenexes.gameserverbot.domain.gameserver.GameServer
import de.xenexes.gameserverbot.domain.gameserver.GameServerId
import de.xenexes.gameserverbot.domain.gameserver.NitradoServerId
import de.xenexes.gameserverbot.domain.shared.UserContext
import de.xenexes.gameserverbot.ports.outbound.GameServerRepository
import de.xenexes.gameserverbot.ports.outbound.failure.RepositoryFailure
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class InMemoryGameServerRepository : GameServerRepository {
    private val servers = ConcurrentHashMap<GameServerId, GameServer>()

    context(_: UserContext) override suspend fun findById(id: GameServerId): Either<RepositoryFailure, GameServer> =
        either {
            ensureNotNull(servers[id]) {
                RepositoryFailure.NotFound("GameServer", id.value)
            }
        }

    context(_: UserContext) override suspend fun findByNitradoId(nitradoId: NitradoServerId): Either<RepositoryFailure, GameServer> =
        either {
            ensureNotNull(servers.values.find { it.nitradoId == nitradoId }) {
                RepositoryFailure.NotFound("GameServer", "nitradoId=${nitradoId.value}")
            }
        }

    context(_: UserContext) override suspend fun findAll(): Either<RepositoryFailure, List<GameServer>> =
        servers.values
            .toList()
            .right()

    context(_: UserContext) override suspend fun save(gameServer: GameServer): Either<RepositoryFailure, GameServer> =
        either {
            servers[gameServer.id] = gameServer
            gameServer
        }

    context(_: UserContext) override suspend fun delete(id: GameServerId): Either<RepositoryFailure, Unit> =
        either {
            servers.remove(id)
        }
}

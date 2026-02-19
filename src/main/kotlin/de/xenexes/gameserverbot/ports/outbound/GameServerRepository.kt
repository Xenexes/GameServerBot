@file:Suppress("ktlint:standard:max-line-length")

package de.xenexes.gameserverbot.ports.outbound

import arrow.core.Either
import de.xenexes.gameserverbot.domain.gameserver.GameServer
import de.xenexes.gameserverbot.domain.gameserver.GameServerId
import de.xenexes.gameserverbot.domain.gameserver.NitradoServerId
import de.xenexes.gameserverbot.domain.shared.UserContext
import de.xenexes.gameserverbot.ports.outbound.failure.RepositoryFailure

interface GameServerRepository {
    context(_: UserContext) suspend fun findById(id: GameServerId): Either<RepositoryFailure, GameServer>

    context(_: UserContext) suspend fun findByNitradoId(nitradoId: NitradoServerId): Either<RepositoryFailure, GameServer>

    context(_: UserContext) suspend fun findAll(): Either<RepositoryFailure, List<GameServer>>

    context(_: UserContext) suspend fun save(gameServer: GameServer): Either<RepositoryFailure, GameServer>

    context(_: UserContext) suspend fun delete(id: GameServerId): Either<RepositoryFailure, Unit>
}

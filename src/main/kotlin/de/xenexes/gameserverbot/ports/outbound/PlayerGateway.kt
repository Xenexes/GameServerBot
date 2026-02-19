package de.xenexes.gameserverbot.ports.outbound

import arrow.core.Either
import de.xenexes.gameserverbot.domain.gameserver.GameServerId
import de.xenexes.gameserverbot.domain.player.Player
import de.xenexes.gameserverbot.domain.player.PlayerFailure
import de.xenexes.gameserverbot.domain.player.PlayerIdentifier
import de.xenexes.gameserverbot.domain.player.PlayerList
import de.xenexes.gameserverbot.domain.player.PlayerListType
import de.xenexes.gameserverbot.domain.shared.UserContext

interface PlayerGateway {
    context(_: UserContext) suspend fun fetchPlayerList(
        serverId: GameServerId,
        listType: PlayerListType,
    ): Either<PlayerFailure, PlayerList>

    context(_: UserContext) suspend fun addToPlayerList(
        serverId: GameServerId,
        listType: PlayerListType,
        identifier: PlayerIdentifier,
    ): Either<PlayerFailure, Unit>

    context(_: UserContext) suspend fun removeFromPlayerList(
        serverId: GameServerId,
        listType: PlayerListType,
        identifier: PlayerIdentifier,
    ): Either<PlayerFailure, Unit>

    context(_: UserContext) suspend fun fetchOnlinePlayers(serverId: GameServerId): Either<PlayerFailure, List<Player>>
}

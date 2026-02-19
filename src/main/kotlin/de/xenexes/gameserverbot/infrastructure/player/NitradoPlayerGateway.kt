@file:Suppress("ktlint:standard:max-line-length")

package de.xenexes.gameserverbot.infrastructure.player

import arrow.core.Either
import arrow.core.flatMap
import de.xenexes.gameserverbot.domain.gameserver.GameServerId
import de.xenexes.gameserverbot.domain.player.Player
import de.xenexes.gameserverbot.domain.player.PlayerFailure
import de.xenexes.gameserverbot.domain.player.PlayerIdentifier
import de.xenexes.gameserverbot.domain.player.PlayerList
import de.xenexes.gameserverbot.domain.player.PlayerListType
import de.xenexes.gameserverbot.domain.shared.UserContext
import de.xenexes.gameserverbot.infrastructure.player.converters.NitradoPlayerConverter.toDomain
import de.xenexes.gameserverbot.infrastructure.player.converters.NitradoPlayerConverter.toPlayerFailure
import de.xenexes.gameserverbot.ports.outbound.GameServerRepository
import de.xenexes.gameserverbot.ports.outbound.NitradoGateway
import de.xenexes.gameserverbot.ports.outbound.PlayerGateway
import org.springframework.stereotype.Component
import java.time.Clock

@Component
class NitradoPlayerGateway(
    private val nitradoGateway: NitradoGateway,
    private val gameServerRepository: GameServerRepository,
    private val clock: Clock,
) : PlayerGateway {
    context(_: UserContext) override suspend fun fetchPlayerList(
        serverId: GameServerId,
        listType: PlayerListType,
    ): Either<PlayerFailure, PlayerList> =
        resolveNitradoId(serverId).flatMap { nitradoId ->
            val result =
                when (listType) {
                    PlayerListType.WHITELIST -> nitradoGateway.fetchWhitelist(nitradoId)
                    PlayerListType.BANLIST -> nitradoGateway.fetchBanlist(nitradoId)
                }

            result
                .mapLeft { it.toPlayerFailure() }
                .map { entries ->
                    PlayerList.create(
                        serverId = serverId,
                        listType = listType,
                        entries = entries.map { it.toDomain() },
                        clock = clock,
                    )
                }
        }

    context(_: UserContext) override suspend fun addToPlayerList(
        serverId: GameServerId,
        listType: PlayerListType,
        identifier: PlayerIdentifier,
    ): Either<PlayerFailure, Unit> =
        resolveNitradoId(serverId).flatMap { nitradoId ->
            val result =
                when (listType) {
                    PlayerListType.WHITELIST -> nitradoGateway.addToWhitelist(nitradoId, identifier.value)
                    PlayerListType.BANLIST -> nitradoGateway.addToBanlist(nitradoId, identifier.value)
                }

            result.mapLeft { it.toPlayerFailure() }
        }

    context(_: UserContext) override suspend fun removeFromPlayerList(
        serverId: GameServerId,
        listType: PlayerListType,
        identifier: PlayerIdentifier,
    ): Either<PlayerFailure, Unit> =
        resolveNitradoId(serverId).flatMap { nitradoId ->
            val result =
                when (listType) {
                    PlayerListType.WHITELIST -> nitradoGateway.removeFromWhitelist(nitradoId, identifier.value)
                    PlayerListType.BANLIST -> nitradoGateway.removeFromBanlist(nitradoId, identifier.value)
                }

            result.mapLeft { it.toPlayerFailure() }
        }

    context(_: UserContext) override suspend fun fetchOnlinePlayers(serverId: GameServerId): Either<PlayerFailure, List<Player>> =
        resolveNitradoId(serverId).flatMap { nitradoId ->
            nitradoGateway
                .fetchPlayers(nitradoId)
                .mapLeft { it.toPlayerFailure() }
                .map { players ->
                    players
                        .filter { it.online }
                        .map { player ->
                            Player.create(
                                name = player.name,
                                identifier = PlayerIdentifier(player.name),
                                isOnline = true,
                            )
                        }
                }
        }

    context(_: UserContext) private suspend fun resolveNitradoId(serverId: GameServerId) =
        gameServerRepository
            .findById(serverId)
            .mapLeft { PlayerFailure.ServerNotFound(serverId) }
            .map { it.nitradoId }
}

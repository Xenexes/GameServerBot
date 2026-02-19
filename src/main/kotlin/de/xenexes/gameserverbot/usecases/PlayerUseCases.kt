package de.xenexes.gameserverbot.usecases

import arrow.core.Either
import de.xenexes.gameserverbot.domain.gameserver.GameServerId
import de.xenexes.gameserverbot.domain.player.Player
import de.xenexes.gameserverbot.domain.player.PlayerIdentifier
import de.xenexes.gameserverbot.domain.player.PlayerList
import de.xenexes.gameserverbot.domain.player.PlayerListEntry
import de.xenexes.gameserverbot.domain.player.PlayerListType
import de.xenexes.gameserverbot.domain.shared.UserContext
import de.xenexes.gameserverbot.ports.outbound.DomainEventPublisher
import de.xenexes.gameserverbot.ports.outbound.PlayerGateway
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Clock

@Component
class PlayerUseCases(
    private val playerGateway: PlayerGateway,
    private val eventPublisher: DomainEventPublisher,
    private val clock: Clock,
) {
    private val logger = KotlinLogging.logger {}

    context(ctx: UserContext) suspend fun getWhitelist(serverId: GameServerId): Either<UseCaseError, PlayerList> =
        useCase {
            playerGateway.fetchPlayerList(serverId, PlayerListType.WHITELIST).bind()
        }

    context(ctx: UserContext) suspend fun getBanlist(serverId: GameServerId): Either<UseCaseError, PlayerList> =
        useCase {
            playerGateway.fetchPlayerList(serverId, PlayerListType.BANLIST).bind()
        }

    context(ctx: UserContext) suspend fun addToWhitelist(
        serverId: GameServerId,
        identifier: PlayerIdentifier,
    ): Either<UseCaseError, Unit> =
        useCase {
            val list = playerGateway.fetchPlayerList(serverId, PlayerListType.WHITELIST).bind()
            val entry = PlayerListEntry.create(identifier.value, identifier.value, identifier.type)
            val updated = list.add(entry, clock).bind()
            playerGateway.addToPlayerList(serverId, PlayerListType.WHITELIST, identifier).bind()
            eventPublisher.publish(updated.consumeEvents())
            logger.info {
                "User ${ctx.userId.value} added ${identifier.value} to whitelist on server ${serverId.value}"
            }
        }

    context(ctx: UserContext) suspend fun removeFromWhitelist(
        serverId: GameServerId,
        identifier: PlayerIdentifier,
    ): Either<UseCaseError, Unit> =
        useCase {
            val list = playerGateway.fetchPlayerList(serverId, PlayerListType.WHITELIST).bind()
            val updated = list.remove(identifier, clock).bind()
            playerGateway.removeFromPlayerList(serverId, PlayerListType.WHITELIST, identifier).bind()
            eventPublisher.publish(updated.consumeEvents())
            logger.info {
                "User ${ctx.userId.value} removed ${identifier.value} from whitelist on server ${serverId.value}"
            }
        }

    context(ctx: UserContext) suspend fun addToBanlist(
        serverId: GameServerId,
        identifier: PlayerIdentifier,
    ): Either<UseCaseError, Unit> =
        useCase {
            val list = playerGateway.fetchPlayerList(serverId, PlayerListType.BANLIST).bind()
            val entry = PlayerListEntry.create(identifier.value, identifier.value, identifier.type)
            val updated = list.add(entry, clock).bind()
            playerGateway.addToPlayerList(serverId, PlayerListType.BANLIST, identifier).bind()
            eventPublisher.publish(updated.consumeEvents())
            logger.info { "User ${ctx.userId.value} banned ${identifier.value} on server ${serverId.value}" }
        }

    context(ctx: UserContext) suspend fun removeFromBanlist(
        serverId: GameServerId,
        identifier: PlayerIdentifier,
    ): Either<UseCaseError, Unit> =
        useCase {
            val list = playerGateway.fetchPlayerList(serverId, PlayerListType.BANLIST).bind()
            val updated = list.remove(identifier, clock).bind()
            playerGateway.removeFromPlayerList(serverId, PlayerListType.BANLIST, identifier).bind()
            eventPublisher.publish(updated.consumeEvents())
            logger.info { "User ${ctx.userId.value} unbanned ${identifier.value} on server ${serverId.value}" }
        }

    context(ctx: UserContext) suspend fun getOnlinePlayers(serverId: GameServerId): Either<UseCaseError, List<Player>> =
        useCase {
            playerGateway.fetchOnlinePlayers(serverId).bind()
        }
}

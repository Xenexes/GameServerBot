package de.xenexes.gameserverbot.usecases

import arrow.core.Either
import arrow.core.getOrElse
import de.xenexes.gameserverbot.domain.gameserver.GameServer
import de.xenexes.gameserverbot.domain.gameserver.GameServerFailure
import de.xenexes.gameserverbot.domain.gameserver.GameServerId
import de.xenexes.gameserverbot.domain.gameserver.GameServerStatus
import de.xenexes.gameserverbot.domain.gameserver.NitradoServerId
import de.xenexes.gameserverbot.domain.shared.UserContext
import de.xenexes.gameserverbot.ports.outbound.DomainEventPublisher
import de.xenexes.gameserverbot.ports.outbound.GameServerRepository
import de.xenexes.gameserverbot.ports.outbound.NitradoGameInfo
import de.xenexes.gameserverbot.ports.outbound.NitradoGateway
import de.xenexes.gameserverbot.ports.outbound.NitradoPlayer
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Clock

@Component
class GameServerUseCases(
    private val repository: GameServerRepository,
    private val nitradoGateway: NitradoGateway,
    private val eventPublisher: DomainEventPublisher,
    private val clock: Clock,
) {
    private val logger = KotlinLogging.logger {}

    context(ctx: UserContext) suspend fun findById(id: GameServerId): Either<UseCaseError, GameServer> =
        useCase {
            repository.findById(id).bind()
        }

    context(ctx: UserContext) suspend fun findAll(): Either<UseCaseError, List<GameServer>> =
        useCase {
            repository.findAll().bind()
        }

    context(ctx: UserContext) suspend fun create(command: CreateGameServerCommand): Either<UseCaseError, GameServer> =
        useCase {
            val existing = repository.findByNitradoId(command.nitradoId)
            ensure(existing.isLeft()) {
                UseCaseError.GameServer(GameServerFailure.AlreadyExists(command.nitradoId))
            }

            val server =
                GameServer
                    .create(
                        name = command.name,
                        nitradoId = command.nitradoId,
                        createdBy = ctx.userId,
                        clock = clock,
                    ).bind()

            val saved = repository.save(server).bind()
            eventPublisher.publish(saved.consumeEvents())
            saved
        }

    context(ctx: UserContext) suspend fun startServer(id: GameServerId): Either<UseCaseError, GameServer> =
        useCase {
            val server = repository.findById(id).bind()

            ensure(server.status.canStart()) {
                UseCaseError.GameServer(
                    GameServerFailure.InvalidState(server.status, "start"),
                )
            }

            nitradoGateway.startServer(server.nitradoId).bind()

            val updated = server.updateStatus(GameServerStatus.RESTARTING, ctx.userId, clock)
            val saved = repository.save(updated).bind()
            eventPublisher.publish(saved.consumeEvents())
            saved
        }

    context(ctx: UserContext) suspend fun stopServer(
        id: GameServerId,
        message: String? = null,
    ): Either<UseCaseError, GameServer> =
        useCase {
            val server = repository.findById(id).bind()

            ensure(server.status.canStop()) {
                UseCaseError.GameServer(
                    GameServerFailure.InvalidState(server.status, "stop"),
                )
            }

            nitradoGateway.stopServer(server.nitradoId, message).bind()

            val updated = server.updateStatus(GameServerStatus.STOPPING, ctx.userId, clock)
            val saved = repository.save(updated).bind()
            eventPublisher.publish(saved.consumeEvents())
            saved
        }

    context(ctx: UserContext) suspend fun restartServer(
        id: GameServerId,
        message: String? = null,
    ): Either<UseCaseError, GameServer> =
        useCase {
            val server = repository.findById(id).bind()

            ensure(server.status.canRestart()) {
                UseCaseError.GameServer(
                    GameServerFailure.InvalidState(server.status, "restart"),
                )
            }

            nitradoGateway.restartServer(server.nitradoId, message).bind()

            val updated = server.updateStatus(GameServerStatus.RESTARTING, ctx.userId, clock)
            val saved = repository.save(updated).bind()
            eventPublisher.publish(saved.consumeEvents())
            saved
        }

    context(ctx: UserContext) suspend fun syncServer(id: GameServerId): Either<UseCaseError, GameServer?> =
        useCase {
            val server = repository.findById(id).bind()
            val externalInfo = nitradoGateway.fetchServerInfo(server.nitradoId).bind()

            val updated =
                server.updateFromExternal(
                    externalStatus = externalInfo.status,
                    ip = externalInfo.ip,
                    port = externalInfo.port,
                    gameCode = externalInfo.gameCode,
                    gameName = externalInfo.gameName,
                    playerSlots = externalInfo.slots,
                    location = externalInfo.location,
                    clock = clock,
                )

            val saved = repository.save(updated).bind()
            if (updated.status == server.status) {
                null // Metadata updated but no status change, no events to publish
            } else {
                eventPublisher.publish(saved.consumeEvents())
                logger.info { "Synced server ${id.value}: status changed to ${saved.status}" }
                saved
            }
        }

    context(ctx: UserContext) suspend fun syncAllServers(): List<Either<UseCaseError, GameServer?>> {
        val servers = repository.findAll().getOrElse { return emptyList() }
        return servers.map { server ->
            syncServer(server.id)
        }
    }

    context(ctx: UserContext) suspend fun deleteServer(id: GameServerId): Either<UseCaseError, Unit> =
        useCase {
            val server = repository.findById(id).bind()
            val withEvent = server.delete(ctx.userId, clock)
            repository.delete(id).bind()
            eventPublisher.publish(withEvent.consumeEvents())
            logger.info { "User ${ctx.userId.value} deleted server ${id.value}" }
        }

    context(ctx: UserContext) suspend fun fetchGameList(id: GameServerId): Either<UseCaseError, List<NitradoGameInfo>> =
        useCase {
            val server = repository.findById(id).bind()
            nitradoGateway.fetchGameList(server.nitradoId).bind()
        }

    context(ctx: UserContext) suspend fun fetchPlayers(id: GameServerId): Either<UseCaseError, List<NitradoPlayer>> =
        useCase {
            val server = repository.findById(id).bind()
            nitradoGateway.fetchPlayers(server.nitradoId).bind()
        }

    context(ctx: UserContext) suspend fun switchGame(
        id: GameServerId,
        gameId: String,
    ): Either<UseCaseError, Unit> =
        useCase {
            val server = repository.findById(id).bind()
            nitradoGateway.switchGame(server.nitradoId, gameId).bind()
            logger.info { "User ${ctx.userId.value} switched game to $gameId on server ${id.value}" }
        }
}

data class CreateGameServerCommand(
    val name: String,
    val nitradoId: NitradoServerId,
)

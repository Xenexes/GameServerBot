package de.xenexes.gameserverbot.infrastructure.discord

import arrow.core.Either
import arrow.core.right
import de.xenexes.gameserverbot.domain.gameserver.GameServerEvent.GameServerCreatedEvent
import de.xenexes.gameserverbot.domain.gameserver.GameServerEvent.GameServerDeletedEvent
import de.xenexes.gameserverbot.domain.gameserver.GameServerEvent.GameServerStatusChangedEvent
import de.xenexes.gameserverbot.domain.player.PlayerEvent
import de.xenexes.gameserverbot.ports.outbound.NotificationGateway
import de.xenexes.gameserverbot.ports.outbound.failure.NotificationFailure
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    name = ["gameserverbot.discord.enabled"],
    havingValue = "false",
    matchIfMissing = true,
)
class NoOpNotificationGateway : NotificationGateway {
    private val logger = KotlinLogging.logger {}

    override suspend fun notifyStatusChanged(event: GameServerStatusChangedEvent): Either<NotificationFailure, Unit> {
        logger.debug {
            "NoOp notification: Server ${event.aggregateId.value} status changed " +
                "${event.previousStatus} -> ${event.newStatus}"
        }
        return Unit.right()
    }

    override suspend fun notifyServerCreated(event: GameServerCreatedEvent): Either<NotificationFailure, Unit> {
        logger.debug { "NoOp notification: Server created ${event.name}" }
        return Unit.right()
    }

    override suspend fun notifyServerDeleted(event: GameServerDeletedEvent): Either<NotificationFailure, Unit> {
        logger.debug { "NoOp notification: Server deleted ${event.aggregateId.value}" }
        return Unit.right()
    }

    override suspend fun notifyPlayerListChanged(event: PlayerEvent): Either<NotificationFailure, Unit> {
        logger.debug { "NoOp notification: Player event on server ${event.aggregateId.serverId.value}" }
        return Unit.right()
    }
}

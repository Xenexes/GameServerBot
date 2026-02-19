package de.xenexes.gameserverbot.usecases

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.raise.Raise
import arrow.core.raise.RaiseDSL
import arrow.core.raise.recover
import arrow.core.right
import de.xenexes.gameserverbot.domain.gameserver.GameServerFailure
import de.xenexes.gameserverbot.domain.player.PlayerFailure
import de.xenexes.gameserverbot.ports.outbound.failure.DiscordFailure
import de.xenexes.gameserverbot.ports.outbound.failure.NitradoFailure
import de.xenexes.gameserverbot.ports.outbound.failure.NotificationFailure
import de.xenexes.gameserverbot.ports.outbound.failure.RepositoryFailure

sealed interface UseCaseError {
    @JvmInline
    value class Repository<E : RepositoryFailure>(
        val error: E,
    ) : UseCaseError

    @JvmInline
    value class GameServer<E : GameServerFailure>(
        val error: E,
    ) : UseCaseError

    @JvmInline
    value class Nitrado<E : NitradoFailure>(
        val error: E,
    ) : UseCaseError

    @JvmInline
    value class Notification<E : NotificationFailure>(
        val error: E,
    ) : UseCaseError

    @JvmInline
    value class Discord<E : DiscordFailure>(
        val error: E,
    ) : UseCaseError

    @JvmInline
    value class Player<E : PlayerFailure>(
        val error: E,
    ) : UseCaseError
}

inline fun <A> useCase(block: UseCaseRaise.() -> A): Either<UseCaseError, A> =
    recover({ block(UseCaseRaise(this)).right() }) { e -> e.left() }

class UseCaseRaise(
    val raise: Raise<UseCaseError>,
) : Raise<UseCaseError> by raise {
    @JvmName("bindRepository")
    @RaiseDSL
    fun <E : RepositoryFailure, T> Either<E, T>.bind(): T = getOrElse { raise.raise(UseCaseError.Repository(it)) }

    @JvmName("bindGameServer")
    @RaiseDSL
    fun <E : GameServerFailure, T> Either<E, T>.bind(): T = getOrElse { raise.raise(UseCaseError.GameServer(it)) }

    @JvmName("bindNitrado")
    @RaiseDSL
    fun <E : NitradoFailure, T> Either<E, T>.bind(): T = getOrElse { raise.raise(UseCaseError.Nitrado(it)) }

    @JvmName("bindNotification")
    @RaiseDSL
    fun <E : NotificationFailure, T> Either<E, T>.bind(): T = getOrElse { raise.raise(UseCaseError.Notification(it)) }

    @JvmName("bindDiscord")
    @RaiseDSL
    fun <E : DiscordFailure, T> Either<E, T>.bind(): T = getOrElse { raise.raise(UseCaseError.Discord(it)) }

    @JvmName("bindPlayer")
    @RaiseDSL
    fun <E : PlayerFailure, T> Either<E, T>.bind(): T = getOrElse { raise.raise(UseCaseError.Player(it)) }

    @RaiseDSL
    inline fun ensure(
        condition: Boolean,
        raise: () -> UseCaseError,
    ) = if (condition) Unit else this.raise.raise(raise())
}

package de.xenexes.gameserverbot.unit.api

import arrow.core.left
import arrow.core.right
import de.xenexes.gameserverbot.api.events.PlayerEventListener
import de.xenexes.gameserverbot.domain.gameserver.GameServerId
import de.xenexes.gameserverbot.domain.player.PlayerEvent
import de.xenexes.gameserverbot.domain.player.PlayerIdentifier
import de.xenexes.gameserverbot.domain.player.PlayerListKey
import de.xenexes.gameserverbot.domain.player.PlayerListType
import de.xenexes.gameserverbot.ports.outbound.failure.NotificationFailure
import de.xenexes.gameserverbot.usecases.NotificationUseCases
import de.xenexes.gameserverbot.usecases.UseCaseError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class PlayerEventListenerTest {
    private lateinit var notificationUseCases: NotificationUseCases
    private lateinit var listener: PlayerEventListener

    @BeforeEach
    fun setup() {
        notificationUseCases = mockk(relaxed = true)
        listener = PlayerEventListener(notificationUseCases)
    }

    @Test
    fun `should send notification when player is added to whitelist`() {
        val event =
            PlayerEvent.PlayerAddedToList(
                aggregateId = PlayerListKey(GameServerId.create(), PlayerListType.WHITELIST),
                listType = PlayerListType.WHITELIST,
                playerName = "TestPlayer",
                identifier = PlayerIdentifier("test-id"),
                occurredAt = Instant.now(),
            )
        coEvery { notificationUseCases.notifyPlayerListChanged(event) } returns Unit.right()

        listener.onPlayerEvent(event)

        coVerify { notificationUseCases.notifyPlayerListChanged(event) }
    }

    @Test
    fun `should send notification when player is removed from whitelist`() {
        val event =
            PlayerEvent.PlayerRemovedFromList(
                aggregateId = PlayerListKey(GameServerId.create(), PlayerListType.WHITELIST),
                listType = PlayerListType.WHITELIST,
                playerName = "TestPlayer",
                identifier = PlayerIdentifier("test-id"),
                occurredAt = Instant.now(),
            )
        coEvery { notificationUseCases.notifyPlayerListChanged(event) } returns Unit.right()

        listener.onPlayerEvent(event)

        coVerify { notificationUseCases.notifyPlayerListChanged(event) }
    }

    @Test
    fun `should log error when notification fails`() {
        val event =
            PlayerEvent.PlayerAddedToList(
                aggregateId = PlayerListKey(GameServerId.create(), PlayerListType.WHITELIST),
                listType = PlayerListType.WHITELIST,
                playerName = "TestPlayer",
                identifier = PlayerIdentifier("test-id"),
                occurredAt = Instant.now(),
            )
        coEvery {
            notificationUseCases.notifyPlayerListChanged(event)
        } returns
            UseCaseError
                .Notification(NotificationFailure.SendFailed("Discord error"))
                .left()

        // Should not throw
        listener.onPlayerEvent(event)

        coVerify { notificationUseCases.notifyPlayerListChanged(event) }
    }
}

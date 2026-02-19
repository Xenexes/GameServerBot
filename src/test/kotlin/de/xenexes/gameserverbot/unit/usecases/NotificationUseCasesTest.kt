package de.xenexes.gameserverbot.unit.usecases

import arrow.core.left
import arrow.core.right
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import de.xenexes.gameserverbot.domain.gameserver.GameServerEvent.GameServerCreatedEvent
import de.xenexes.gameserverbot.domain.gameserver.GameServerEvent.GameServerDeletedEvent
import de.xenexes.gameserverbot.domain.gameserver.GameServerEvent.GameServerStatusChangedEvent
import de.xenexes.gameserverbot.domain.gameserver.GameServerId
import de.xenexes.gameserverbot.domain.gameserver.GameServerStatus
import de.xenexes.gameserverbot.domain.gameserver.NitradoServerId
import de.xenexes.gameserverbot.domain.player.PlayerEvent
import de.xenexes.gameserverbot.domain.player.PlayerIdentifier
import de.xenexes.gameserverbot.domain.player.PlayerListKey
import de.xenexes.gameserverbot.domain.player.PlayerListType
import de.xenexes.gameserverbot.domain.shared.UserId
import de.xenexes.gameserverbot.ports.outbound.NotificationGateway
import de.xenexes.gameserverbot.ports.outbound.failure.NotificationFailure
import de.xenexes.gameserverbot.usecases.NotificationUseCases
import de.xenexes.gameserverbot.usecases.UseCaseError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

class NotificationUseCasesTest {
    private lateinit var notificationGateway: NotificationGateway
    private lateinit var useCases: NotificationUseCases

    private val occurredAt = Instant.parse("2024-01-15T10:00:00Z")

    @BeforeEach
    fun setup() {
        notificationGateway = mockk()
        useCases = NotificationUseCases(notificationGateway)
    }

    @Nested
    inner class NotifyStatusChanged {
        @Test
        fun `should delegate status change to gateway`() =
            runTest {
                // Given
                val event =
                    GameServerStatusChangedEvent(
                        aggregateId = GameServerId.create(),
                        serverName = "Test Server",
                        previousStatus = GameServerStatus.STARTED,
                        newStatus = GameServerStatus.STOPPED,
                        triggeredBy = UserId.CRON_JOB,
                        occurredAt = occurredAt,
                    )
                coEvery { notificationGateway.notifyStatusChanged(event) } returns Unit.right()

                // When
                val result = useCases.notifyStatusChanged(event)

                // Then
                assertThat(result.isRight()).isTrue()
                coVerify { notificationGateway.notifyStatusChanged(event) }
            }

        @Test
        fun `should return failure when gateway fails on status change`() =
            runTest {
                // Given
                val event =
                    GameServerStatusChangedEvent(
                        aggregateId = GameServerId.create(),
                        serverName = "Test Server",
                        previousStatus = GameServerStatus.STARTED,
                        newStatus = GameServerStatus.STOPPED,
                        triggeredBy = UserId.CRON_JOB,
                        occurredAt = occurredAt,
                    )
                coEvery { notificationGateway.notifyStatusChanged(event) } returns
                    NotificationFailure.SendFailed("Network error").left()

                // When
                val result = useCases.notifyStatusChanged(event)

                // Then
                assertThat(result.isLeft()).isTrue()
                result.onLeft { assertThat(it).isInstanceOf(UseCaseError.Notification::class) }
            }
    }

    @Nested
    inner class NotifyServerCreated {
        @Test
        fun `should delegate server created event to gateway`() =
            runTest {
                // Given
                val event =
                    GameServerCreatedEvent(
                        aggregateId = GameServerId.create(),
                        name = "New Server",
                        nitradoId = NitradoServerId(12345),
                        createdBy = UserId("admin"),
                        occurredAt = occurredAt,
                    )
                coEvery { notificationGateway.notifyServerCreated(event) } returns Unit.right()

                // When
                val result = useCases.notifyServerCreated(event)

                // Then
                assertThat(result.isRight()).isTrue()
                coVerify { notificationGateway.notifyServerCreated(event) }
            }
    }

    @Nested
    inner class NotifyServerDeleted {
        @Test
        fun `should delegate server deleted event to gateway`() =
            runTest {
                // Given
                val event =
                    GameServerDeletedEvent(
                        aggregateId = GameServerId.create(),
                        serverName = "Old Server",
                        deletedBy = UserId("admin"),
                        occurredAt = occurredAt,
                    )
                coEvery { notificationGateway.notifyServerDeleted(event) } returns Unit.right()

                // When
                val result = useCases.notifyServerDeleted(event)

                // Then
                assertThat(result.isRight()).isTrue()
                coVerify { notificationGateway.notifyServerDeleted(event) }
            }
    }

    @Nested
    inner class NotifyPlayerListChanged {
        @Test
        fun `should delegate whitelist change event to gateway`() =
            runTest {
                // Given
                val event =
                    PlayerEvent.PlayerAddedToList(
                        aggregateId = PlayerListKey(GameServerId.create(), PlayerListType.WHITELIST),
                        listType = PlayerListType.WHITELIST,
                        playerName = "NewPlayer",
                        identifier = PlayerIdentifier("player-id"),
                        occurredAt = occurredAt,
                    )
                coEvery { notificationGateway.notifyPlayerListChanged(event) } returns Unit.right()

                // When
                val result = useCases.notifyPlayerListChanged(event)

                // Then
                assertThat(result.isRight()).isTrue()
                coVerify { notificationGateway.notifyPlayerListChanged(event) }
            }

        @Test
        fun `should delegate banlist change event to gateway`() =
            runTest {
                // Given
                val event =
                    PlayerEvent.PlayerAddedToList(
                        aggregateId = PlayerListKey(GameServerId.create(), PlayerListType.BANLIST),
                        listType = PlayerListType.BANLIST,
                        playerName = "Cheater",
                        identifier = PlayerIdentifier("cheater-id"),
                        occurredAt = occurredAt,
                    )
                coEvery { notificationGateway.notifyPlayerListChanged(event) } returns Unit.right()

                // When
                val result = useCases.notifyPlayerListChanged(event)

                // Then
                assertThat(result.isRight()).isTrue()
                coVerify { notificationGateway.notifyPlayerListChanged(event) }
            }

        @Test
        fun `should return failure when gateway fails on player list change`() =
            runTest {
                // Given
                val event =
                    PlayerEvent.PlayerRemovedFromList(
                        aggregateId = PlayerListKey(GameServerId.create(), PlayerListType.WHITELIST),
                        listType = PlayerListType.WHITELIST,
                        playerName = "Player",
                        identifier = PlayerIdentifier("player-id"),
                        occurredAt = occurredAt,
                    )
                coEvery { notificationGateway.notifyPlayerListChanged(event) } returns
                    NotificationFailure.SendFailed("Timeout").left()

                // When
                val result = useCases.notifyPlayerListChanged(event)

                // Then
                assertThat(result.isLeft()).isTrue()
                result.onLeft { assertThat(it).isInstanceOf(UseCaseError.Notification::class) }
            }
    }
}

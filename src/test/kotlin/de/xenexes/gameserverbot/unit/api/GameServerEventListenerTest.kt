package de.xenexes.gameserverbot.unit.api

import arrow.core.right
import de.xenexes.gameserverbot.api.events.GameServerEventListener
import de.xenexes.gameserverbot.domain.gameserver.GameServerEvent.GameServerCreatedEvent
import de.xenexes.gameserverbot.domain.gameserver.GameServerEvent.GameServerDeletedEvent
import de.xenexes.gameserverbot.domain.gameserver.GameServerEvent.GameServerStatusChangedEvent
import de.xenexes.gameserverbot.domain.gameserver.GameServerId
import de.xenexes.gameserverbot.domain.gameserver.GameServerStatus
import de.xenexes.gameserverbot.domain.gameserver.NitradoServerId
import de.xenexes.gameserverbot.domain.shared.UserId
import de.xenexes.gameserverbot.usecases.NotificationUseCases
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class GameServerEventListenerTest {
    private lateinit var notificationUseCases: NotificationUseCases
    private lateinit var listener: GameServerEventListener

    @BeforeEach
    fun setup() {
        notificationUseCases = mockk(relaxed = true)
        listener = GameServerEventListener(notificationUseCases)
    }

    @Test
    fun `should send notification when server health degrades`() {
        // Given
        val event =
            GameServerStatusChangedEvent(
                aggregateId = GameServerId.create(),
                serverName = "My Server",
                previousStatus = GameServerStatus.STARTED,
                newStatus = GameServerStatus.STOPPED,
                triggeredBy = UserId.CRON_JOB,
                occurredAt = Instant.now(),
            )
        coEvery { notificationUseCases.notifyStatusChanged(event) } returns Unit.right()

        // When
        listener.onStatusChanged(event)

        // Then
        coVerify { notificationUseCases.notifyStatusChanged(event) }
    }

    @Test
    fun `should send notification when server health improves`() {
        // Given
        val event =
            GameServerStatusChangedEvent(
                aggregateId = GameServerId.create(),
                serverName = "My Server",
                previousStatus = GameServerStatus.STOPPED,
                newStatus = GameServerStatus.STARTED,
                triggeredBy = UserId.CRON_JOB,
                occurredAt = Instant.now(),
            )
        coEvery { notificationUseCases.notifyStatusChanged(event) } returns Unit.right()

        // When
        listener.onStatusChanged(event)

        // Then
        coVerify { notificationUseCases.notifyStatusChanged(event) }
    }

    @Test
    fun `should not send notification when both statuses are unhealthy`() {
        // Given - STOPPED -> RESTARTING (both are unhealthy, so no degradation or improvement)
        val event =
            GameServerStatusChangedEvent(
                aggregateId = GameServerId.create(),
                serverName = "My Server",
                previousStatus = GameServerStatus.STOPPED,
                newStatus = GameServerStatus.RESTARTING,
                triggeredBy = UserId.CRON_JOB,
                occurredAt = Instant.now(),
            )

        // When
        listener.onStatusChanged(event)

        // Then - no notification should be sent (no health change)
        coVerify(exactly = 0) { notificationUseCases.notifyStatusChanged(event) }
    }

    @Test
    fun `should not send notification when transitioning between unhealthy states`() {
        // Given - STOPPING -> STOPPED (both are unhealthy)
        val event =
            GameServerStatusChangedEvent(
                aggregateId = GameServerId.create(),
                serverName = "My Server",
                previousStatus = GameServerStatus.STOPPING,
                newStatus = GameServerStatus.STOPPED,
                triggeredBy = UserId.CRON_JOB,
                occurredAt = Instant.now(),
            )

        // When
        listener.onStatusChanged(event)

        // Then - no notification should be sent (no health change)
        coVerify(exactly = 0) { notificationUseCases.notifyStatusChanged(event) }
    }

    @Test
    fun `should send notification when server goes from healthy to maintenance`() {
        // Given - STARTED -> BACKUP_CREATION is a degradation (healthy -> unhealthy)
        val event =
            GameServerStatusChangedEvent(
                aggregateId = GameServerId.create(),
                serverName = "My Server",
                previousStatus = GameServerStatus.STARTED,
                newStatus = GameServerStatus.BACKUP_CREATION,
                triggeredBy = UserId.CRON_JOB,
                occurredAt = Instant.now(),
            )
        coEvery { notificationUseCases.notifyStatusChanged(event) } returns Unit.right()

        // When
        listener.onStatusChanged(event)

        // Then - notification should be sent (health degradation)
        coVerify { notificationUseCases.notifyStatusChanged(event) }
    }

    @Test
    fun `should send notification when server is created`() {
        // Given
        val event =
            GameServerCreatedEvent(
                aggregateId = GameServerId.create(),
                name = "My Server",
                nitradoId = NitradoServerId(12345),
                createdBy = UserId("admin"),
                occurredAt = Instant.now(),
            )
        coEvery { notificationUseCases.notifyServerCreated(event) } returns Unit.right()

        // When
        listener.onServerCreated(event)

        // Then
        coVerify { notificationUseCases.notifyServerCreated(event) }
    }

    @Test
    fun `should send notification when server is deleted`() {
        // Given
        val event =
            GameServerDeletedEvent(
                aggregateId = GameServerId.create(),
                serverName = "My Server",
                deletedBy = UserId("admin"),
                occurredAt = Instant.now(),
            )
        coEvery { notificationUseCases.notifyServerDeleted(event) } returns Unit.right()

        // When
        listener.onServerDeleted(event)

        // Then
        coVerify { notificationUseCases.notifyServerDeleted(event) }
    }
}

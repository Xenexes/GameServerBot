package de.xenexes.gameserverbot.unit.domain

import de.xenexes.gameserverbot.domain.gameserver.GameServerEvent.GameServerStatusChangedEvent
import de.xenexes.gameserverbot.domain.gameserver.GameServerId
import de.xenexes.gameserverbot.domain.gameserver.GameServerStatus
import de.xenexes.gameserverbot.domain.shared.UserId
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameServerEventTest {
    private val serverId = GameServerId.create()
    private val user = UserId("test-user")
    private val testInstant = Instant.parse("2024-01-15T10:00:00Z")

    @Test
    fun `isStatusImprovement should return true when unhealthy becomes healthy`() {
        val event =
            GameServerStatusChangedEvent(
                aggregateId = serverId,
                serverName = "Test Server",
                previousStatus = GameServerStatus.STOPPED,
                newStatus = GameServerStatus.STARTED,
                triggeredBy = user,
                occurredAt = testInstant,
            )
        assertTrue(event.isStatusImprovement())
    }

    @Test
    fun `isStatusImprovement should return false when healthy remains healthy`() {
        val event =
            GameServerStatusChangedEvent(
                aggregateId = serverId,
                serverName = "Test Server",
                previousStatus = GameServerStatus.STARTED,
                newStatus = GameServerStatus.STARTED,
                triggeredBy = user,
                occurredAt = testInstant,
            )
        assertFalse(event.isStatusImprovement())
    }

    @Test
    fun `isStatusImprovement should return false when healthy becomes unhealthy`() {
        val event =
            GameServerStatusChangedEvent(
                aggregateId = serverId,
                serverName = "Test Server",
                previousStatus = GameServerStatus.STARTED,
                newStatus = GameServerStatus.STOPPED,
                triggeredBy = user,
                occurredAt = testInstant,
            )
        assertFalse(event.isStatusImprovement())
    }

    @Test
    fun `isStatusDegradation should return true when healthy becomes unhealthy`() {
        val event =
            GameServerStatusChangedEvent(
                aggregateId = serverId,
                serverName = "Test Server",
                previousStatus = GameServerStatus.STARTED,
                newStatus = GameServerStatus.STOPPED,
                triggeredBy = user,
                occurredAt = testInstant,
            )
        assertTrue(event.isStatusDegradation())
    }

    @Test
    fun `isStatusDegradation should return false when unhealthy remains unhealthy`() {
        val event =
            GameServerStatusChangedEvent(
                aggregateId = serverId,
                serverName = "Test Server",
                previousStatus = GameServerStatus.STOPPED,
                newStatus = GameServerStatus.RESTARTING,
                triggeredBy = user,
                occurredAt = testInstant,
            )
        assertFalse(event.isStatusDegradation())
    }

    @Test
    fun `isStatusDegradation should return false when unhealthy becomes healthy`() {
        val event =
            GameServerStatusChangedEvent(
                aggregateId = serverId,
                serverName = "Test Server",
                previousStatus = GameServerStatus.STOPPED,
                newStatus = GameServerStatus.STARTED,
                triggeredBy = user,
                occurredAt = testInstant,
            )
        assertFalse(event.isStatusDegradation())
    }
}

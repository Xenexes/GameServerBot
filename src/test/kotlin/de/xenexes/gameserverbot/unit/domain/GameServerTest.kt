package de.xenexes.gameserverbot.unit.domain

import arrow.core.getOrElse
import de.xenexes.gameserverbot.domain.gameserver.GameServer
import de.xenexes.gameserverbot.domain.gameserver.GameServerEvent.GameServerCreatedEvent
import de.xenexes.gameserverbot.domain.gameserver.GameServerEvent.GameServerStatusChangedEvent
import de.xenexes.gameserverbot.domain.gameserver.GameServerFailure
import de.xenexes.gameserverbot.domain.gameserver.GameServerId
import de.xenexes.gameserverbot.domain.gameserver.GameServerStatus
import de.xenexes.gameserverbot.domain.gameserver.NitradoServerId
import de.xenexes.gameserverbot.domain.shared.UserId
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GameServerTest {
    private val fixedInstant = Instant.parse("2024-01-15T10:00:00Z")
    private val fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC)
    private val testUser = UserId("test-user")
    private val nitradoId = NitradoServerId(12345L)

    @Test
    fun `create should succeed with valid name`() {
        val result =
            GameServer.create(
                name = "Test Server",
                nitradoId = nitradoId,
                createdBy = testUser,
                clock = fixedClock,
            )

        assertTrue(result.isRight())
        val server = result.getOrElse { throw AssertionError("Expected Right") }
        assertEquals("Test Server", server.name)
        assertEquals(nitradoId, server.nitradoId)
        assertEquals(GameServerStatus.UNKNOWN, server.status)
        assertEquals(fixedInstant, server.createdAt)
        assertEquals(fixedInstant, server.updatedAt)
    }

    @Test
    fun `create should fail with blank name`() {
        val result =
            GameServer.create(
                name = "",
                nitradoId = nitradoId,
                createdBy = testUser,
                clock = fixedClock,
            )

        assertTrue(result.isLeft())
        val failure = result.leftOrNull()
        assertIs<GameServerFailure.InvalidName>(failure)
    }

    @Test
    fun `create should emit GameServerCreatedEvent`() {
        val result =
            GameServer.create(
                name = "Test Server",
                nitradoId = nitradoId,
                createdBy = testUser,
                clock = fixedClock,
            )

        val server = result.getOrElse { throw AssertionError("Expected Right") }
        val events = server.consumeEvents()

        assertEquals(1, events.size)
        val event = events.first()
        assertIs<GameServerCreatedEvent>(event)
        assertEquals("Test Server", event.name)
        assertEquals(nitradoId, event.nitradoId)
        assertEquals(testUser, event.createdBy)
    }

    @Test
    fun `updateStatus should emit event when status changes`() {
        val server = createTestServer(GameServerStatus.STOPPED)
        val laterClock = Clock.fixed(fixedInstant.plusSeconds(60), ZoneOffset.UTC)

        val updated = server.updateStatus(GameServerStatus.STARTED, testUser, laterClock)

        assertEquals(GameServerStatus.STARTED, updated.status)
        val events = updated.consumeEvents()
        assertEquals(1, events.size)
        val event = events.first()
        assertIs<GameServerStatusChangedEvent>(event)
        assertEquals(GameServerStatus.STOPPED, event.previousStatus)
        assertEquals(GameServerStatus.STARTED, event.newStatus)
        assertEquals(testUser, event.triggeredBy)
    }

    @Test
    fun `updateStatus should not emit event when status unchanged`() {
        val server = createTestServer(GameServerStatus.STARTED)

        val updated = server.updateStatus(GameServerStatus.STARTED, testUser, fixedClock)

        assertEquals(server, updated)
        val events = updated.consumeEvents()
        assertTrue(events.isEmpty())
    }

    @Test
    fun `updateFromExternal should update all properties`() {
        val server = createTestServer(GameServerStatus.UNKNOWN)
        val laterClock = Clock.fixed(fixedInstant.plusSeconds(60), ZoneOffset.UTC)

        val updated =
            server.updateFromExternal(
                externalStatus = GameServerStatus.STARTED,
                ip = "192.168.1.1",
                port = 25565,
                gameCode = "minecraft",
                gameName = "Minecraft",
                playerSlots = 20,
                location = "EU",
                clock = laterClock,
            )

        assertEquals("192.168.1.1", updated.ip)
        assertEquals(25565, updated.port)
        assertEquals("minecraft", updated.gameCode)
        assertEquals("Minecraft", updated.gameName)
        assertEquals(20, updated.playerSlots)
        assertEquals("EU", updated.location)
        assertEquals(GameServerStatus.STARTED, updated.status)
    }

    @Test
    fun `updateFromExternal should emit status change event when status differs`() {
        val server = createTestServer(GameServerStatus.STOPPED)

        val updated =
            server.updateFromExternal(
                externalStatus = GameServerStatus.STARTED,
                ip = null,
                port = null,
                gameCode = null,
                gameName = null,
                playerSlots = null,
                location = null,
                clock = fixedClock,
            )

        val events = updated.consumeEvents()
        assertEquals(1, events.size)
        val event = events.first()
        assertIs<GameServerStatusChangedEvent>(event)
        assertEquals(UserId.CRON_JOB, event.triggeredBy)
    }

    @Test
    fun `consumeEvents should clear pending events`() {
        val result =
            GameServer.create(
                name = "Test Server",
                nitradoId = nitradoId,
                createdBy = testUser,
                clock = fixedClock,
            )
        val server = result.getOrElse { throw AssertionError("Expected Right") }

        val firstConsume = server.consumeEvents()
        val secondConsume = server.consumeEvents()

        assertEquals(1, firstConsume.size)
        assertTrue(secondConsume.isEmpty())
    }

    private fun createTestServer(status: GameServerStatus): GameServer =
        GameServer.restore(
            id = GameServerId.create(),
            name = "Test Server",
            status = status,
            nitradoId = nitradoId,
            ip = null,
            port = null,
            gameCode = null,
            gameName = null,
            playerSlots = null,
            location = null,
            createdAt = fixedInstant,
            updatedAt = fixedInstant,
        )
}

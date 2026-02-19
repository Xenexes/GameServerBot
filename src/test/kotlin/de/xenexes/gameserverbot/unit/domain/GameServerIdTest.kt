package de.xenexes.gameserverbot.unit.domain

import de.xenexes.gameserverbot.domain.gameserver.GameServerId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GameServerIdTest {
    @Test
    fun `should create GameServerId with valid value`() {
        val id = GameServerId("test-id")
        assertEquals("test-id", id.value)
    }

    @Test
    fun `should throw when creating GameServerId with blank value`() {
        assertThrows<IllegalArgumentException> {
            GameServerId("")
        }
    }

    @Test
    fun `should throw when creating GameServerId with whitespace only`() {
        assertThrows<IllegalArgumentException> {
            GameServerId("   ")
        }
    }

    @Test
    fun `should create unique IDs with factory method`() {
        val id1 = GameServerId.create()
        val id2 = GameServerId.create()
        assertNotEquals(id1, id2)
    }
}

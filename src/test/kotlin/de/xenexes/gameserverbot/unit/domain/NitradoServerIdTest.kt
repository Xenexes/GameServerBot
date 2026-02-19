package de.xenexes.gameserverbot.unit.domain

import de.xenexes.gameserverbot.domain.gameserver.NitradoServerId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class NitradoServerIdTest {
    @Test
    fun `should create NitradoServerId with positive value`() {
        val id = NitradoServerId(12345L)
        assertEquals(12345L, id.value)
    }

    @Test
    fun `should throw when creating NitradoServerId with zero`() {
        assertThrows<IllegalArgumentException> {
            NitradoServerId(0L)
        }
    }

    @Test
    fun `should throw when creating NitradoServerId with negative value`() {
        assertThrows<IllegalArgumentException> {
            NitradoServerId(-1L)
        }
    }
}

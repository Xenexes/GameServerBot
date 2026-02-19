package de.xenexes.gameserverbot.unit.domain

import de.xenexes.gameserverbot.domain.shared.UserId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class UserIdTest {
    @Test
    fun `should create UserId with valid value`() {
        val id = UserId("user-123")
        assertEquals("user-123", id.value)
    }

    @Test
    fun `should throw when creating UserId with blank value`() {
        assertThrows<IllegalArgumentException> {
            UserId("")
        }
    }

    @Test
    fun `should throw when creating UserId with whitespace only`() {
        assertThrows<IllegalArgumentException> {
            UserId("   ")
        }
    }

    @Test
    fun `should have SYSTEM constant`() {
        assertEquals("SYSTEM", UserId.SYSTEM.value)
    }

    @Test
    fun `should have CRON_JOB constant`() {
        assertEquals("CRON_JOB", UserId.CRON_JOB.value)
    }
}

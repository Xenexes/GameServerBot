package de.xenexes.gameserverbot.unit.domain

import de.xenexes.gameserverbot.domain.gameserver.GameServerStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameServerStatusTest {
    @Test
    fun `canStart should return true for STOPPED`() {
        assertTrue(GameServerStatus.STOPPED.canStart())
    }

    @Test
    fun `canStart should return true for SUSPENDED`() {
        assertTrue(GameServerStatus.SUSPENDED.canStart())
    }

    @ParameterizedTest
    @EnumSource(value = GameServerStatus::class, names = ["STOPPED", "SUSPENDED"], mode = EnumSource.Mode.EXCLUDE)
    fun `canStart should return false for other statuses`(status: GameServerStatus) {
        assertFalse(status.canStart())
    }

    @Test
    fun `canStop should return true only for STARTED`() {
        assertTrue(GameServerStatus.STARTED.canStop())
        assertFalse(GameServerStatus.STOPPED.canStop())
        assertFalse(GameServerStatus.RESTARTING.canStop())
    }

    @Test
    fun `canRestart should return true only for STARTED`() {
        assertTrue(GameServerStatus.STARTED.canRestart())
        assertFalse(GameServerStatus.STOPPED.canRestart())
        assertFalse(GameServerStatus.STOPPING.canRestart())
    }

    @Test
    fun `isHealthy should return true only for STARTED`() {
        assertTrue(GameServerStatus.STARTED.isHealthy())
        assertFalse(GameServerStatus.STOPPED.isHealthy())
        assertFalse(GameServerStatus.UNKNOWN.isHealthy())
    }

    @Test
    fun `isInMaintenance should return true for maintenance statuses`() {
        assertTrue(GameServerStatus.BACKUP_RESTORE.isInMaintenance())
        assertTrue(GameServerStatus.BACKUP_CREATION.isInMaintenance())
        assertTrue(GameServerStatus.CHUNKFIX.isInMaintenance())
        assertTrue(GameServerStatus.OVERVIEWMAP_RENDER.isInMaintenance())
        assertTrue(GameServerStatus.GS_INSTALLATION.isInMaintenance())
    }

    @Test
    fun `isInMaintenance should return false for non-maintenance statuses`() {
        assertFalse(GameServerStatus.STARTED.isInMaintenance())
        assertFalse(GameServerStatus.STOPPED.isInMaintenance())
        assertFalse(GameServerStatus.RESTARTING.isInMaintenance())
    }
}

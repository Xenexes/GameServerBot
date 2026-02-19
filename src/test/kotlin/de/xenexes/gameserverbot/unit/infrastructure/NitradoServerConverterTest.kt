package de.xenexes.gameserverbot.unit.infrastructure

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import de.xenexes.gameserverbot.domain.gameserver.GameServerStatus
import de.xenexes.gameserverbot.infrastructure.http.nitrado.converters.NitradoServerConverter
import de.xenexes.gameserverbot.ports.outbound.failure.NitradoFailure
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.ratelimiter.RequestNotPermitted
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class NitradoServerConverterTest {
    @Nested
    inner class MapStatus {
        @ParameterizedTest
        @CsvSource(
            "started, STARTED",
            "stopped, STOPPED",
            "suspended, SUSPENDED",
            "stopping, STOPPING",
            "restarting, RESTARTING",
            "guardian_locked, GUARDIAN_LOCKED",
            "backup_restore, BACKUP_RESTORE",
            "backup_creation, BACKUP_CREATION",
            "chunkfix, CHUNKFIX",
            "overviewmap_render, OVERVIEWMAP_RENDER",
            "gs_installation, GS_INSTALLATION",
        )
        fun `should map known Nitrado status to GameServerStatus`(
            nitradoStatus: String,
            expected: GameServerStatus,
        ) {
            assertThat(NitradoServerConverter.mapStatus(nitradoStatus)).isEqualTo(expected)
        }

        @Test
        fun `should map unknown status to UNKNOWN`() {
            assertThat(NitradoServerConverter.mapStatus("some_new_status")).isEqualTo(GameServerStatus.UNKNOWN)
        }

        @Test
        fun `should be case insensitive`() {
            assertThat(NitradoServerConverter.mapStatus("STARTED")).isEqualTo(GameServerStatus.STARTED)
            assertThat(NitradoServerConverter.mapStatus("Started")).isEqualTo(GameServerStatus.STARTED)
        }
    }

    @Nested
    inner class MapException {
        @Test
        fun `should map CallNotPermittedException to ServiceUnavailable`() {
            val exception = io.mockk.mockk<CallNotPermittedException>()
            val result = NitradoServerConverter.mapException(exception)
            assertThat(result).isInstanceOf(NitradoFailure.ServiceUnavailable::class)
        }

        @Test
        fun `should map RequestNotPermitted to RateLimitExceeded`() {
            val exception = io.mockk.mockk<RequestNotPermitted>()
            val result = NitradoServerConverter.mapException(exception)
            assertThat(result).isInstanceOf(NitradoFailure.RateLimitExceeded::class)
        }

        @Test
        fun `should map generic exception to NetworkError`() {
            val exception = RuntimeException("connection timeout")
            val result = NitradoServerConverter.mapException(exception)
            assertThat(result).isInstanceOf(NitradoFailure.NetworkError::class)
            assertThat((result as NitradoFailure.NetworkError).cause).isEqualTo(exception)
        }
    }
}

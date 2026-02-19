package de.xenexes.gameserverbot.unit.infrastructure

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import de.xenexes.gameserverbot.domain.gameserver.NitradoServerId
import de.xenexes.gameserverbot.domain.player.IdentifierType
import de.xenexes.gameserverbot.domain.player.PlayerFailure
import de.xenexes.gameserverbot.domain.player.PlayerIdentifier
import de.xenexes.gameserverbot.infrastructure.player.converters.NitradoPlayerConverter.toDomain
import de.xenexes.gameserverbot.infrastructure.player.converters.NitradoPlayerConverter.toPlayerFailure
import de.xenexes.gameserverbot.ports.outbound.NitradoPlayerListEntry
import de.xenexes.gameserverbot.ports.outbound.failure.NitradoFailure
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NitradoPlayerConverterTest {
    @Nested
    inner class ToPlayerFailure {
        @Test
        fun `should map ServerNotFound to OperationFailed`() {
            val failure = NitradoFailure.ServerNotFound(NitradoServerId(123))
            val result = failure.toPlayerFailure()
            assertThat(result).isInstanceOf(PlayerFailure.OperationFailed::class)
            assertThat((result as PlayerFailure.OperationFailed).reason).isEqualTo("Server not found")
        }

        @Test
        fun `should map InvalidToken to OperationFailed`() {
            val failure = NitradoFailure.InvalidToken("expired")
            val result = failure.toPlayerFailure()
            assertThat(result).isInstanceOf(PlayerFailure.OperationFailed::class)
            assertThat((result as PlayerFailure.OperationFailed).reason).isEqualTo("Invalid API token")
        }

        @Test
        fun `should map RateLimitExceeded to OperationFailed`() {
            val failure = NitradoFailure.RateLimitExceeded()
            val result = failure.toPlayerFailure()
            assertThat(result).isInstanceOf(PlayerFailure.OperationFailed::class)
            assertThat((result as PlayerFailure.OperationFailed).reason).isEqualTo("Rate limit exceeded")
        }

        @Test
        fun `should map ServiceUnavailable to OperationFailed`() {
            val failure = NitradoFailure.ServiceUnavailable("maintenance")
            val result = failure.toPlayerFailure()
            assertThat(result).isInstanceOf(PlayerFailure.OperationFailed::class)
            assertThat((result as PlayerFailure.OperationFailed).reason).isEqualTo("Service unavailable")
        }

        @Test
        fun `should map ApiError to OperationFailed with message`() {
            val failure = NitradoFailure.ApiError("Bad request", 400)
            val result = failure.toPlayerFailure()
            assertThat(result).isInstanceOf(PlayerFailure.OperationFailed::class)
            assertThat((result as PlayerFailure.OperationFailed).reason).isEqualTo("Bad request")
        }

        @Test
        fun `should map NetworkError to OperationFailed with cause message`() {
            val failure = NitradoFailure.NetworkError(RuntimeException("timeout"))
            val result = failure.toPlayerFailure()
            assertThat(result).isInstanceOf(PlayerFailure.OperationFailed::class)
            assertThat((result as PlayerFailure.OperationFailed).reason).isEqualTo("Network error: timeout")
        }
    }

    @Nested
    inner class ToDomain {
        @Test
        fun `should convert NitradoPlayerListEntry to PlayerListEntry with steam_id type`() {
            val entry =
                NitradoPlayerListEntry(
                    name = "TestPlayer",
                    id = "76561198000000001",
                    idType = "steam_id",
                )

            val result = entry.toDomain()

            assertThat(result.name).isEqualTo("TestPlayer")
            assertThat(result.identifier).isEqualTo(PlayerIdentifier("76561198000000001"))
            assertThat(result.identifierType).isEqualTo(IdentifierType.STEAM_ID)
        }

        @Test
        fun `should convert NitradoPlayerListEntry with unknown type to USERNAME`() {
            val entry =
                NitradoPlayerListEntry(
                    name = "TestPlayer",
                    id = "playerName",
                    idType = "name",
                )

            val result = entry.toDomain()

            assertThat(result.identifierType).isEqualTo(IdentifierType.USERNAME)
        }
    }
}

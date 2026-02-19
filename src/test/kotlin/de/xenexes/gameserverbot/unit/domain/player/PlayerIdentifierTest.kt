package de.xenexes.gameserverbot.unit.domain.player

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import de.xenexes.gameserverbot.domain.player.IdentifierType
import de.xenexes.gameserverbot.domain.player.PlayerFailure
import de.xenexes.gameserverbot.domain.player.PlayerIdentifier
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PlayerIdentifierTest {
    @Nested
    inner class Creation {
        @Test
        fun `should create identifier successfully with valid username`() {
            // When
            val result = PlayerIdentifier.create("PlayerName123")

            // Then
            assertThat(result.isRight()).isEqualTo(true)
            result.onRight {
                assertThat(it.value).isEqualTo("PlayerName123")
                assertThat(it.type).isEqualTo(IdentifierType.USERNAME)
            }
        }

        @Test
        fun `should trim whitespace from identifier`() {
            // When
            val result = PlayerIdentifier.create("  PlayerName  ")

            // Then
            assertThat(result.isRight()).isEqualTo(true)
            result.onRight {
                assertThat(it.value).isEqualTo("PlayerName")
            }
        }

        @Test
        fun `should fail with blank identifier`() {
            // When
            val result = PlayerIdentifier.create("   ")

            // Then
            assertThat(result.isLeft()).isEqualTo(true)
            result.onLeft {
                assertThat(it).isInstanceOf(PlayerFailure.InvalidIdentifier::class)
            }
        }

        @Test
        fun `should fail with empty identifier`() {
            // When
            val result = PlayerIdentifier.create("")

            // Then
            assertThat(result.isLeft()).isEqualTo(true)
            result.onLeft {
                assertThat(it).isInstanceOf(PlayerFailure.InvalidIdentifier::class)
            }
        }
    }

    @Nested
    inner class IdentifierTypeDetection {
        @Test
        fun `should detect Steam ID format`() {
            // Steam IDs are 17 digits
            val identifier = PlayerIdentifier("76561198000000001")

            assertThat(identifier.type).isEqualTo(IdentifierType.STEAM_ID)
        }

        @Test
        fun `should detect Minecraft UUID format`() {
            val identifier = PlayerIdentifier("a1b2c3d4-e5f6-7890-abcd-ef1234567890")

            assertThat(identifier.type).isEqualTo(IdentifierType.MINECRAFT_UUID)
        }

        @Test
        fun `should detect Minecraft UUID format case insensitive`() {
            val identifier = PlayerIdentifier("A1B2C3D4-E5F6-7890-ABCD-EF1234567890")

            assertThat(identifier.type).isEqualTo(IdentifierType.MINECRAFT_UUID)
        }

        @Test
        fun `should detect username for regular player names`() {
            val identifier = PlayerIdentifier("PlayerName123")

            assertThat(identifier.type).isEqualTo(IdentifierType.USERNAME)
        }

        @Test
        fun `should detect username for short numeric strings`() {
            // Not 17 digits, so not a Steam ID
            val identifier = PlayerIdentifier("12345")

            assertThat(identifier.type).isEqualTo(IdentifierType.USERNAME)
        }
    }
}

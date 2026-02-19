package de.xenexes.gameserverbot.unit.domain.player

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEqualTo
import assertk.assertions.isTrue
import de.xenexes.gameserverbot.domain.player.Player
import de.xenexes.gameserverbot.domain.player.PlayerIdentifier
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PlayerTest {
    @Nested
    inner class Creation {
        @Test
        fun `should create player with default online status false`() {
            // When
            val player =
                Player.create(
                    name = "TestPlayer",
                    identifier = PlayerIdentifier("test-id"),
                )

            // Then
            assertThat(player.name).isEqualTo("TestPlayer")
            assertThat(player.identifier.value).isEqualTo("test-id")
            assertThat(player.isOnline).isFalse()
        }

        @Test
        fun `should create player with online status`() {
            // When
            val player =
                Player.create(
                    name = "OnlinePlayer",
                    identifier = PlayerIdentifier("player-id"),
                    isOnline = true,
                )

            // Then
            assertThat(player.isOnline).isTrue()
        }

        @Test
        fun `should create player from list entry`() {
            // When
            val player =
                Player.fromListEntry(
                    name = "WhitelistedPlayer",
                    externalId = "76561198000000001",
                )

            // Then
            assertThat(player.name).isEqualTo("WhitelistedPlayer")
            assertThat(player.identifier.value).isEqualTo("76561198000000001")
            assertThat(player.isOnline).isFalse()
        }

        @Test
        fun `should generate unique ID for each player`() {
            // When
            val player1 =
                Player.create(
                    name = "Player1",
                    identifier = PlayerIdentifier("id-1"),
                )
            val player2 =
                Player.create(
                    name = "Player2",
                    identifier = PlayerIdentifier("id-2"),
                )

            // Then
            assertThat(player1.id).isNotEqualTo(player2.id)
        }
    }
}

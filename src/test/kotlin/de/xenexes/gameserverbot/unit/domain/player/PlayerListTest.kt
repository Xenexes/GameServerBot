package de.xenexes.gameserverbot.unit.domain.player

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import de.xenexes.gameserverbot.domain.gameserver.GameServerId
import de.xenexes.gameserverbot.domain.player.IdentifierType
import de.xenexes.gameserverbot.domain.player.PlayerFailure
import de.xenexes.gameserverbot.domain.player.PlayerIdentifier
import de.xenexes.gameserverbot.domain.player.PlayerList
import de.xenexes.gameserverbot.domain.player.PlayerListEntry
import de.xenexes.gameserverbot.domain.player.PlayerListType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class PlayerListTest {
    private val fixedClock = Clock.fixed(Instant.parse("2025-01-01T12:00:00Z"), ZoneId.of("UTC"))
    private val testServerId = GameServerId("test-server-1")

    @Nested
    inner class Creation {
        @Test
        fun `should create empty player list`() {
            // When
            val list = PlayerList.empty(testServerId, PlayerListType.WHITELIST, fixedClock)

            // Then
            assertThat(list.serverId).isEqualTo(testServerId)
            assertThat(list.listType).isEqualTo(PlayerListType.WHITELIST)
            assertThat(list.entries).hasSize(0)
        }

        @Test
        fun `should create player list with entries`() {
            // Given
            val entries =
                listOf(
                    PlayerListEntry.create("Player1", "id-1", IdentifierType.USERNAME),
                    PlayerListEntry.create("Player2", "12345678901234567", IdentifierType.STEAM_ID),
                )

            // When
            val list = PlayerList.create(testServerId, PlayerListType.BANLIST, entries, fixedClock)

            // Then
            assertThat(list.entries).hasSize(2)
            assertThat(list.listType).isEqualTo(PlayerListType.BANLIST)
        }
    }

    @Nested
    inner class Contains {
        @Test
        fun `should return true when player is in list`() {
            // Given
            val identifier = PlayerIdentifier("player-id")
            val entry = PlayerListEntry.create("Player1", "player-id", IdentifierType.USERNAME)
            val list = PlayerList.create(testServerId, PlayerListType.WHITELIST, listOf(entry), fixedClock)

            // When & Then
            assertThat(list.contains(identifier)).isTrue()
        }

        @Test
        fun `should return false when player is not in list`() {
            // Given
            val list = PlayerList.empty(testServerId, PlayerListType.WHITELIST, fixedClock)

            // When & Then
            assertThat(list.contains(PlayerIdentifier("unknown-id"))).isFalse()
        }
    }

    @Nested
    inner class AddPlayer {
        @Test
        fun `should add player to list successfully`() {
            // Given
            val list = PlayerList.empty(testServerId, PlayerListType.WHITELIST, fixedClock)
            val entry = PlayerListEntry.create("NewPlayer", "new-id", IdentifierType.USERNAME)

            // When
            val result = list.add(entry, fixedClock)

            // Then
            assertThat(result.isRight()).isEqualTo(true)
            result.onRight { updated ->
                assertThat(updated.entries).hasSize(1)
                assertThat(updated.contains(PlayerIdentifier("new-id"))).isTrue()
            }
        }

        @Test
        fun `should generate PlayerAddedToList event when adding player`() {
            // Given
            val list = PlayerList.empty(testServerId, PlayerListType.WHITELIST, fixedClock)
            val entry = PlayerListEntry.create("NewPlayer", "new-id", IdentifierType.USERNAME)

            // When
            val result = list.add(entry, fixedClock)

            // Then
            result.onRight { updated ->
                val events = updated.consumeEvents()
                assertThat(events).hasSize(1)
                assertThat(
                    events[0],
                ).isInstanceOf(de.xenexes.gameserverbot.domain.player.PlayerEvent.PlayerAddedToList::class)
            }
        }

        @Test
        fun `should fail when player is already in list`() {
            // Given
            val entry = PlayerListEntry.create("ExistingPlayer", "existing-id", IdentifierType.USERNAME)
            val list = PlayerList.create(testServerId, PlayerListType.WHITELIST, listOf(entry), fixedClock)
            val duplicateEntry = PlayerListEntry.create("ExistingPlayer", "existing-id", IdentifierType.USERNAME)

            // When
            val result = list.add(duplicateEntry, fixedClock)

            // Then
            assertThat(result.isLeft()).isEqualTo(true)
            result.onLeft {
                assertThat(it).isInstanceOf(PlayerFailure.AlreadyInList::class)
            }
        }
    }

    @Nested
    inner class RemovePlayer {
        @Test
        fun `should remove player from list successfully`() {
            // Given
            val entry = PlayerListEntry.create("Player1", "player-id", IdentifierType.USERNAME)
            val list = PlayerList.create(testServerId, PlayerListType.BANLIST, listOf(entry), fixedClock)

            // When
            val result = list.remove(PlayerIdentifier("player-id"), fixedClock)

            // Then
            assertThat(result.isRight()).isEqualTo(true)
            result.onRight { updated ->
                assertThat(updated.entries).hasSize(0)
                assertThat(updated.contains(PlayerIdentifier("player-id"))).isFalse()
            }
        }

        @Test
        fun `should generate PlayerRemovedFromList event when removing player`() {
            // Given
            val entry = PlayerListEntry.create("Player1", "player-id", IdentifierType.USERNAME)
            val list = PlayerList.create(testServerId, PlayerListType.BANLIST, listOf(entry), fixedClock)

            // When
            val result = list.remove(PlayerIdentifier("player-id"), fixedClock)

            // Then
            result.onRight { updated ->
                val events = updated.consumeEvents()
                assertThat(events).hasSize(1)
                assertThat(
                    events[0],
                ).isInstanceOf(de.xenexes.gameserverbot.domain.player.PlayerEvent.PlayerRemovedFromList::class)
            }
        }

        @Test
        fun `should fail when player is not in list`() {
            // Given
            val list = PlayerList.empty(testServerId, PlayerListType.WHITELIST, fixedClock)

            // When
            val result = list.remove(PlayerIdentifier("non-existent"), fixedClock)

            // Then
            assertThat(result.isLeft()).isEqualTo(true)
            result.onLeft {
                assertThat(it).isInstanceOf(PlayerFailure.NotInList::class)
            }
        }
    }

    @Nested
    inner class ConsumeEvents {
        @Test
        fun `should clear events after consuming`() {
            // Given
            val list = PlayerList.empty(testServerId, PlayerListType.WHITELIST, fixedClock)
            val entry = PlayerListEntry.create("NewPlayer", "new-id", IdentifierType.USERNAME)
            val result = list.add(entry, fixedClock)

            // When
            result.onRight { updated ->
                val firstConsume = updated.consumeEvents()
                val secondConsume = updated.consumeEvents()

                // Then
                assertThat(firstConsume).hasSize(1)
                assertThat(secondConsume).hasSize(0)
            }
        }
    }
}

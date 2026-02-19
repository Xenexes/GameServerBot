@file:Suppress("ktlint:standard:max-line-length")

package de.xenexes.gameserverbot.unit.usecases

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import de.xenexes.gameserverbot.domain.gameserver.GameServerId
import de.xenexes.gameserverbot.domain.player.IdentifierType
import de.xenexes.gameserverbot.domain.player.Player
import de.xenexes.gameserverbot.domain.player.PlayerEvent
import de.xenexes.gameserverbot.domain.player.PlayerFailure
import de.xenexes.gameserverbot.domain.player.PlayerIdentifier
import de.xenexes.gameserverbot.domain.player.PlayerList
import de.xenexes.gameserverbot.domain.player.PlayerListEntry
import de.xenexes.gameserverbot.domain.player.PlayerListType
import de.xenexes.gameserverbot.domain.shared.DomainEvent
import de.xenexes.gameserverbot.domain.shared.UserContext
import de.xenexes.gameserverbot.ports.outbound.DomainEventPublisher
import de.xenexes.gameserverbot.ports.outbound.PlayerGateway
import de.xenexes.gameserverbot.usecases.PlayerUseCases
import de.xenexes.gameserverbot.usecases.UseCaseError
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class PlayerUseCasesTest {
    private lateinit var playerGateway: FakePlayerGateway
    private lateinit var eventPublisher: FakeEventPublisher
    private lateinit var clock: Clock
    private lateinit var playerUseCases: PlayerUseCases

    private val testServerId = GameServerId("test-server-1")
    private val testUserContext = UserContext.api("test-user")

    @BeforeEach
    fun setup() {
        clock = Clock.fixed(Instant.parse("2025-01-01T12:00:00Z"), ZoneId.of("UTC"))
        playerGateway = FakePlayerGateway(clock)
        eventPublisher = FakeEventPublisher()
        playerUseCases = PlayerUseCases(playerGateway, eventPublisher, clock)
    }

    @Nested
    inner class GetWhitelist {
        @Test
        fun `should return whitelist successfully`() =
            runTest {
                // Given
                val entries =
                    listOf(
                        PlayerListEntry.create("Player1", "id-1", IdentifierType.USERNAME),
                        PlayerListEntry.create("Player2", "12345678901234567", IdentifierType.STEAM_ID),
                    )
                playerGateway.setWhitelist(testServerId, entries)

                // When
                val result =
                    with(testUserContext) {
                        playerUseCases.getWhitelist(testServerId)
                    }

                // Then
                assertThat(result.isRight()).isEqualTo(true)
                result.onRight { list ->
                    assertThat(list.entries).hasSize(2)
                    assertThat(list.listType).isEqualTo(PlayerListType.WHITELIST)
                }
            }

        @Test
        fun `should return error when gateway fails`() =
            runTest {
                // Given
                playerGateway.setError(PlayerFailure.ServerNotFound(testServerId))

                // When
                val result =
                    with(testUserContext) {
                        playerUseCases.getWhitelist(testServerId)
                    }

                // Then
                assertThat(result.isLeft()).isEqualTo(true)
                result.onLeft {
                    assertThat(it).isInstanceOf(UseCaseError.Player::class)
                }
            }
    }

    @Nested
    inner class GetBanlist {
        @Test
        fun `should return banlist successfully`() =
            runTest {
                // Given
                val entries = listOf(PlayerListEntry.create("BannedPlayer", "ban-id", IdentifierType.USERNAME))
                playerGateway.setBanlist(testServerId, entries)

                // When
                val result =
                    with(testUserContext) {
                        playerUseCases.getBanlist(testServerId)
                    }

                // Then
                assertThat(result.isRight()).isEqualTo(true)
                result.onRight { list ->
                    assertThat(list.entries).hasSize(1)
                    assertThat(list.listType).isEqualTo(PlayerListType.BANLIST)
                }
            }
    }

    @Nested
    inner class AddToWhitelist {
        @Test
        fun `should add player to whitelist successfully`() =
            runTest {
                // Given
                val identifier = PlayerIdentifier("new-player")

                // When
                val result =
                    with(testUserContext) {
                        playerUseCases.addToWhitelist(testServerId, identifier)
                    }

                // Then
                assertThat(result.isRight()).isEqualTo(true)
                assertThat(playerGateway.addedToWhitelist).hasSize(1)
            }

        @Test
        fun `should publish PlayerAddedToList event when adding to whitelist`() =
            runTest {
                // Given
                val identifier = PlayerIdentifier("new-player")

                // When
                with(testUserContext) {
                    playerUseCases.addToWhitelist(testServerId, identifier)
                }

                // Then
                assertThat(eventPublisher.publishedEvents).hasSize(1)
                val event = eventPublisher.publishedEvents.first() as PlayerEvent.PlayerAddedToList
                assertThat(event.aggregateId.serverId).isEqualTo(testServerId)
                assertThat(event.listType).isEqualTo(PlayerListType.WHITELIST)
                assertThat(event.playerName).isEqualTo("new-player")
            }

        @Test
        fun `should return error when adding to whitelist fails`() =
            runTest {
                // Given
                val identifier = PlayerIdentifier("duplicate-player")
                playerGateway.setError(PlayerFailure.AlreadyInList(identifier, PlayerListType.WHITELIST))

                // When
                val result =
                    with(testUserContext) {
                        playerUseCases.addToWhitelist(testServerId, identifier)
                    }

                // Then
                assertThat(result.isLeft()).isEqualTo(true)
            }
    }

    @Nested
    inner class RemoveFromWhitelist {
        @Test
        fun `should remove player from whitelist successfully`() =
            runTest {
                // Given
                val identifier = PlayerIdentifier("existing-player")
                playerGateway.setWhitelist(
                    testServerId,
                    listOf(PlayerListEntry.create(identifier.value, identifier.value, identifier.type)),
                )

                // When
                val result =
                    with(testUserContext) {
                        playerUseCases.removeFromWhitelist(testServerId, identifier)
                    }

                // Then
                assertThat(result.isRight()).isEqualTo(true)
                assertThat(playerGateway.removedFromWhitelist).hasSize(1)
            }

        @Test
        fun `should publish PlayerRemovedFromList event when removing from whitelist`() =
            runTest {
                // Given
                val identifier = PlayerIdentifier("existing-player")
                playerGateway.setWhitelist(
                    testServerId,
                    listOf(PlayerListEntry.create(identifier.value, identifier.value, identifier.type)),
                )

                // When
                with(testUserContext) {
                    playerUseCases.removeFromWhitelist(testServerId, identifier)
                }

                // Then
                assertThat(eventPublisher.publishedEvents).hasSize(1)
                val event = eventPublisher.publishedEvents.first() as PlayerEvent.PlayerRemovedFromList
                assertThat(event.aggregateId.serverId).isEqualTo(testServerId)
                assertThat(event.listType).isEqualTo(PlayerListType.WHITELIST)
            }
    }

    @Nested
    inner class AddToBanlist {
        @Test
        fun `should add player to banlist successfully`() =
            runTest {
                // Given
                val identifier = PlayerIdentifier("cheater")

                // When
                val result =
                    with(testUserContext) {
                        playerUseCases.addToBanlist(testServerId, identifier)
                    }

                // Then
                assertThat(result.isRight()).isEqualTo(true)
                assertThat(playerGateway.addedToBanlist).hasSize(1)
            }

        @Test
        fun `should publish PlayerAddedToList event with BANLIST type when banning`() =
            runTest {
                // Given
                val identifier = PlayerIdentifier("cheater")

                // When
                with(testUserContext) {
                    playerUseCases.addToBanlist(testServerId, identifier)
                }

                // Then
                assertThat(eventPublisher.publishedEvents).hasSize(1)
                val event = eventPublisher.publishedEvents.first() as PlayerEvent.PlayerAddedToList
                assertThat(event.listType).isEqualTo(PlayerListType.BANLIST)
            }
    }

    @Nested
    inner class RemoveFromBanlist {
        @Test
        fun `should remove player from banlist successfully`() =
            runTest {
                // Given
                val identifier = PlayerIdentifier("reformed-player")
                playerGateway.setBanlist(
                    testServerId,
                    listOf(PlayerListEntry.create(identifier.value, identifier.value, identifier.type)),
                )

                // When
                val result =
                    with(testUserContext) {
                        playerUseCases.removeFromBanlist(testServerId, identifier)
                    }

                // Then
                assertThat(result.isRight()).isEqualTo(true)
                assertThat(playerGateway.removedFromBanlist).hasSize(1)
            }

        @Test
        fun `should publish PlayerRemovedFromList event with BANLIST type when unbanning`() =
            runTest {
                // Given
                val identifier = PlayerIdentifier("reformed-player")
                playerGateway.setBanlist(
                    testServerId,
                    listOf(PlayerListEntry.create(identifier.value, identifier.value, identifier.type)),
                )

                // When
                with(testUserContext) {
                    playerUseCases.removeFromBanlist(testServerId, identifier)
                }

                // Then
                assertThat(eventPublisher.publishedEvents).hasSize(1)
                val event = eventPublisher.publishedEvents.first() as PlayerEvent.PlayerRemovedFromList
                assertThat(event.listType).isEqualTo(PlayerListType.BANLIST)
            }
    }

    @Nested
    inner class GetOnlinePlayers {
        @Test
        fun `should return online players successfully`() =
            runTest {
                // Given
                val players =
                    listOf(
                        Player.create("OnlinePlayer1", PlayerIdentifier("player-1"), isOnline = true),
                        Player.create("OnlinePlayer2", PlayerIdentifier("player-2"), isOnline = true),
                    )
                playerGateway.setOnlinePlayers(testServerId, players)

                // When
                val result =
                    with(testUserContext) {
                        playerUseCases.getOnlinePlayers(testServerId)
                    }

                // Then
                assertThat(result.isRight()).isEqualTo(true)
                result.onRight { playerList ->
                    assertThat(playerList).hasSize(2)
                }
            }

        @Test
        fun `should return error when fetching players fails`() =
            runTest {
                // Given
                playerGateway.setError(PlayerFailure.OperationFailed("Network error"))

                // When
                val result =
                    with(testUserContext) {
                        playerUseCases.getOnlinePlayers(testServerId)
                    }

                // Then
                assertThat(result.isLeft()).isEqualTo(true)
            }
    }
}

private class FakePlayerGateway(
    private val clock: Clock,
) : PlayerGateway {
    private val whitelists = mutableMapOf<GameServerId, List<PlayerListEntry>>()
    private val banlists = mutableMapOf<GameServerId, List<PlayerListEntry>>()
    private val onlinePlayers = mutableMapOf<GameServerId, List<Player>>()
    private var error: PlayerFailure? = null

    val addedToWhitelist = mutableListOf<Pair<GameServerId, PlayerIdentifier>>()
    val removedFromWhitelist = mutableListOf<Pair<GameServerId, PlayerIdentifier>>()
    val addedToBanlist = mutableListOf<Pair<GameServerId, PlayerIdentifier>>()
    val removedFromBanlist = mutableListOf<Pair<GameServerId, PlayerIdentifier>>()

    fun setWhitelist(
        serverId: GameServerId,
        entries: List<PlayerListEntry>,
    ) {
        whitelists[serverId] = entries
    }

    fun setBanlist(
        serverId: GameServerId,
        entries: List<PlayerListEntry>,
    ) {
        banlists[serverId] = entries
    }

    fun setOnlinePlayers(
        serverId: GameServerId,
        players: List<Player>,
    ) {
        onlinePlayers[serverId] = players
    }

    fun setError(failure: PlayerFailure) {
        error = failure
    }

    context(_: UserContext) override suspend fun fetchPlayerList(
        serverId: GameServerId,
        listType: PlayerListType,
    ): Either<PlayerFailure, PlayerList> {
        error?.let { return it.left() }

        val entries =
            when (listType) {
                PlayerListType.WHITELIST -> whitelists[serverId] ?: emptyList()
                PlayerListType.BANLIST -> banlists[serverId] ?: emptyList()
            }

        return PlayerList.create(serverId, listType, entries, clock).right()
    }

    context(_: UserContext) override suspend fun addToPlayerList(
        serverId: GameServerId,
        listType: PlayerListType,
        identifier: PlayerIdentifier,
    ): Either<PlayerFailure, Unit> {
        error?.let { return it.left() }

        when (listType) {
            PlayerListType.WHITELIST -> addedToWhitelist.add(serverId to identifier)
            PlayerListType.BANLIST -> addedToBanlist.add(serverId to identifier)
        }

        return Unit.right()
    }

    context(_: UserContext) override suspend fun removeFromPlayerList(
        serverId: GameServerId,
        listType: PlayerListType,
        identifier: PlayerIdentifier,
    ): Either<PlayerFailure, Unit> {
        error?.let { return it.left() }

        when (listType) {
            PlayerListType.WHITELIST -> removedFromWhitelist.add(serverId to identifier)
            PlayerListType.BANLIST -> removedFromBanlist.add(serverId to identifier)
        }

        return Unit.right()
    }

    context(_: UserContext) override suspend fun fetchOnlinePlayers(serverId: GameServerId): Either<PlayerFailure, List<Player>> {
        error?.let { return it.left() }

        return (onlinePlayers[serverId] ?: emptyList()).right()
    }
}

private class FakeEventPublisher : DomainEventPublisher {
    val publishedEvents = mutableListOf<DomainEvent<*>>()

    override suspend fun publish(events: List<DomainEvent<*>>) {
        publishedEvents.addAll(events)
    }
}

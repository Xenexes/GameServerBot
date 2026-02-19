@file:Suppress("ktlint:standard:max-line-length")

package de.xenexes.gameserverbot.unit.api

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import de.xenexes.gameserverbot.api.discord.DiscordCommandRegistry
import de.xenexes.gameserverbot.api.discord.DiscordHandler
import de.xenexes.gameserverbot.domain.discord.DiscordInteraction
import de.xenexes.gameserverbot.domain.discord.DiscordPermissionService
import de.xenexes.gameserverbot.domain.discord.DiscordRole
import de.xenexes.gameserverbot.domain.discord.DiscordRoleId
import de.xenexes.gameserverbot.domain.discord.DiscordUser
import de.xenexes.gameserverbot.domain.discord.DiscordUserId
import de.xenexes.gameserverbot.domain.discord.Permission
import de.xenexes.gameserverbot.domain.gameserver.GameServer
import de.xenexes.gameserverbot.domain.gameserver.GameServerId
import de.xenexes.gameserverbot.domain.gameserver.GameServerStatus
import de.xenexes.gameserverbot.domain.gameserver.NitradoServerId
import de.xenexes.gameserverbot.domain.player.Player
import de.xenexes.gameserverbot.domain.player.PlayerFailure
import de.xenexes.gameserverbot.domain.player.PlayerIdentifier
import de.xenexes.gameserverbot.domain.player.PlayerList
import de.xenexes.gameserverbot.domain.player.PlayerListType
import de.xenexes.gameserverbot.domain.shared.UserContext
import de.xenexes.gameserverbot.ports.outbound.NitradoGameInfo
import de.xenexes.gameserverbot.ports.outbound.NitradoGateway
import de.xenexes.gameserverbot.ports.outbound.NitradoPlayer
import de.xenexes.gameserverbot.ports.outbound.NitradoPlayerListEntry
import de.xenexes.gameserverbot.ports.outbound.NitradoServerInfo
import de.xenexes.gameserverbot.ports.outbound.NitradoServiceInfo
import de.xenexes.gameserverbot.ports.outbound.PlayerGateway
import de.xenexes.gameserverbot.ports.outbound.failure.NitradoFailure
import de.xenexes.gameserverbot.unit.usecases.FakeDomainEventPublisher
import de.xenexes.gameserverbot.unit.usecases.InMemoryGameServerRepositoryDouble
import de.xenexes.gameserverbot.usecases.GameServerUseCases
import de.xenexes.gameserverbot.usecases.PlayerUseCases
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class DiscordCommandRegistryTest {
    private lateinit var repository: InMemoryGameServerRepositoryDouble
    private lateinit var nitrado: ConfigurableNitradoGateway
    private lateinit var registry: DiscordCommandRegistry

    private val fixedInstant = Instant.parse("2024-01-15T10:00:00Z")

    private val testServer =
        GameServer.restore(
            id = GameServerId.create(),
            name = "Test Server",
            status = GameServerStatus.STARTED,
            nitradoId = NitradoServerId(12345),
            ip = "127.0.0.1",
            port = 25565,
            gameCode = "mc",
            gameName = "Minecraft",
            playerSlots = 20,
            location = "EU",
            createdAt = fixedInstant,
            updatedAt = fixedInstant,
        )

    private val adminUser =
        DiscordUser(
            id = DiscordUserId("123456789"),
            username = "admin",
            roles =
                setOf(
                    DiscordRole(
                        id = DiscordRoleId("admin-role"),
                        name = "Admin",
                        permissions = setOf(Permission.ADMIN),
                    ),
                ),
        )

    @BeforeEach
    fun setup() {
        repository = InMemoryGameServerRepositoryDouble()
        repository.addServer(testServer)
        nitrado = ConfigurableNitradoGateway()
        val eventPublisher = FakeDomainEventPublisher()
        val clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)
        val gameServerUseCases = GameServerUseCases(repository, nitrado, eventPublisher, clock)
        val playerUseCases = PlayerUseCases(MinimalPlayerGateway(), eventPublisher, clock)
        registry =
            DiscordCommandRegistry(
                gameServerUseCases = gameServerUseCases,
                playerUseCases = playerUseCases,
                discordHandler = DiscordHandler(),
                permissionService = DiscordPermissionService(),
            )
    }

    @Nested
    @DisplayName("HandleServerGame")
    inner class HandleServerGameTests {
        @Nested
        inner class ListSubcommand {
            @Test
            fun `should display folderShort and gameId for each game in list`() =
                runTest {
                    // Given
                    nitrado.gameListResult =
                        listOf(
                            NitradoGameInfo(
                                gameId = "mc",
                                folderShort = "mc",
                                gameHuman = "Minecraft Vanilla",
                                installed = true,
                                active = true,
                            ),
                            NitradoGameInfo(
                                gameId = "sevendtd",
                                folderShort = "sevendtd",
                                gameHuman = "7 Days to Die",
                                installed = false,
                                active = false,
                            ),
                        )
                    val interaction = fakeInteraction(subcommand = "list")

                    // When
                    registry.handleCommand(interaction)

                    // Then
                    val response = interaction.editedResponse!!
                    assertThat(response).contains("`mc` | id: `mc`")
                    assertThat(response).contains("`sevendtd` | id: `sevendtd`")
                }

            @Test
            fun `should show switch hint on list page`() =
                runTest {
                    // Given
                    nitrado.gameListResult =
                        listOf(
                            NitradoGameInfo(
                                gameId = "mc",
                                folderShort = "mc",
                                gameHuman = "Minecraft Vanilla",
                                installed = true,
                                active = true,
                            ),
                        )
                    val interaction = fakeInteraction(subcommand = "list")

                    // When
                    registry.handleCommand(interaction)

                    // Then
                    assertThat(interaction.editedResponse!!).contains("/server-game switch game-id:<folder-short>")
                }

            @Test
            fun `should show switch hint on filtered list`() =
                runTest {
                    // Given
                    nitrado.gameListResult =
                        listOf(
                            NitradoGameInfo(
                                gameId = "mc",
                                folderShort = "mc",
                                gameHuman = "Minecraft Vanilla",
                                installed = true,
                                active = true,
                            ),
                            NitradoGameInfo(
                                gameId = "sevendtd",
                                folderShort = "sevendtd",
                                gameHuman = "7 Days to Die",
                                installed = false,
                                active = false,
                            ),
                        )
                    val interaction =
                        fakeInteraction(subcommand = "list", stringParams = mapOf("filter" to "Minecraft"))

                    // When
                    registry.handleCommand(interaction)

                    // Then
                    val response = interaction.editedResponse!!
                    assertThat(response).contains("Minecraft Vanilla")
                    assertThat(response).contains("/server-game switch game-id:<folder-short>")
                }

            @Test
            fun `should paginate correctly when games exceed page size`() =
                runTest {
                    // Given - 16 games, PAGE_SIZE = 15, page 2 has the 16th game
                    nitrado.gameListResult =
                        (1..16).map { i ->
                            NitradoGameInfo(
                                gameId = "game$i",
                                folderShort = "game$i",
                                gameHuman = "Game $i",
                                installed = false,
                                active = false,
                            )
                        }
                    val interaction = fakeInteraction(subcommand = "list", intParams = mapOf("page" to 2))

                    // When
                    registry.handleCommand(interaction)

                    // Then
                    val response = interaction.editedResponse!!
                    assertThat(response).contains("Game 16")
                    assertThat(response).contains("Page 2/2")
                }
        }

        @Nested
        inner class InstalledSubcommand {
            @Test
            fun `should display active game with folderShort and gameId`() =
                runTest {
                    // Given
                    nitrado.gameListResult =
                        listOf(
                            NitradoGameInfo(
                                gameId = "mc",
                                folderShort = "mc",
                                gameHuman = "Minecraft Vanilla",
                                installed = true,
                                active = true,
                            ),
                            NitradoGameInfo(
                                gameId = "sevendtd",
                                folderShort = "sevendtd",
                                gameHuman = "7 Days to Die",
                                installed = true,
                                active = false,
                            ),
                        )
                    val interaction = fakeInteraction(subcommand = "installed")

                    // When
                    registry.handleCommand(interaction)

                    // Then
                    val response = interaction.editedResponse!!
                    assertThat(response).contains("`mc` | id: `mc`")
                    assertThat(response).contains("— Active")
                    assertThat(response).contains("`sevendtd` | id: `sevendtd`")
                }

            @Test
            fun `should show switch hint on installed list`() =
                runTest {
                    // Given
                    nitrado.gameListResult =
                        listOf(
                            NitradoGameInfo(
                                gameId = "mc",
                                folderShort = "mc",
                                gameHuman = "Minecraft Vanilla",
                                installed = true,
                                active = true,
                            ),
                        )
                    val interaction = fakeInteraction(subcommand = "installed")

                    // When
                    registry.handleCommand(interaction)

                    // Then
                    assertThat(interaction.editedResponse!!).contains("/server-game switch game-id:<folder-short>")
                }
        }

        @Nested
        inner class SwitchSubcommand {
            @Test
            fun `should call switchGame with provided game id`() =
                runTest {
                    // Given
                    val interaction = fakeInteraction(subcommand = "switch", stringParams = mapOf("game-id" to "mc"))

                    // When
                    registry.handleCommand(interaction)

                    // Then
                    assertThat(nitrado.switchGameCalls).hasSize(1)
                    assertThat(nitrado.switchGameCalls.first().second).isEqualTo("mc")
                    assertThat(interaction.editedResponse!!).contains("mc")
                }
        }
    }

    private fun fakeInteraction(
        commandName: String = "server-game",
        subcommand: String? = null,
        stringParams: Map<String, String> = emptyMap(),
        intParams: Map<String, Int> = emptyMap(),
    ): FakeDiscordInteraction =
        FakeDiscordInteraction(
            commandName = commandName,
            user = adminUser,
            subcommand = subcommand,
            stringParams = stringParams,
            intParams = intParams,
        )
}

private class FakeDiscordInteraction(
    override val commandName: String,
    private val user: DiscordUser,
    private val subcommand: String? = null,
    private val stringParams: Map<String, String> = emptyMap(),
    private val intParams: Map<String, Int> = emptyMap(),
) : DiscordInteraction {
    var editedResponse: String? = null
    var immediateResponse: String? = null

    override suspend fun getUser(): DiscordUser = user

    override suspend fun defer() {}

    override suspend fun editResponse(message: String) {
        editedResponse = message
    }

    override suspend fun respondImmediately(message: String) {
        immediateResponse = message
    }

    override fun getSubcommandName(): String? = subcommand

    override fun getStringParameter(name: String): String? = stringParams[name]

    override fun getIntParameter(name: String): Int? = intParams[name]
}

private class ConfigurableNitradoGateway : NitradoGateway {
    var gameListResult: List<NitradoGameInfo> = emptyList()
    val switchGameCalls = mutableListOf<Pair<NitradoServerId, String>>()

    override suspend fun startServer(nitradoId: NitradoServerId): Either<NitradoFailure, Unit> = Unit.right()

    override suspend fun stopServer(
        nitradoId: NitradoServerId,
        message: String?,
    ): Either<NitradoFailure, Unit> = Unit.right()

    override suspend fun restartServer(
        nitradoId: NitradoServerId,
        message: String?,
    ): Either<NitradoFailure, Unit> = Unit.right()

    override suspend fun fetchServerInfo(nitradoId: NitradoServerId): Either<NitradoFailure, NitradoServerInfo> =
        NitradoFailure.ServerNotFound(nitradoId).left()

    override suspend fun fetchServices(): Either<NitradoFailure, List<NitradoServiceInfo>> =
        emptyList<NitradoServiceInfo>().right()

    override suspend fun fetchGameList(nitradoId: NitradoServerId): Either<NitradoFailure, List<NitradoGameInfo>> =
        gameListResult.right()

    override suspend fun fetchPlayers(nitradoId: NitradoServerId): Either<NitradoFailure, List<NitradoPlayer>> =
        emptyList<NitradoPlayer>().right()

    override suspend fun switchGame(
        nitradoId: NitradoServerId,
        gameId: String,
    ): Either<NitradoFailure, Unit> {
        switchGameCalls.add(nitradoId to gameId)
        return Unit.right()
    }

    override suspend fun fetchWhitelist(
        nitradoId: NitradoServerId,
    ): Either<NitradoFailure, List<NitradoPlayerListEntry>> = emptyList<NitradoPlayerListEntry>().right()

    override suspend fun addToWhitelist(
        nitradoId: NitradoServerId,
        identifier: String,
    ): Either<NitradoFailure, Unit> = Unit.right()

    override suspend fun removeFromWhitelist(
        nitradoId: NitradoServerId,
        identifier: String,
    ): Either<NitradoFailure, Unit> = Unit.right()

    override suspend fun fetchBanlist(
        nitradoId: NitradoServerId,
    ): Either<NitradoFailure, List<NitradoPlayerListEntry>> = emptyList<NitradoPlayerListEntry>().right()

    override suspend fun addToBanlist(
        nitradoId: NitradoServerId,
        identifier: String,
    ): Either<NitradoFailure, Unit> = Unit.right()

    override suspend fun removeFromBanlist(
        nitradoId: NitradoServerId,
        identifier: String,
    ): Either<NitradoFailure, Unit> = Unit.right()
}

private class MinimalPlayerGateway : PlayerGateway {
    private val clock = Clock.fixed(Instant.parse("2024-01-15T10:00:00Z"), ZoneOffset.UTC)

    context(_: UserContext) override suspend fun fetchPlayerList(
        serverId: GameServerId,
        listType: PlayerListType,
    ): Either<PlayerFailure, PlayerList> = PlayerList.create(serverId, listType, emptyList(), clock).right()

    context(_: UserContext) override suspend fun addToPlayerList(
        serverId: GameServerId,
        listType: PlayerListType,
        identifier: PlayerIdentifier,
    ): Either<PlayerFailure, Unit> = Unit.right()

    context(_: UserContext) override suspend fun removeFromPlayerList(
        serverId: GameServerId,
        listType: PlayerListType,
        identifier: PlayerIdentifier,
    ): Either<PlayerFailure, Unit> = Unit.right()

    context(_: UserContext) override suspend fun fetchOnlinePlayers(serverId: GameServerId): Either<PlayerFailure, List<Player>> =
        emptyList<Player>()
            .right()
}

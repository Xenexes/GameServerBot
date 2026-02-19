package de.xenexes.gameserverbot.unit.infrastructure

import arrow.core.left
import arrow.core.right
import assertk.assertThat
import assertk.assertions.isEqualTo
import de.xenexes.gameserverbot.domain.gameserver.GameServerStatus
import de.xenexes.gameserverbot.domain.gameserver.NitradoServerId
import de.xenexes.gameserverbot.infrastructure.http.nitrado.CacheConfig
import de.xenexes.gameserverbot.infrastructure.http.nitrado.CachingNitradoGateway
import de.xenexes.gameserverbot.ports.outbound.NitradoGameInfo
import de.xenexes.gameserverbot.ports.outbound.NitradoGateway
import de.xenexes.gameserverbot.ports.outbound.NitradoPlayer
import de.xenexes.gameserverbot.ports.outbound.NitradoPlayerListEntry
import de.xenexes.gameserverbot.ports.outbound.NitradoServerInfo
import de.xenexes.gameserverbot.ports.outbound.NitradoServerQuery
import de.xenexes.gameserverbot.ports.outbound.NitradoServiceDetails
import de.xenexes.gameserverbot.ports.outbound.NitradoServiceInfo
import de.xenexes.gameserverbot.ports.outbound.failure.NitradoFailure
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration

class CachingNitradoGatewayTest {
    private lateinit var delegate: NitradoGateway
    private lateinit var cachingGateway: CachingNitradoGateway

    private val testServerId = NitradoServerId(12345)

    @BeforeEach
    fun setup() {
        delegate = mockk()
        cachingGateway =
            CachingNitradoGateway(
                delegate,
                CacheConfig(
                    serverStatusTtl = Duration.ofMinutes(5),
                    servicesTtl = Duration.ofMinutes(5),
                    gameListTtl = Duration.ofMinutes(5),
                    playersTtl = Duration.ofMinutes(5),
                    playerListTtl = Duration.ofMinutes(5),
                    maximumSize = 100,
                ),
            )
    }

    @Nested
    @DisplayName("fetchServerInfo caching")
    inner class FetchServerInfoTests {
        private val serverInfo =
            NitradoServerInfo(
                nitradoId = testServerId,
                status = GameServerStatus.STARTED,
                ip = "127.0.0.1",
                port = 25565,
                gameCode = "mc",
                gameName = "Minecraft",
                slots = 10,
                location = "DE",
                query =
                    NitradoServerQuery(
                        serverName = "Test",
                        connectIp = "127.0.0.1:25565",
                        map = "world",
                        version = "1.20",
                        playersCurrent = 2,
                        playersMax = 10,
                    ),
            )

        @Test
        fun `should call delegate on cache miss`() =
            runTest {
                coEvery { delegate.fetchServerInfo(testServerId) } returns serverInfo.right()

                val result = cachingGateway.fetchServerInfo(testServerId)

                assertThat(result.isRight()).isEqualTo(true)
                result.onRight { assertThat(it).isEqualTo(serverInfo) }
                coVerify(exactly = 1) { delegate.fetchServerInfo(testServerId) }
            }

        @Test
        fun `should return cached value on cache hit`() =
            runTest {
                coEvery { delegate.fetchServerInfo(testServerId) } returns serverInfo.right()

                cachingGateway.fetchServerInfo(testServerId)
                val result = cachingGateway.fetchServerInfo(testServerId)

                assertThat(result.isRight()).isEqualTo(true)
                result.onRight { assertThat(it).isEqualTo(serverInfo) }
                coVerify(exactly = 1) { delegate.fetchServerInfo(testServerId) }
            }

        @Test
        fun `should not cache error responses`() =
            runTest {
                val error = NitradoFailure.ApiError("Server error", 500)
                coEvery { delegate.fetchServerInfo(testServerId) } returns error.left()

                cachingGateway.fetchServerInfo(testServerId)
                cachingGateway.fetchServerInfo(testServerId)

                coVerify(exactly = 2) { delegate.fetchServerInfo(testServerId) }
            }

        @Test
        fun `should evict cache on startServer`() =
            runTest {
                coEvery { delegate.fetchServerInfo(testServerId) } returns serverInfo.right()
                coEvery { delegate.startServer(testServerId) } returns Unit.right()

                cachingGateway.fetchServerInfo(testServerId)
                cachingGateway.startServer(testServerId)
                cachingGateway.fetchServerInfo(testServerId)

                coVerify(exactly = 2) { delegate.fetchServerInfo(testServerId) }
            }

        @Test
        fun `should evict cache on stopServer`() =
            runTest {
                coEvery { delegate.fetchServerInfo(testServerId) } returns serverInfo.right()
                coEvery { delegate.stopServer(testServerId, any()) } returns Unit.right()

                cachingGateway.fetchServerInfo(testServerId)
                cachingGateway.stopServer(testServerId, "maintenance")
                cachingGateway.fetchServerInfo(testServerId)

                coVerify(exactly = 2) { delegate.fetchServerInfo(testServerId) }
            }

        @Test
        fun `should evict cache on restartServer`() =
            runTest {
                coEvery { delegate.fetchServerInfo(testServerId) } returns serverInfo.right()
                coEvery { delegate.restartServer(testServerId, any()) } returns Unit.right()

                cachingGateway.fetchServerInfo(testServerId)
                cachingGateway.restartServer(testServerId)
                cachingGateway.fetchServerInfo(testServerId)

                coVerify(exactly = 2) { delegate.fetchServerInfo(testServerId) }
            }

        @Test
        fun `should not evict cache when mutation fails`() =
            runTest {
                val error = NitradoFailure.ApiError("Failed", 500)
                coEvery { delegate.fetchServerInfo(testServerId) } returns serverInfo.right()
                coEvery { delegate.startServer(testServerId) } returns error.left()

                cachingGateway.fetchServerInfo(testServerId)
                cachingGateway.startServer(testServerId)
                cachingGateway.fetchServerInfo(testServerId)

                coVerify(exactly = 1) { delegate.fetchServerInfo(testServerId) }
            }
    }

    @Nested
    @DisplayName("fetchServices caching")
    inner class FetchServicesTests {
        private val services =
            listOf(
                NitradoServiceInfo(
                    serviceId = 1,
                    status = "active",
                    typeHuman = "Gameserver",
                    details = NitradoServiceDetails("127.0.0.1", "Server", "mc", 10),
                ),
            )

        @Test
        fun `should cache services list`() =
            runTest {
                coEvery { delegate.fetchServices() } returns services.right()

                cachingGateway.fetchServices()
                cachingGateway.fetchServices()

                coVerify(exactly = 1) { delegate.fetchServices() }
            }
    }

    @Nested
    @DisplayName("fetchGameList caching")
    inner class FetchGameListTests {
        private val games =
            listOf(
                NitradoGameInfo(
                    gameId = "mc",
                    folderShort = "mc",
                    gameHuman = "Minecraft",
                    installed = true,
                    active = true,
                ),
            )

        @Test
        fun `should cache game list`() =
            runTest {
                coEvery { delegate.fetchGameList(testServerId) } returns games.right()

                cachingGateway.fetchGameList(testServerId)
                cachingGateway.fetchGameList(testServerId)

                coVerify(exactly = 1) { delegate.fetchGameList(testServerId) }
            }

        @Test
        fun `should evict game list on switchGame`() =
            runTest {
                coEvery { delegate.fetchGameList(testServerId) } returns games.right()
                coEvery { delegate.switchGame(testServerId, "mc") } returns Unit.right()

                cachingGateway.fetchGameList(testServerId)
                cachingGateway.switchGame(testServerId, "mc")
                cachingGateway.fetchGameList(testServerId)

                coVerify(exactly = 2) { delegate.fetchGameList(testServerId) }
            }
    }

    @Nested
    @DisplayName("fetchPlayers caching")
    inner class FetchPlayersTests {
        private val players =
            listOf(
                NitradoPlayer(name = "Player1", online = true),
            )

        @Test
        fun `should cache players list`() =
            runTest {
                coEvery { delegate.fetchPlayers(testServerId) } returns players.right()

                cachingGateway.fetchPlayers(testServerId)
                cachingGateway.fetchPlayers(testServerId)

                coVerify(exactly = 1) { delegate.fetchPlayers(testServerId) }
            }
    }

    @Nested
    @DisplayName("whitelist caching")
    inner class WhitelistTests {
        private val whitelist =
            listOf(
                NitradoPlayerListEntry(name = "Player1", id = "123", idType = "steam-id"),
            )

        @Test
        fun `should cache whitelist`() =
            runTest {
                coEvery { delegate.fetchWhitelist(testServerId) } returns whitelist.right()

                cachingGateway.fetchWhitelist(testServerId)
                cachingGateway.fetchWhitelist(testServerId)

                coVerify(exactly = 1) { delegate.fetchWhitelist(testServerId) }
            }

        @Test
        fun `should evict whitelist on addToWhitelist`() =
            runTest {
                coEvery { delegate.fetchWhitelist(testServerId) } returns whitelist.right()
                coEvery { delegate.addToWhitelist(testServerId, "NewPlayer") } returns Unit.right()

                cachingGateway.fetchWhitelist(testServerId)
                cachingGateway.addToWhitelist(testServerId, "NewPlayer")
                cachingGateway.fetchWhitelist(testServerId)

                coVerify(exactly = 2) { delegate.fetchWhitelist(testServerId) }
            }

        @Test
        fun `should evict whitelist on removeFromWhitelist`() =
            runTest {
                coEvery { delegate.fetchWhitelist(testServerId) } returns whitelist.right()
                coEvery { delegate.removeFromWhitelist(testServerId, "Player1") } returns Unit.right()

                cachingGateway.fetchWhitelist(testServerId)
                cachingGateway.removeFromWhitelist(testServerId, "Player1")
                cachingGateway.fetchWhitelist(testServerId)

                coVerify(exactly = 2) { delegate.fetchWhitelist(testServerId) }
            }
    }

    @Nested
    @DisplayName("banlist caching")
    inner class BanlistTests {
        private val banlist =
            listOf(
                NitradoPlayerListEntry(name = "Banned1", id = "456", idType = "steam-id"),
            )

        @Test
        fun `should cache banlist`() =
            runTest {
                coEvery { delegate.fetchBanlist(testServerId) } returns banlist.right()

                cachingGateway.fetchBanlist(testServerId)
                cachingGateway.fetchBanlist(testServerId)

                coVerify(exactly = 1) { delegate.fetchBanlist(testServerId) }
            }

        @Test
        fun `should evict banlist on addToBanlist`() =
            runTest {
                coEvery { delegate.fetchBanlist(testServerId) } returns banlist.right()
                coEvery { delegate.addToBanlist(testServerId, "Cheater") } returns Unit.right()

                cachingGateway.fetchBanlist(testServerId)
                cachingGateway.addToBanlist(testServerId, "Cheater")
                cachingGateway.fetchBanlist(testServerId)

                coVerify(exactly = 2) { delegate.fetchBanlist(testServerId) }
            }

        @Test
        fun `should evict banlist on removeFromBanlist`() =
            runTest {
                coEvery { delegate.fetchBanlist(testServerId) } returns banlist.right()
                coEvery { delegate.removeFromBanlist(testServerId, "Banned1") } returns Unit.right()

                cachingGateway.fetchBanlist(testServerId)
                cachingGateway.removeFromBanlist(testServerId, "Banned1")
                cachingGateway.fetchBanlist(testServerId)

                coVerify(exactly = 2) { delegate.fetchBanlist(testServerId) }
            }
    }

    @Nested
    @DisplayName("cache isolation")
    inner class CacheIsolationTests {
        @Test
        fun `should cache different servers independently`() =
            runTest {
                val serverId1 = NitradoServerId(111)
                val serverId2 = NitradoServerId(222)

                val info1 =
                    NitradoServerInfo(
                        nitradoId = serverId1,
                        status = GameServerStatus.STARTED,
                        ip = "1.1.1.1",
                        port = 25565,
                        gameCode = "mc",
                        gameName = "Minecraft",
                        slots = 10,
                        location = "DE",
                    )
                val info2 =
                    NitradoServerInfo(
                        nitradoId = serverId2,
                        status = GameServerStatus.STOPPED,
                        ip = "2.2.2.2",
                        port = 25565,
                        gameCode = "mc",
                        gameName = "Minecraft",
                        slots = 20,
                        location = "US",
                    )

                coEvery { delegate.fetchServerInfo(serverId1) } returns info1.right()
                coEvery { delegate.fetchServerInfo(serverId2) } returns info2.right()

                val result1 = cachingGateway.fetchServerInfo(serverId1)
                val result2 = cachingGateway.fetchServerInfo(serverId2)

                assertThat(result1.isRight()).isEqualTo(true)
                assertThat(result2.isRight()).isEqualTo(true)
                result1.onRight { assertThat(it.ip).isEqualTo("1.1.1.1") }
                result2.onRight { assertThat(it.ip).isEqualTo("2.2.2.2") }

                coVerify(exactly = 1) { delegate.fetchServerInfo(serverId1) }
                coVerify(exactly = 1) { delegate.fetchServerInfo(serverId2) }
            }

        @Test
        fun `should only evict cache for the affected server`() =
            runTest {
                val serverId1 = NitradoServerId(111)
                val serverId2 = NitradoServerId(222)

                val info1 =
                    NitradoServerInfo(
                        nitradoId = serverId1,
                        status = GameServerStatus.STARTED,
                        ip = "1.1.1.1",
                        port = 25565,
                        gameCode = "mc",
                        gameName = "Minecraft",
                        slots = 10,
                        location = "DE",
                    )
                val info2 =
                    NitradoServerInfo(
                        nitradoId = serverId2,
                        status = GameServerStatus.STOPPED,
                        ip = "2.2.2.2",
                        port = 25565,
                        gameCode = "mc",
                        gameName = "Minecraft",
                        slots = 20,
                        location = "US",
                    )

                coEvery { delegate.fetchServerInfo(serverId1) } returns info1.right()
                coEvery { delegate.fetchServerInfo(serverId2) } returns info2.right()
                coEvery { delegate.startServer(serverId1) } returns Unit.right()

                cachingGateway.fetchServerInfo(serverId1)
                cachingGateway.fetchServerInfo(serverId2)
                cachingGateway.startServer(serverId1)
                cachingGateway.fetchServerInfo(serverId1)
                cachingGateway.fetchServerInfo(serverId2)

                coVerify(exactly = 2) { delegate.fetchServerInfo(serverId1) }
                coVerify(exactly = 1) { delegate.fetchServerInfo(serverId2) }
            }
    }
}

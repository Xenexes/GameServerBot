@file:Suppress("ktlint:standard:max-line-length")

package de.xenexes.gameserverbot.integration

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.delete
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import de.xenexes.gameserverbot.domain.gameserver.GameServerStatus
import de.xenexes.gameserverbot.domain.gameserver.NitradoServerId
import de.xenexes.gameserverbot.infrastructure.http.nitrado.KtorNitradoGateway
import de.xenexes.gameserverbot.infrastructure.http.nitrado.NitradoProperties
import de.xenexes.gameserverbot.ports.outbound.failure.NitradoFailure
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class KtorNitradoGatewayIT {
    private lateinit var wireMockServer: WireMockServer
    private lateinit var gateway: KtorNitradoGateway

    private val testServerId = NitradoServerId(17256046)

    @BeforeEach
    fun setup() {
        wireMockServer =
            WireMockServer(
                wireMockConfig()
                    .dynamicPort()
                    .usingFilesUnderClasspath("wiremock/nitrado"),
            )
        wireMockServer.start()

        val properties =
            NitradoProperties(
                baseUrl = "http://localhost:${wireMockServer.port()}",
                apiToken = "test-token",
            )
        gateway = KtorNitradoGateway(properties)
    }

    @AfterEach
    fun teardown() {
        wireMockServer.stop()
    }

    @Nested
    @DisplayName("Server Info")
    inner class ServerInfoTests {
        @Test
        fun `should fetch server info successfully`() =
            runTest {
                // Given
                wireMockServer.stubFor(
                    get(urlPathEqualTo("/services/${testServerId.value}/gameservers"))
                        .willReturn(
                            aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBodyFile("response-GET-services-id-gameservers.200.json"),
                        ),
                )

                // When
                val result = gateway.fetchServerInfo(testServerId)

                // Then
                assertThat(result.isRight()).isEqualTo(true)
                result.onRight { info ->
                    assertThat(info.status).isEqualTo(GameServerStatus.STARTED)
                    assertThat(info.ip).isEqualTo("127.0.0.1")
                    assertThat(info.port).isEqualTo(25565)
                    assertThat(info.gameCode).isEqualTo("mc")
                    assertThat(info.gameName).isEqualTo("Minecraft Vanilla")
                    assertThat(info.slots).isEqualTo(4)
                    assertThat(info.location).isEqualTo("DE")
                    assertThat(info.query).isNotNull()
                    assertThat(info.query?.playersCurrent).isEqualTo(1)
                    assertThat(info.query?.playersMax).isEqualTo(4)
                }
            }

        @Test
        fun `should return api error on error response`() =
            runTest {
                // Given
                wireMockServer.stubFor(
                    get(urlPathEqualTo("/services/${testServerId.value}/gameservers"))
                        .willReturn(
                            aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody("""{"status": "error", "message": "Internal server error"}"""),
                        ),
                )

                // When
                val result = gateway.fetchServerInfo(testServerId)

                // Then
                assertThat(result.isLeft()).isEqualTo(true)
                result.onLeft { error ->
                    assertThat(error).isInstanceOf(NitradoFailure.ApiError::class)
                }
            }
    }

    @Nested
    @DisplayName("Server Control")
    inner class ServerControlTests {
        @Test
        fun `should start server successfully`() =
            runTest {
                // Given
                wireMockServer.stubFor(
                    post(urlPathEqualTo("/services/${testServerId.value}/gameservers/restart"))
                        .willReturn(
                            aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBodyFile("response-GET-services-id-gameservers-restart.200.json"),
                        ),
                )

                // When
                val result = gateway.startServer(testServerId)

                // Then
                assertThat(result.isRight()).isEqualTo(true)
            }

        @Test
        fun `should stop server successfully`() =
            runTest {
                // Given
                wireMockServer.stubFor(
                    post(urlPathEqualTo("/services/${testServerId.value}/gameservers/stop"))
                        .willReturn(
                            aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBodyFile("response-GET-services-id-gameservers-stop.200.json"),
                        ),
                )

                // When
                val result = gateway.stopServer(testServerId, "Server maintenance")

                // Then
                assertThat(result.isRight()).isEqualTo(true)
            }

        @Test
        fun `should restart server successfully`() =
            runTest {
                // Given
                wireMockServer.stubFor(
                    post(urlPathEqualTo("/services/${testServerId.value}/gameservers/restart"))
                        .willReturn(
                            aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBodyFile("response-GET-services-id-gameservers-restart.200.json"),
                        ),
                )

                // When
                val result = gateway.restartServer(testServerId)

                // Then
                assertThat(result.isRight()).isEqualTo(true)
            }

        @Test
        fun `should return invalid token error on 401`() =
            runTest {
                // Given
                wireMockServer.stubFor(
                    post(urlPathEqualTo("/services/${testServerId.value}/gameservers/restart"))
                        .willReturn(
                            aResponse()
                                .withStatus(401)
                                .withHeader("Content-Type", "application/json")
                                .withBodyFile("response-error-401.json"),
                        ),
                )

                // When
                val result = gateway.restartServer(testServerId)

                // Then
                assertThat(result.isLeft()).isEqualTo(true)
                result.onLeft { error ->
                    assertThat(error).isInstanceOf(NitradoFailure.InvalidToken::class)
                }
            }

        @Test
        fun `should return server not found error on 404`() =
            runTest {
                // Given
                wireMockServer.stubFor(
                    post(urlPathEqualTo("/services/${testServerId.value}/gameservers/restart"))
                        .willReturn(
                            aResponse()
                                .withStatus(404)
                                .withHeader("Content-Type", "application/json")
                                .withBodyFile("response-error-404.json"),
                        ),
                )

                // When
                val result = gateway.restartServer(testServerId)

                // Then
                assertThat(result.isLeft()).isEqualTo(true)
                result.onLeft { error ->
                    assertThat(error).isInstanceOf(NitradoFailure.ServerNotFound::class)
                }
            }

        @Test
        fun `should return rate limit error on 429`() =
            runTest {
                // Given
                wireMockServer.stubFor(
                    post(urlPathEqualTo("/services/${testServerId.value}/gameservers/restart"))
                        .willReturn(
                            aResponse()
                                .withStatus(429)
                                .withHeader("Content-Type", "application/json")
                                .withBodyFile("response-error-429.json"),
                        ),
                )

                // When
                val result = gateway.restartServer(testServerId)

                // Then
                assertThat(result.isLeft()).isEqualTo(true)
                result.onLeft { error ->
                    assertThat(error).isInstanceOf(NitradoFailure.RateLimitExceeded::class)
                }
            }

        @Test
        fun `should return service unavailable error on 503`() =
            runTest {
                // Given
                wireMockServer.stubFor(
                    post(urlPathEqualTo("/services/${testServerId.value}/gameservers/restart"))
                        .willReturn(
                            aResponse()
                                .withStatus(503)
                                .withHeader("Content-Type", "application/json")
                                .withBodyFile("response-error-503.json"),
                        ),
                )

                // When
                val result = gateway.restartServer(testServerId)

                // Then
                assertThat(result.isLeft()).isEqualTo(true)
                result.onLeft { error ->
                    assertThat(error).isInstanceOf(NitradoFailure.ServiceUnavailable::class)
                }
            }
    }

    @Nested
    @DisplayName("Services")
    inner class ServicesTests {
        @Test
        fun `should fetch services list successfully`() =
            runTest {
                // Given
                wireMockServer.stubFor(
                    get(urlPathEqualTo("/services"))
                        .willReturn(
                            aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBodyFile("response-GET-service-list.200.json"),
                        ),
                )

                // When
                val result = gateway.fetchServices()

                // Then
                assertThat(result.isRight()).isEqualTo(true)
                result.onRight { services ->
                    assertThat(services).hasSize(2)
                    assertThat(services[0].serviceId).isEqualTo(3L)
                    assertThat(services[0].status).isEqualTo("active")
                    assertThat(services[0].details.name).isEqualTo("Nitrado.net Battlefield 4 Server")
                    assertThat(services[1].serviceId).isEqualTo(6L)
                    assertThat(services[1].details.name).isEqualTo("Nitrado.net Minecraft Server")
                }
            }
    }

    @Nested
    @DisplayName("Games")
    inner class GamesTests {
        @Test
        fun `should fetch game list successfully`() =
            runTest {
                // Given
                wireMockServer.stubFor(
                    get(urlPathEqualTo("/services/${testServerId.value}/gameservers/games"))
                        .willReturn(
                            aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBodyFile("response-GET-services-id-gameservers-games.200.json"),
                        ),
                )

                // When
                val result = gateway.fetchGameList(testServerId)

                // Then
                assertThat(result.isRight()).isEqualTo(true)
                result.onRight { games ->
                    assertThat(games).hasSize(2)
                    assertThat(games[0].gameId).isEqualTo("mc")
                    assertThat(games[0].folderShort).isEqualTo("mc")
                    assertThat(games[0].gameHuman).isEqualTo("Minecraft Vanilla")
                    assertThat(games[0].installed).isEqualTo(true)
                    assertThat(games[0].active).isEqualTo(true)
                    assertThat(games[1].gameId).isEqualTo("sevendtd")
                    assertThat(games[1].folderShort).isEqualTo("sevendtd")
                    assertThat(games[1].installed).isEqualTo(false)
                    assertThat(games[1].active).isEqualTo(false)
                }
            }

        @Test
        fun `should switch game successfully`() =
            runTest {
                // Given
                wireMockServer.stubFor(
                    post(urlPathEqualTo("/services/${testServerId.value}/gameservers/games/start"))
                        .willReturn(
                            aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBodyFile("response-POST-games-start.200.json"),
                        ),
                )

                // When
                val result = gateway.switchGame(testServerId, "mc")

                // Then
                assertThat(result.isRight()).isEqualTo(true)
            }

        @Test
        fun `should switch game with form-urlencoded body`() =
            runTest {
                // Given
                wireMockServer.stubFor(
                    post(urlPathEqualTo("/services/${testServerId.value}/gameservers/games/start"))
                        .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
                        .withRequestBody(containing("game=mc"))
                        .willReturn(
                            aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBodyFile("response-POST-games-start.200.json"),
                        ),
                )

                // When
                val result = gateway.switchGame(testServerId, "mc")

                // Then
                assertThat(result.isRight()).isEqualTo(true)
            }
    }

    @Nested
    @DisplayName("Players")
    inner class PlayersTests {
        @Test
        fun `should fetch players successfully`() =
            runTest {
                // Given
                wireMockServer.stubFor(
                    get(urlPathEqualTo("/services/${testServerId.value}/gameservers/games/players"))
                        .willReturn(
                            aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBodyFile("response-GET-services-id-gameservers-games-players.200.json"),
                        ),
                )

                // When
                val result = gateway.fetchPlayers(testServerId)

                // Then
                assertThat(result.isRight()).isEqualTo(true)
            }
    }

    @Nested
    @DisplayName("Whitelist")
    inner class WhitelistTests {
        @Test
        fun `should fetch whitelist successfully`() =
            runTest {
                // Given
                wireMockServer.stubFor(
                    get(urlPathEqualTo("/services/${testServerId.value}/gameservers/games/whitelist"))
                        .willReturn(
                            aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBodyFile("response-GET-services-id-gameservers-games-whitelist.200.json"),
                        ),
                )

                // When
                val result = gateway.fetchWhitelist(testServerId)

                // Then
                assertThat(result.isRight()).isEqualTo(true)
                result.onRight { whitelist ->
                    assertThat(whitelist).hasSize(2)
                    assertThat(whitelist[0].name).isEqualTo("Player1")
                    assertThat(whitelist[0].id).isEqualTo("76561198000000001")
                    assertThat(whitelist[0].idType).isEqualTo("steam-id")
                }
            }

        @Test
        fun `should add to whitelist successfully`() =
            runTest {
                // Given
                wireMockServer.stubFor(
                    post(urlPathEqualTo("/services/${testServerId.value}/gameservers/games/whitelist"))
                        .willReturn(
                            aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBodyFile("response-POST-whitelist.200.json"),
                        ),
                )

                // When
                val result = gateway.addToWhitelist(testServerId, "NewPlayer")

                // Then
                assertThat(result.isRight()).isEqualTo(true)
            }

        @Test
        fun `should remove from whitelist successfully`() =
            runTest {
                // Given
                wireMockServer.stubFor(
                    delete(urlPathEqualTo("/services/${testServerId.value}/gameservers/games/whitelist"))
                        .willReturn(
                            aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBodyFile("response-DELETE-whitelist.200.json"),
                        ),
                )

                // When
                val result = gateway.removeFromWhitelist(testServerId, "Player1")

                // Then
                assertThat(result.isRight()).isEqualTo(true)
            }
    }

    @Nested
    @DisplayName("Banlist")
    inner class BanlistTests {
        @Test
        fun `should fetch banlist successfully`() =
            runTest {
                // Given
                wireMockServer.stubFor(
                    get(urlPathEqualTo("/services/${testServerId.value}/gameservers/games/banlist"))
                        .willReturn(
                            aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBodyFile("response-GET-services-id-gameservers-games-banlist.200.json"),
                        ),
                )

                // When
                val result = gateway.fetchBanlist(testServerId)

                // Then
                assertThat(result.isRight()).isEqualTo(true)
                result.onRight { banlist ->
                    assertThat(banlist).hasSize(1)
                    assertThat(banlist[0].name).isEqualTo("BannedPlayer1")
                }
            }

        @Test
        fun `should add to banlist successfully`() =
            runTest {
                // Given
                wireMockServer.stubFor(
                    post(urlPathEqualTo("/services/${testServerId.value}/gameservers/games/banlist"))
                        .willReturn(
                            aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBodyFile("response-POST-banlist.200.json"),
                        ),
                )

                // When
                val result = gateway.addToBanlist(testServerId, "Cheater")

                // Then
                assertThat(result.isRight()).isEqualTo(true)
            }

        @Test
        fun `should remove from banlist successfully`() =
            runTest {
                // Given
                wireMockServer.stubFor(
                    delete(urlPathEqualTo("/services/${testServerId.value}/gameservers/games/banlist"))
                        .willReturn(
                            aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBodyFile("response-DELETE-banlist.200.json"),
                        ),
                )

                // When
                val result = gateway.removeFromBanlist(testServerId, "BannedPlayer1")

                // Then
                assertThat(result.isRight()).isEqualTo(true)
            }
    }
}

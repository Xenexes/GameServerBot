package de.xenexes.gameserverbot.infrastructure.http.nitrado

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import de.xenexes.gameserverbot.domain.gameserver.NitradoServerId
import de.xenexes.gameserverbot.infrastructure.http.HttpClientFactory
import de.xenexes.gameserverbot.infrastructure.http.nitrado.converters.NitradoServerConverter.mapException
import de.xenexes.gameserverbot.infrastructure.http.nitrado.converters.NitradoServerConverter.mapStatus
import de.xenexes.gameserverbot.infrastructure.http.nitrado.dto.NitradoApiResponse
import de.xenexes.gameserverbot.infrastructure.http.nitrado.dto.NitradoBanlistData
import de.xenexes.gameserverbot.infrastructure.http.nitrado.dto.NitradoGameServerData
import de.xenexes.gameserverbot.infrastructure.http.nitrado.dto.NitradoGamesData
import de.xenexes.gameserverbot.infrastructure.http.nitrado.dto.NitradoPlayersData
import de.xenexes.gameserverbot.infrastructure.http.nitrado.dto.NitradoServicesData
import de.xenexes.gameserverbot.infrastructure.http.nitrado.dto.NitradoWhitelistData
import de.xenexes.gameserverbot.infrastructure.http.resilience.ResilientHttpClient
import de.xenexes.gameserverbot.ports.outbound.NitradoGameInfo
import de.xenexes.gameserverbot.ports.outbound.NitradoGateway
import de.xenexes.gameserverbot.ports.outbound.NitradoPlayer
import de.xenexes.gameserverbot.ports.outbound.NitradoPlayerListEntry
import de.xenexes.gameserverbot.ports.outbound.NitradoServerInfo
import de.xenexes.gameserverbot.ports.outbound.NitradoServerQuery
import de.xenexes.gameserverbot.ports.outbound.NitradoServiceDetails
import de.xenexes.gameserverbot.ports.outbound.NitradoServiceInfo
import de.xenexes.gameserverbot.ports.outbound.failure.NitradoFailure
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.isSuccess

class KtorNitradoGateway(
    private val properties: NitradoProperties,
) : NitradoGateway {
    private val logger = KotlinLogging.logger {}

    private val client: ResilientHttpClient =
        HttpClientFactory.createResilientHttpClient(
            clientName = "nitrado",
            baseUrl = properties.baseUrl,
            callTimeout = properties.callTimeout,
            connectTimeout = properties.connectTimeout,
            resilienceProperties = properties.toResilienceProperties(),
        )

    private fun HttpRequestBuilder.withAuth() {
        header(HttpHeaders.Authorization, "Bearer ${properties.apiToken}")
    }

    override suspend fun startServer(nitradoId: NitradoServerId): Either<NitradoFailure, Unit> =
        executeAction(nitradoId, "restart")

    override suspend fun stopServer(
        nitradoId: NitradoServerId,
        message: String?,
    ): Either<NitradoFailure, Unit> =
        executeAction(nitradoId, "stop") {
            message?.let { parameter("stop_message", it) }
        }

    override suspend fun restartServer(
        nitradoId: NitradoServerId,
        message: String?,
    ): Either<NitradoFailure, Unit> =
        executeAction(nitradoId, "restart") {
            message?.let { parameter("restart_message", it) }
        }

    override suspend fun fetchServerInfo(nitradoId: NitradoServerId): Either<NitradoFailure, NitradoServerInfo> =
        either {
            catch({
                val response: NitradoApiResponse<NitradoGameServerData> =
                    client
                        .get("/services/${nitradoId.value}/gameservers") {
                            withAuth()
                        }.body()

                ensure(response.status == "success" && response.data != null) {
                    NitradoFailure.ApiError(
                        response.message ?: "Unknown error",
                        null,
                    )
                }

                val gs = response.data.gameserver
                NitradoServerInfo(
                    nitradoId = nitradoId,
                    status = mapStatus(gs.status),
                    ip = gs.ip,
                    port = gs.port,
                    gameCode = gs.game,
                    gameName = gs.gameHuman,
                    slots = gs.slots,
                    location = gs.location,
                    query =
                        gs.query?.let { q ->
                            NitradoServerQuery(
                                serverName = q.serverName,
                                connectIp = q.connectIp,
                                map = q.map,
                                version = q.version,
                                playersCurrent = q.playerCurrent ?: 0,
                                playersMax = q.playerMax ?: gs.slots,
                            )
                        },
                )
            }) { e ->
                logger.error(e) { "Failed to fetch server info for ${nitradoId.value}" }
                raise(mapException(e))
            }
        }

    override suspend fun fetchServices(): Either<NitradoFailure, List<NitradoServiceInfo>> =
        either {
            catch({
                val response: NitradoApiResponse<NitradoServicesData> =
                    client
                        .get("/services") {
                            withAuth()
                        }.body()

                ensure(response.status == "success" && response.data != null) {
                    NitradoFailure.ApiError(
                        response.message ?: "Unknown error",
                        null,
                    )
                }

                response.data.services.map { svc ->
                    NitradoServiceInfo(
                        serviceId = svc.id,
                        status = svc.status,
                        typeHuman = svc.typeHuman,
                        details =
                            NitradoServiceDetails(
                                address = svc.details.address,
                                name = svc.details.name,
                                game = svc.details.game,
                                slots = svc.details.slots,
                            ),
                    )
                }
            }) { e ->
                logger.error(e) { "Failed to fetch services" }
                raise(mapException(e))
            }
        }

    override suspend fun fetchGameList(nitradoId: NitradoServerId): Either<NitradoFailure, List<NitradoGameInfo>> =
        either {
            catch({
                val response: NitradoApiResponse<NitradoGamesData> =
                    client
                        .get("/services/${nitradoId.value}/gameservers/games") {
                            withAuth()
                        }.body()

                ensure(response.status == "success" && response.data != null) {
                    NitradoFailure.ApiError(
                        response.message ?: "Unknown error",
                        null,
                    )
                }

                response.data.games.map { game ->
                    NitradoGameInfo(
                        gameId = game.id,
                        folderShort = game.folderShort,
                        gameHuman = game.name,
                        installed = game.installed,
                        active = game.active,
                    )
                }
            }) { e ->
                logger.error(e) { "Failed to fetch game list for ${nitradoId.value}" }
                raise(mapException(e))
            }
        }

    override suspend fun fetchPlayers(nitradoId: NitradoServerId): Either<NitradoFailure, List<NitradoPlayer>> =
        either {
            catch({
                val response: NitradoApiResponse<NitradoPlayersData> =
                    client
                        .get("/services/${nitradoId.value}/gameservers/games/players") {
                            withAuth()
                        }.body()

                ensure(response.status == "success" && response.data != null) {
                    NitradoFailure.ApiError(
                        response.message ?: "Unknown error",
                        null,
                    )
                }

                response.data.players.map { player ->
                    NitradoPlayer(
                        name = player.name,
                        online = player.online,
                    )
                }
            }) { e ->
                logger.error(e) { "Failed to fetch players for ${nitradoId.value}" }
                raise(mapException(e))
            }
        }

    override suspend fun switchGame(
        nitradoId: NitradoServerId,
        gameId: String,
    ): Either<NitradoFailure, Unit> =
        either {
            catch({
                val response =
                    client.post("/services/${nitradoId.value}/gameservers/games/start") {
                        withAuth()
                        setBody(FormDataContent(Parameters.build { append("game", gameId) }))
                    }

                ensure(response.status.isSuccess()) {
                    NitradoFailure.ApiError("HTTP ${response.status.value}", response.status.value)
                }
            }) { e ->
                logger.error(e) { "Failed to switch game for ${nitradoId.value}" }
                raise(mapException(e))
            }
        }

    override suspend fun fetchWhitelist(
        nitradoId: NitradoServerId,
    ): Either<NitradoFailure, List<NitradoPlayerListEntry>> =
        either {
            catch({
                val response: NitradoApiResponse<NitradoWhitelistData> =
                    client
                        .get("/services/${nitradoId.value}/gameservers/games/whitelist") {
                            withAuth()
                        }.body()

                ensure(response.status == "success" && response.data != null) {
                    NitradoFailure.ApiError(response.message ?: "Unknown error", null)
                }

                response.data.whitelist.map { entry ->
                    NitradoPlayerListEntry(
                        name = entry.name,
                        id = entry.id,
                        idType = entry.idType,
                    )
                }
            }) { e ->
                logger.error(e) { "Failed to fetch whitelist for ${nitradoId.value}" }
                raise(mapException(e))
            }
        }

    override suspend fun addToWhitelist(
        nitradoId: NitradoServerId,
        identifier: String,
    ): Either<NitradoFailure, Unit> = executePlayerListAction(nitradoId, "whitelist", identifier, isAdd = true)

    override suspend fun removeFromWhitelist(
        nitradoId: NitradoServerId,
        identifier: String,
    ): Either<NitradoFailure, Unit> = executePlayerListAction(nitradoId, "whitelist", identifier, isAdd = false)

    override suspend fun fetchBanlist(
        nitradoId: NitradoServerId,
    ): Either<NitradoFailure, List<NitradoPlayerListEntry>> =
        either {
            catch({
                val response: NitradoApiResponse<NitradoBanlistData> =
                    client
                        .get("/services/${nitradoId.value}/gameservers/games/banlist") {
                            withAuth()
                        }.body()

                ensure(response.status == "success" && response.data != null) {
                    NitradoFailure.ApiError(response.message ?: "Unknown error", null)
                }

                response.data.banlist.map { entry ->
                    NitradoPlayerListEntry(
                        name = entry.name,
                        id = entry.id,
                        idType = entry.idType,
                    )
                }
            }) { e ->
                logger.error(e) { "Failed to fetch banlist for ${nitradoId.value}" }
                raise(mapException(e))
            }
        }

    override suspend fun addToBanlist(
        nitradoId: NitradoServerId,
        identifier: String,
    ): Either<NitradoFailure, Unit> = executePlayerListAction(nitradoId, "banlist", identifier, isAdd = true)

    override suspend fun removeFromBanlist(
        nitradoId: NitradoServerId,
        identifier: String,
    ): Either<NitradoFailure, Unit> = executePlayerListAction(nitradoId, "banlist", identifier, isAdd = false)

    private suspend fun executePlayerListAction(
        nitradoId: NitradoServerId,
        listType: String,
        identifier: String,
        isAdd: Boolean,
    ): Either<NitradoFailure, Unit> =
        either {
            catch({
                val response =
                    if (isAdd) {
                        client.post("/services/${nitradoId.value}/gameservers/games/$listType") {
                            withAuth()
                            parameter("identifier", identifier)
                        }
                    } else {
                        client.delete("/services/${nitradoId.value}/gameservers/games/$listType") {
                            withAuth()
                            parameter("identifier", identifier)
                        }
                    }

                ensure(response.status.isSuccess()) {
                    NitradoFailure.ApiError("HTTP ${response.status.value}", response.status.value)
                }
            }) { e ->
                val action = if (isAdd) "add to" else "remove from"
                logger.error(e) { "Failed to $action $listType for ${nitradoId.value}" }
                raise(mapException(e))
            }
        }

    private suspend fun executeAction(
        nitradoId: NitradoServerId,
        action: String,
        additionalParams: HttpRequestBuilder.() -> Unit = {},
    ): Either<NitradoFailure, Unit> =
        either {
            catch({
                val response =
                    client.post("/services/${nitradoId.value}/gameservers/$action") {
                        withAuth()
                        additionalParams()
                    }

                when (response.status) {
                    HttpStatusCode.Unauthorized ->
                        raise(NitradoFailure.InvalidToken("Invalid or expired API token"))
                    HttpStatusCode.TooManyRequests ->
                        raise(NitradoFailure.RateLimitExceeded())
                    HttpStatusCode.ServiceUnavailable ->
                        raise(NitradoFailure.ServiceUnavailable("Nitrado API is under maintenance"))
                    HttpStatusCode.NotFound ->
                        raise(NitradoFailure.ServerNotFound(nitradoId))
                    else ->
                        ensure(response.status.isSuccess()) {
                            NitradoFailure.ApiError("HTTP ${response.status.value}", response.status.value)
                        }
                }
            }) { e ->
                logger.error(e) { "Failed to execute $action on server ${nitradoId.value}" }
                raise(mapException(e))
            }
        }
}

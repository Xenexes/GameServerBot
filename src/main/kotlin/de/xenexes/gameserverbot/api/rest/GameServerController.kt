package de.xenexes.gameserverbot.api.rest

import de.xenexes.gameserverbot.api.rest.dto.CreateGameServerRequest
import de.xenexes.gameserverbot.api.rest.dto.GameServerDto
import de.xenexes.gameserverbot.api.rest.dto.RestartServerRequest
import de.xenexes.gameserverbot.api.rest.dto.StopServerRequest
import de.xenexes.gameserverbot.api.rest.dto.toDto
import de.xenexes.gameserverbot.domain.gameserver.GameServerId
import de.xenexes.gameserverbot.domain.gameserver.NitradoServerId
import de.xenexes.gameserverbot.usecases.CreateGameServerCommand
import de.xenexes.gameserverbot.usecases.GameServerUseCases
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/game-servers")
class GameServerController(
    private val handler: ControllerHandler,
    private val useCases: GameServerUseCases,
) {
    @GetMapping
    suspend fun getAll(): ResponseEntity<List<GameServerDto>> =
        handler {
            val servers = useCases.findAll().bind()
            ResponseEntity.ok(servers.map { it.toDto() })
        }

    @GetMapping("/{id}")
    suspend fun getById(
        @PathVariable id: String,
    ): ResponseEntity<GameServerDto> =
        handler {
            val server = useCases.findById(GameServerId(id)).bind()
            ResponseEntity.ok(server.toDto())
        }

    @PostMapping
    suspend fun create(
        @RequestBody request: CreateGameServerRequest,
    ): ResponseEntity<GameServerDto> =
        handler {
            val command =
                CreateGameServerCommand(
                    name = request.name,
                    nitradoId = NitradoServerId(request.nitradoId),
                )
            val server = useCases.create(command).bind()
            ResponseEntity.status(HttpStatus.CREATED).body(server.toDto())
        }

    @PostMapping("/{id}/start")
    suspend fun start(
        @PathVariable id: String,
    ): ResponseEntity<GameServerDto> =
        handler {
            val server = useCases.startServer(GameServerId(id)).bind()
            ResponseEntity.ok(server.toDto())
        }

    @PostMapping("/{id}/stop")
    suspend fun stop(
        @PathVariable id: String,
        @RequestBody(required = false) request: StopServerRequest?,
    ): ResponseEntity<GameServerDto> =
        handler {
            val server = useCases.stopServer(GameServerId(id), request?.message).bind()
            ResponseEntity.ok(server.toDto())
        }

    @PostMapping("/{id}/restart")
    suspend fun restart(
        @PathVariable id: String,
        @RequestBody(required = false) request: RestartServerRequest?,
    ): ResponseEntity<GameServerDto> =
        handler {
            val server = useCases.restartServer(GameServerId(id), request?.message).bind()
            ResponseEntity.ok(server.toDto())
        }

    @DeleteMapping("/{id}")
    suspend fun delete(
        @PathVariable id: String,
    ): ResponseEntity<Unit> =
        handler {
            useCases.deleteServer(GameServerId(id)).bind()
            ResponseEntity.noContent().build()
        }
}

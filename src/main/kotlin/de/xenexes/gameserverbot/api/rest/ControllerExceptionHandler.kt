package de.xenexes.gameserverbot.api.rest

import de.xenexes.gameserverbot.api.FailureException
import de.xenexes.gameserverbot.domain.gameserver.GameServerFailure
import de.xenexes.gameserverbot.domain.player.PlayerFailure
import de.xenexes.gameserverbot.ports.outbound.failure.DiscordFailure
import de.xenexes.gameserverbot.ports.outbound.failure.NitradoFailure
import de.xenexes.gameserverbot.ports.outbound.failure.NotificationFailure
import de.xenexes.gameserverbot.ports.outbound.failure.RepositoryFailure
import de.xenexes.gameserverbot.usecases.UseCaseError
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ControllerExceptionHandler {
    @ExceptionHandler(FailureException::class)
    fun handleFailure(ex: FailureException): ProblemDetail = map(ex.failure)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ProblemDetail =
        ProblemDetail.forStatus(HttpStatus.BAD_REQUEST).apply {
            title = "Invalid request"
            detail = ex.message
        }

    private fun map(error: UseCaseError): ProblemDetail =
        when (error) {
            is UseCaseError.Repository<*> -> mapRepositoryError(error.error)
            is UseCaseError.GameServer<*> -> mapGameServerError(error.error)
            is UseCaseError.Nitrado<*> -> mapNitradoError(error.error)
            is UseCaseError.Notification<*> -> mapNotificationError(error.error)
            is UseCaseError.Discord<*> -> mapDiscordError(error.error)
            is UseCaseError.Player<*> -> mapPlayerError(error.error)
        }

    private fun mapRepositoryError(error: RepositoryFailure): ProblemDetail =
        when (error) {
            is RepositoryFailure.NotFound ->
                ProblemDetail.forStatus(HttpStatus.NOT_FOUND).apply {
                    title = "${error.entityType} not found"
                    detail = "Entity with id ${error.id} not found"
                }

            is RepositoryFailure.DuplicateKey ->
                ProblemDetail.forStatus(HttpStatus.CONFLICT).apply {
                    title = "Duplicate ${error.entityType}"
                    detail = "Entity with key ${error.key} already exists"
                }

            is RepositoryFailure.ConnectionError ->
                ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE).apply {
                    title = "Database error"
                }
        }

    private fun mapGameServerError(error: GameServerFailure): ProblemDetail =
        when (error) {
            is GameServerFailure.InvalidState ->
                ProblemDetail.forStatus(HttpStatus.CONFLICT).apply {
                    title = "Invalid state"
                    detail = "Cannot ${error.action} server in state ${error.current}"
                }

            is GameServerFailure.InvalidName ->
                ProblemDetail.forStatus(HttpStatus.BAD_REQUEST).apply {
                    title = "Invalid name"
                    detail = error.reason
                }

            is GameServerFailure.AlreadyExists ->
                ProblemDetail.forStatus(HttpStatus.CONFLICT).apply {
                    title = "Server already exists"
                }
        }

    private fun mapNitradoError(error: NitradoFailure): ProblemDetail =
        when (error) {
            is NitradoFailure.ApiError ->
                ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY).apply {
                    title = "Nitrado API error"
                    detail = error.message
                }

            is NitradoFailure.NetworkError ->
                ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE).apply {
                    title = "Nitrado service unavailable"
                }

            is NitradoFailure.ServerNotFound ->
                ProblemDetail.forStatus(HttpStatus.NOT_FOUND).apply {
                    title = "Nitrado server not found"
                }

            is NitradoFailure.InvalidToken ->
                ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED).apply {
                    title = "Invalid Nitrado API token"
                    detail = error.message
                }

            is NitradoFailure.RateLimitExceeded ->
                ProblemDetail.forStatus(HttpStatus.TOO_MANY_REQUESTS).apply {
                    title = "Nitrado API rate limit exceeded"
                    detail = error.retryAfter?.let { "Retry after $it seconds" }
                }

            is NitradoFailure.ServiceUnavailable ->
                ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE).apply {
                    title = "Nitrado service unavailable"
                    detail = error.message
                }
        }

    private fun mapNotificationError(error: NotificationFailure): ProblemDetail =
        when (error) {
            is NotificationFailure.SendFailed ->
                ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY).apply {
                    title = "Notification failed"
                    detail = error.reason
                }
        }

    private fun mapDiscordError(error: DiscordFailure): ProblemDetail =
        when (error) {
            is DiscordFailure.NotInitialized ->
                ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE).apply {
                    title = "Discord not initialized"
                    detail = error.reason
                }

            is DiscordFailure.ConnectionFailed ->
                ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY).apply {
                    title = "Discord connection failed"
                    detail = error.reason
                }

            is DiscordFailure.CommandFailed ->
                ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR).apply {
                    title = "Discord command failed"
                    detail = error.reason
                }

            is DiscordFailure.PermissionDenied ->
                ProblemDetail.forStatus(HttpStatus.FORBIDDEN).apply {
                    title = "Discord permission denied"
                    detail = error.reason
                }

            is DiscordFailure.ChannelNotFound ->
                ProblemDetail.forStatus(HttpStatus.NOT_FOUND).apply {
                    title = "Discord channel not found"
                    detail = "Channel ${error.channelId} not found"
                }

            is DiscordFailure.GuildNotFound ->
                ProblemDetail.forStatus(HttpStatus.NOT_FOUND).apply {
                    title = "Discord guild not found"
                    detail = "Guild ${error.guildId} not found"
                }
        }

    private fun mapPlayerError(error: PlayerFailure): ProblemDetail =
        when (error) {
            is PlayerFailure.InvalidIdentifier ->
                ProblemDetail.forStatus(HttpStatus.BAD_REQUEST).apply {
                    title = "Invalid player identifier"
                    detail = error.reason
                }

            is PlayerFailure.AlreadyInList ->
                ProblemDetail.forStatus(HttpStatus.CONFLICT).apply {
                    title = "Player already in ${error.listType.name.lowercase()}"
                    detail = "Player ${error.identifier.value} is already in the list"
                }

            is PlayerFailure.NotInList ->
                ProblemDetail.forStatus(HttpStatus.NOT_FOUND).apply {
                    title = "Player not in ${error.listType.name.lowercase()}"
                    detail = "Player ${error.identifier.value} is not in the list"
                }

            is PlayerFailure.ServerNotFound ->
                ProblemDetail.forStatus(HttpStatus.NOT_FOUND).apply {
                    title = "Server not found"
                    detail = "Server ${error.serverId.value} not found"
                }

            is PlayerFailure.OperationFailed ->
                ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY).apply {
                    title = "Player operation failed"
                    detail = error.reason
                }
        }
}

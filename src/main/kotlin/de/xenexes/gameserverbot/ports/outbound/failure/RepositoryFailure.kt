package de.xenexes.gameserverbot.ports.outbound.failure

sealed interface RepositoryFailure {
    data class NotFound(
        val entityType: String,
        val id: String,
    ) : RepositoryFailure

    data class DuplicateKey(
        val entityType: String,
        val key: String,
    ) : RepositoryFailure

    data class ConnectionError(
        val cause: Throwable,
    ) : RepositoryFailure
}

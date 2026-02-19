package de.xenexes.gameserverbot.ports.outbound

interface UserRepository {
    fun findAll(): List<UserCredentials>
}

data class UserCredentials(
    val username: String,
    val password: String,
    val roles: List<String>,
)

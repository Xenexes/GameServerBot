package de.xenexes.gameserverbot.infrastructure.security

import de.xenexes.gameserverbot.config.SecurityProperties
import de.xenexes.gameserverbot.ports.outbound.UserCredentials
import de.xenexes.gameserverbot.ports.outbound.UserRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Repository
import java.security.SecureRandom

@Repository
class ConfigUserRepository(
    securityProperties: SecurityProperties,
) : UserRepository {
    private val logger = KotlinLogging.logger {}

    private val resolvedUsers: List<UserCredentials> =
        securityProperties.basicAuth.values.map { entity ->
            val password = entity.password?.takeIf { it.isNotBlank() } ?: generateAndLog(entity.username)
            UserCredentials(entity.username, password, entity.roles)
        }

    override fun findAll(): List<UserCredentials> = resolvedUsers

    private fun generateAndLog(username: String): String {
        val password = generatePassword()
        logger.info { "========================================" }
        logger.info { "Generated password for '$username': $password" }
        logger.info { "========================================" }
        return password
    }

    private fun generatePassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!&*?+-"
        val random = SecureRandom()
        return (1..PASSWORD_LENGTH)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }

    companion object {
        private const val PASSWORD_LENGTH = 24
    }
}

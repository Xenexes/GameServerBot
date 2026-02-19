package de.xenexes.gameserverbot.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "gameserverbot.security")
data class SecurityProperties(
    val basicAuth: Map<String, BasicAuthEntity> = emptyMap(),
) {
    data class BasicAuthEntity(
        val username: String,
        val password: String?,
        val roles: List<String> = listOf("USER"),
    )
}

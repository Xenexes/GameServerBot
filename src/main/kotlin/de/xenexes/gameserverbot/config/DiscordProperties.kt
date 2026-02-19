package de.xenexes.gameserverbot.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "gameserverbot.discord")
data class DiscordProperties(
    val enabled: Boolean = false,
    val botToken: String? = null,
    val guildId: String? = null,
    val notificationChannelId: String? = null,
    val adminRoleId: String? = null,
    val allowedRoleId: String? = null,
    val webhookUrl: String? = null,
    val commandTimeout: Long = 30000,
    val presenceUpdateInterval: Long = 15000,
)

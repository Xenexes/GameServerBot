package de.xenexes.gameserverbot.domain.discord

import io.github.oshai.kotlinlogging.KotlinLogging

class DiscordPermissionService {
    private val logger = KotlinLogging.logger {}

    fun hasPermission(
        user: DiscordUser,
        commandName: String,
    ): Boolean {
        if (user.hasAdminPermission()) {
            logger.debug { "User ${user.username} has admin permission" }
            return true
        }

        val requiredPermissions = getRequiredPermissions(commandName)
        val hasPermission = requiredPermissions.any { user.hasPermission(it) }

        logger.debug {
            "User ${user.username} permission check for $commandName: $hasPermission"
        }

        return hasPermission
    }

    private fun getRequiredPermissions(commandName: String): Set<Permission> =
        when (commandName) {
            "server-control" -> setOf(Permission.SERVER_MANAGEMENT)
            "server-status" -> setOf(Permission.SERVER_MANAGEMENT)
            "server-game" -> setOf(Permission.SERVER_MANAGEMENT)
            "server-players" -> setOf(Permission.PLAYER_MANAGEMENT)
            "server-player-lists" -> setOf(Permission.PLAYER_MANAGEMENT)
            "server-whitelist" -> setOf(Permission.PLAYER_MANAGEMENT)
            "server-banlist" -> setOf(Permission.PLAYER_MANAGEMENT)
            else -> setOf(Permission.SERVER_MANAGEMENT)
        }
}

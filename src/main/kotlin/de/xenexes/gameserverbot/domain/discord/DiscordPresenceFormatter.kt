package de.xenexes.gameserverbot.domain.discord

import de.xenexes.gameserverbot.domain.gameserver.GameServerStatus

object DiscordPresenceFormatter {
    fun format(
        status: GameServerStatus,
        gameName: String? = null,
        playerCount: Int? = null,
        maxPlayers: Int? = null,
    ): String {
        val statusEmoji = getStatusEmoji(status)
        val statusText = getStatusText(status)

        return buildString {
            append(statusEmoji)
            append(" ")
            append(statusText)

            gameName?.let { game ->
                append(" | ")
                append(game)
            }

            if (playerCount != null && maxPlayers != null) {
                append(" | ")
                append(playerCount)
                append("/")
                append(maxPlayers)
                append(" players")
            }
        }
    }

    fun formatShort(
        name: String,
        status: GameServerStatus,
    ): String {
        val statusEmoji = getStatusEmoji(status)
        return "$statusEmoji $name"
    }

    private fun getStatusEmoji(status: GameServerStatus): String =
        when (status) {
            GameServerStatus.STARTED -> "\uD83D\uDFE2"
            GameServerStatus.STOPPED -> "\uD83D\uDD34"
            GameServerStatus.RESTARTING -> "\uD83D\uDFE1"
            GameServerStatus.STOPPING -> "\uD83D\uDFE0"
            GameServerStatus.SUSPENDED -> "\u26AB"
            GameServerStatus.GUARDIAN_LOCKED -> "\uD83D\uDD12"
            GameServerStatus.BACKUP_RESTORE -> "\uD83D\uDCE6"
            GameServerStatus.BACKUP_CREATION -> "\uD83D\uDCE6"
            GameServerStatus.CHUNKFIX -> "\uD83D\uDD27"
            GameServerStatus.OVERVIEWMAP_RENDER -> "\uD83D\uDDFA\uFE0F"
            GameServerStatus.GS_INSTALLATION -> "\u2699\uFE0F"
            GameServerStatus.UNKNOWN -> "\u2753"
        }

    private fun getStatusText(status: GameServerStatus): String =
        when (status) {
            GameServerStatus.STARTED -> "Online"
            GameServerStatus.STOPPED -> "Offline"
            GameServerStatus.RESTARTING -> "Restarting"
            GameServerStatus.STOPPING -> "Stopping"
            GameServerStatus.SUSPENDED -> "Suspended"
            GameServerStatus.GUARDIAN_LOCKED -> "Locked"
            GameServerStatus.BACKUP_RESTORE -> "Restoring"
            GameServerStatus.BACKUP_CREATION -> "Backup"
            GameServerStatus.CHUNKFIX -> "Maintenance"
            GameServerStatus.OVERVIEWMAP_RENDER -> "Rendering"
            GameServerStatus.GS_INSTALLATION -> "Installing"
            GameServerStatus.UNKNOWN -> "Unknown"
        }
}

package de.xenexes.gameserverbot.domain.discord

data class DiscordUser(
    val id: DiscordUserId,
    val username: String,
    private val roles: Set<DiscordRole> = emptySet(),
) {
    fun getRoles(): Set<DiscordRole> = roles

    fun hasRole(roleId: DiscordRoleId): Boolean = roles.any { it.id == roleId }

    fun hasPermission(permission: Permission): Boolean = roles.any { it.hasPermission(permission) }

    fun hasAdminPermission(): Boolean = roles.any { it.hasAdminPermission() }
}

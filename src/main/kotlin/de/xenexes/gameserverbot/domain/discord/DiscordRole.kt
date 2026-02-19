package de.xenexes.gameserverbot.domain.discord

data class DiscordRole(
    val id: DiscordRoleId,
    val name: String,
    val permissions: Set<Permission> = emptySet(),
) {
    fun hasPermission(permission: Permission): Boolean = permissions.contains(permission)

    fun hasAdminPermission(): Boolean = hasPermission(Permission.ADMIN)
}

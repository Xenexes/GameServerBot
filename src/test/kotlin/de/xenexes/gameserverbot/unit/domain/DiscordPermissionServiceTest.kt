package de.xenexes.gameserverbot.unit.domain

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import de.xenexes.gameserverbot.domain.discord.DiscordPermissionService
import de.xenexes.gameserverbot.domain.discord.DiscordRole
import de.xenexes.gameserverbot.domain.discord.DiscordRoleId
import de.xenexes.gameserverbot.domain.discord.DiscordUser
import de.xenexes.gameserverbot.domain.discord.DiscordUserId
import de.xenexes.gameserverbot.domain.discord.Permission
import org.junit.jupiter.api.Test

class DiscordPermissionServiceTest {
    private val service = DiscordPermissionService()

    @Test
    fun `should allow command when user has required permission`() {
        // Given
        val user =
            DiscordUser(
                id = DiscordUserId("123"),
                username = "mod",
                roles = setOf(DiscordRole(DiscordRoleId("mod-role"), "Moderator", setOf(Permission.SERVER_MANAGEMENT))),
            )

        // When & Then
        assertThat(service.hasPermission(user, "server-status")).isTrue()
    }

    @Test
    fun `should deny command when user lacks required permission`() {
        // Given
        val user =
            DiscordUser(
                id = DiscordUserId("123"),
                username = "player",
                roles = setOf(DiscordRole(DiscordRoleId("player-role"), "Player", setOf(Permission.PLAYER_MANAGEMENT))),
            )

        // When & Then
        assertThat(service.hasPermission(user, "server-control")).isFalse()
    }

    @Test
    fun `should allow admin to use all commands`() {
        // Given
        val user =
            DiscordUser(
                id = DiscordUserId("123"),
                username = "admin",
                roles = setOf(DiscordRole(DiscordRoleId("admin-role"), "Admin", setOf(Permission.ADMIN))),
            )

        // When & Then - admin bypasses permission check for all commands
        assertThat(service.hasPermission(user, "server-control")).isTrue()
        assertThat(service.hasPermission(user, "server-whitelist")).isTrue()
        assertThat(service.hasPermission(user, "server-banlist")).isTrue()
    }

    @Test
    fun `should deny unknown command when user has no permissions`() {
        // Given - unknown commands default to requiring SERVER_MANAGEMENT
        val user =
            DiscordUser(
                id = DiscordUserId("123"),
                username = "user",
                roles = emptySet(),
            )

        // When & Then
        assertThat(service.hasPermission(user, "unknown-command")).isFalse()
    }

    @Test
    fun `should allow player management commands for users with player management permission`() {
        // Given
        val user =
            DiscordUser(
                id = DiscordUserId("456"),
                username = "moderator",
                roles = setOf(DiscordRole(DiscordRoleId("mod-role"), "Moderator", setOf(Permission.PLAYER_MANAGEMENT))),
            )

        // When & Then
        assertThat(service.hasPermission(user, "server-whitelist")).isTrue()
        assertThat(service.hasPermission(user, "server-banlist")).isTrue()
        assertThat(service.hasPermission(user, "server-player-lists")).isTrue()
    }
}

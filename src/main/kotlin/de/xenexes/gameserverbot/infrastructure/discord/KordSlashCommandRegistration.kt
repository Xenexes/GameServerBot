package de.xenexes.gameserverbot.infrastructure.discord

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.rest.builder.interaction.BaseInputChatBuilder
import dev.kord.rest.builder.interaction.integer
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.subCommand
import io.github.oshai.kotlinlogging.KotlinLogging

class KordSlashCommandRegistration {
    private val logger = KotlinLogging.logger {}

    suspend fun registerAllCommands(
        kord: Kord,
        guildId: Snowflake,
    ) {
        logger.info { "Registering slash commands for guild $guildId" }

        registerServerControlCommands(kord, guildId)
        registerServerStatusCommand(kord, guildId)
        registerServerGameCommands(kord, guildId)
        registerServerPlayersCommand(kord, guildId)
        registerPlayerListTypesCommand(kord, guildId)
        registerWhitelistCommands(kord, guildId)
        registerBanlistCommands(kord, guildId)

        logger.info { "All slash commands registered" }
    }

    private suspend fun registerServerControlCommands(
        kord: Kord,
        guildId: Snowflake,
    ) {
        kord.createGuildChatInputCommand(guildId, "server-control", "Control the game server") {
            subCommand("start", "Start the game server") {
                addServerOption()
            }
            subCommand("stop", "Stop the game server") {
                addServerOption()
            }
            subCommand("restart", "Restart the game server") {
                addServerOption()
            }
        }
        logger.debug { "Registered server-control commands" }
    }

    private suspend fun registerServerStatusCommand(
        kord: Kord,
        guildId: Snowflake,
    ) {
        kord.createGuildChatInputCommand(guildId, "server-status", "Get current server status") {
            addServerOption()
        }
        logger.debug { "Registered server-status command" }
    }

    private suspend fun registerServerGameCommands(
        kord: Kord,
        guildId: Snowflake,
    ) {
        kord.createGuildChatInputCommand(guildId, "server-game", "Manage server games") {
            subCommand("list", "List all available games (paginated)") {
                integer("page", "Page number (default: 1)") {
                    required = false
                    minValue = 1
                }
                string("filter", "Filter games by name (case-insensitive, partial match)") {
                    required = false
                }
                addServerOption()
            }
            subCommand("installed", "List only installed games") {
                addServerOption()
            }
            subCommand("switch", "Switch to a different game") {
                string("game-id", "Game ID to switch to") {
                    required = true
                }
                addServerOption()
            }
        }
        logger.debug { "Registered server-game commands" }
    }

    private suspend fun registerServerPlayersCommand(
        kord: Kord,
        guildId: Snowflake,
    ) {
        kord.createGuildChatInputCommand(guildId, "server-players", "List online players") {
            addServerOption()
        }
        logger.debug { "Registered server-players command" }
    }

    private suspend fun registerPlayerListTypesCommand(
        kord: Kord,
        guildId: Snowflake,
    ) {
        kord.createGuildChatInputCommand(
            guildId,
            "server-player-lists",
            "Show available player list types and ID format",
        ) {
            addServerOption()
        }
        logger.debug { "Registered server-player-lists command" }
    }

    private suspend fun registerWhitelistCommands(
        kord: Kord,
        guildId: Snowflake,
    ) {
        kord.createGuildChatInputCommand(guildId, "server-whitelist", "Manage server whitelist") {
            subCommand("list", "Show whitelisted players") {
                addServerOption()
            }
            subCommand("add", "Add a player to whitelist") {
                string("player", "Player name or ID") {
                    required = true
                }
                addServerOption()
            }
            subCommand("remove", "Remove a player from whitelist") {
                string("player", "Player name or ID") {
                    required = true
                }
                addServerOption()
            }
        }
        logger.debug { "Registered server-whitelist commands" }
    }

    private suspend fun registerBanlistCommands(
        kord: Kord,
        guildId: Snowflake,
    ) {
        kord.createGuildChatInputCommand(guildId, "server-banlist", "Manage server banlist") {
            subCommand("list", "Show banned players") {
                addServerOption()
            }
            subCommand("add", "Ban a player") {
                string("player", "Player name or ID") {
                    required = true
                }
                addServerOption()
            }
            subCommand("remove", "Unban a player") {
                string("player", "Player name or ID") {
                    required = true
                }
                addServerOption()
            }
        }
        logger.debug { "Registered server-banlist commands" }
    }

    private fun BaseInputChatBuilder.addServerOption() {
        string("server", "Server name (optional, defaults to first server)") {
            required = false
            autocomplete = true
        }
    }
}

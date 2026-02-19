@file:Suppress("ktlint:standard:max-line-length")

package de.xenexes.gameserverbot.api.discord

import de.xenexes.gameserverbot.domain.discord.DiscordInteraction
import de.xenexes.gameserverbot.domain.discord.DiscordPermissionService
import de.xenexes.gameserverbot.domain.gameserver.GameServer
import de.xenexes.gameserverbot.domain.gameserver.GameServerFailure
import de.xenexes.gameserverbot.domain.gameserver.GameServerStatus
import de.xenexes.gameserverbot.domain.player.PlayerIdentifier
import de.xenexes.gameserverbot.domain.shared.UserContext
import de.xenexes.gameserverbot.usecases.GameServerUseCases
import de.xenexes.gameserverbot.usecases.PlayerUseCases
import de.xenexes.gameserverbot.usecases.UseCaseError
import de.xenexes.gameserverbot.usecases.UseCaseRaise
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    name = ["gameserverbot.discord.enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
class DiscordCommandRegistry(
    private val gameServerUseCases: GameServerUseCases,
    private val playerUseCases: PlayerUseCases,
    private val discordHandler: DiscordHandler,
    private val permissionService: DiscordPermissionService,
) {
    suspend fun handleCommand(interaction: DiscordInteraction) {
        val discordUser = interaction.getUser()

        if (!permissionService.hasPermission(discordUser, interaction.commandName)) {
            interaction.respondImmediately("❌ You don't have permission to use this command.")
            return
        }

        interaction.defer()

        val response =
            discordHandler(discordUser.id) {
                when (interaction.commandName) {
                    "server-control" -> handleServerControl(interaction)
                    "server-status" -> handleServerStatus(interaction)
                    "server-game" -> handleServerGame(interaction)
                    "server-players" -> handleServerPlayers(interaction)
                    "server-player-lists" -> handleServerPlayerLists(interaction)
                    "server-whitelist" -> handleServerWhitelist(interaction)
                    "server-banlist" -> handleServerBanlist(interaction)
                    else -> "❌ Unknown command"
                }
            }.fold(
                { error -> formatError(error) },
                { it },
            )

        interaction.editResponse(response)
    }

    context(_: UserContext) private suspend fun UseCaseRaise.resolveServer(interaction: DiscordInteraction): GameServer {
        val serverParam = interaction.getStringParameter("server")
        val servers = gameServerUseCases.findAll().bind()

        ensure(servers.isNotEmpty()) {
            UseCaseError.GameServer(GameServerFailure.InvalidName("No servers configured"))
        }

        if (serverParam.isNullOrBlank()) return servers.first()

        return servers.find { it.id.value == serverParam }
            ?: servers.find { it.name.equals(serverParam, ignoreCase = true) }
            ?: run {
                val available = servers.joinToString(", ") { "**${it.name}**" }
                raise(UseCaseError.GameServer(GameServerFailure.InvalidName("Server not found. Available: $available")))
            }
    }

    context(_: UserContext) private suspend fun UseCaseRaise.handleServerControl(interaction: DiscordInteraction): String {
        val subcommand =
            interaction.getSubcommandName() ?: return "❌ Please specify a subcommand: start, stop, or restart"

        val server = resolveServer(interaction)

        val updated =
            when (subcommand) {
                "start" -> gameServerUseCases.startServer(server.id).bind()
                "stop" -> gameServerUseCases.stopServer(server.id).bind()
                "restart" -> gameServerUseCases.restartServer(server.id).bind()
                else -> return "❌ Unknown subcommand: $subcommand"
            }

        return "✅ Server **${updated.name}** ${subcommand}ed successfully. Status: ${updated.status}"
    }

    context(_: UserContext) private suspend fun UseCaseRaise.handleServerStatus(interaction: DiscordInteraction): String {
        val serverParam = interaction.getStringParameter("server")

        if (serverParam.isNullOrBlank()) {
            val servers = gameServerUseCases.findAll().bind()
            return if (servers.isEmpty()) {
                "❌ No servers configured"
            } else {
                servers.joinToString("\n") { formatServerStatus(it) }
            }
        }

        val server = resolveServer(interaction)
        return formatServerStatus(server)
    }

    private fun statusEmoji(status: GameServerStatus): String =
        when (status) {
            GameServerStatus.STARTED -> "🟢"
            GameServerStatus.STOPPED -> "🔴"
            GameServerStatus.RESTARTING, GameServerStatus.STOPPING -> "🔄"
            GameServerStatus.SUSPENDED -> "⛔"
            GameServerStatus.GUARDIAN_LOCKED -> "🔒"
            GameServerStatus.UNKNOWN -> "❓"
            else -> "🔧"
        }

    private fun formatServerStatus(server: GameServer): String =
        buildString {
            appendLine("${statusEmoji(server.status)} **${server.name}**")
            appendLine("📊 Status: ${formatStatusLabel(server.status)}")
            server.gameName?.let { appendLine("🎮 Game: $it") }
            server.ip?.let { ip ->
                server.port?.let { port ->
                    appendLine("🌐 Address: `$ip:$port`")
                }
            }
            server.playerSlots?.let { appendLine("👥 Slots: $it players") }
            server.location?.let { append("📍 Location: $it") }
        }

    private fun formatStatusLabel(status: GameServerStatus): String =
        status.name
            .replace("_", " ")
            .lowercase()
            .replaceFirstChar { it.uppercase() }

    context(_: UserContext) private suspend fun UseCaseRaise.handleServerGame(interaction: DiscordInteraction): String {
        val subcommand =
            interaction.getSubcommandName()
                ?: return "❌ Please specify a subcommand: list, installed, or switch"

        val server = resolveServer(interaction)

        return when (subcommand) {
            "list" -> {
                val allGames = gameServerUseCases.fetchGameList(server.id).bind()
                val filter = interaction.getStringParameter("filter")
                val games =
                    if (filter.isNullOrBlank()) {
                        allGames
                    } else {
                        allGames.filter { it.gameHuman.contains(filter, ignoreCase = true) }
                    }

                if (games.isEmpty()) {
                    return if (filter.isNullOrBlank()) {
                        "🎮 No games available for **${server.name}**"
                    } else {
                        "🎮 No games matching **\"$filter\"** found on **${server.name}**"
                    }
                }

                val page = (interaction.getIntParameter("page") ?: 1).coerceAtLeast(1)
                val totalPages = (games.size + PAGE_SIZE - 1) / PAGE_SIZE

                if (page > totalPages) {
                    return "❌ Page $page does not exist. There are only $totalPages page(s) of games."
                }

                val pageGames = games.drop((page - 1) * PAGE_SIZE).take(PAGE_SIZE)
                val filterSuffix = if (filter.isNullOrBlank()) "" else " filter:$filter"
                buildString {
                    if (filter.isNullOrBlank()) {
                        appendLine("🎮 **Available Games for ${server.name}** (Page $page/$totalPages):")
                    } else {
                        appendLine("🎮 **Games matching \"$filter\" on ${server.name}** (Page $page/$totalPages):")
                    }
                    pageGames.forEach { game ->
                        val emoji = if (game.installed) "🟢" else "🔵"
                        appendLine("$emoji **${game.gameHuman}** (`${game.folderShort}` | id: `${game.gameId}`)")
                    }
                    if (page < totalPages) {
                        appendLine(
                            "📄 Page $page/$totalPages — Next: `/server-game list page:${page + 1}$filterSuffix`",
                        )
                    } else {
                        appendLine("📄 Page $page/$totalPages")
                    }
                    append("💡 Use `/server-game switch game-id:<folder-short>` to switch games")
                }
            }

            "installed" -> {
                val games = gameServerUseCases.fetchGameList(server.id).bind()
                val installed = games.filter { it.installed }

                if (installed.isEmpty()) return "🎮 No installed games found on **${server.name}**"

                buildString {
                    appendLine("🎮 **Installed Games for ${server.name}** (${installed.size} installed):")
                    installed.forEach { game ->
                        if (game.active) {
                            appendLine(
                                "🟢 **${game.gameHuman}** (`${game.folderShort}` | id: `${game.gameId}`) — Active",
                            )
                        } else {
                            appendLine("🟡 **${game.gameHuman}** (`${game.folderShort}` | id: `${game.gameId}`)")
                        }
                    }
                    append("💡 Use `/server-game switch game-id:<folder-short>` to change game.")
                }
            }

            "switch" -> {
                val gameId =
                    interaction.getStringParameter("game-id")
                        ?: return "❌ Please specify a game ID"

                gameServerUseCases.switchGame(server.id, gameId).bind()
                "🔄 Game switch to **$gameId** initiated on **${server.name}**. Server will restart."
            }

            else -> {
                "❌ Unknown subcommand: $subcommand"
            }
        }
    }

    companion object {
        private const val PAGE_SIZE = 15
    }

    context(_: UserContext) private suspend fun UseCaseRaise.handleServerPlayers(interaction: DiscordInteraction): String {
        val server = resolveServer(interaction)
        val players = gameServerUseCases.fetchPlayers(server.id).bind()

        val onlinePlayers = players.filter { it.online }
        return if (onlinePlayers.isEmpty()) {
            "👥 No players currently online on **${server.name}**"
        } else {
            val playerList = onlinePlayers.joinToString(", ") { it.name }
            "👥 **Players online on ${server.name}** (${onlinePlayers.size}):\n$playerList"
        }
    }

    context(_: UserContext) private suspend fun UseCaseRaise.handleServerPlayerLists(interaction: DiscordInteraction): String {
        val server = resolveServer(interaction)

        // Fetch both lists independently — don't short-circuit on individual failures
        val whitelistResult = playerUseCases.getWhitelist(server.id)
        val banlistResult = playerUseCases.getBanlist(server.id)

        return buildString {
            append("**Player Lists for ${server.name}**\n\n")

            whitelistResult.fold(
                { append("❌ Whitelist: Error fetching whitelist\n") },
                { playerList ->
                    if (playerList.entries.isEmpty()) {
                        append("📝 **Whitelist:** Empty\n")
                    } else {
                        val idType = playerList.entries.firstOrNull()?.identifierType ?: "unknown"
                        append("📝 **Whitelist** (ID type: $idType): ${playerList.entries.size} entries\n")
                    }
                },
            )

            banlistResult.fold(
                { append("❌ Banlist: Error fetching banlist\n") },
                { playerList ->
                    if (playerList.entries.isEmpty()) {
                        append("🔨 **Banlist:** Empty\n")
                    } else {
                        val idType = playerList.entries.firstOrNull()?.identifierType ?: "unknown"
                        append("🔨 **Banlist** (ID type: $idType): ${playerList.entries.size} entries\n")
                    }
                },
            )

            append("\nUse `/server-whitelist` or `/server-banlist` to manage players.")
        }
    }

    context(_: UserContext) private suspend fun UseCaseRaise.handleServerWhitelist(interaction: DiscordInteraction): String {
        val subcommand =
            interaction.getSubcommandName() ?: return "❌ Please specify a subcommand: list, add, or remove"

        val server = resolveServer(interaction)

        return when (subcommand) {
            "list" -> {
                val playerList = playerUseCases.getWhitelist(server.id).bind()
                if (playerList.entries.isEmpty()) {
                    "📝 **Whitelist for ${server.name}:** Empty"
                } else {
                    val entries =
                        playerList.entries.joinToString("\n") { entry ->
                            "- **${entry.name}** (`${entry.identifier.value}`)"
                        }
                    "📝 **Whitelist for ${server.name}** (${playerList.entries.size} entries):\n$entries"
                }
            }

            "add" -> {
                val player =
                    interaction.getStringParameter("player")
                        ?: return "❌ Please specify a player name or ID"
                val identifier = PlayerIdentifier.create(player).bind()
                playerUseCases.addToWhitelist(server.id, identifier).bind()
                "✅ Added **$player** to whitelist on **${server.name}**"
            }

            "remove" -> {
                val player =
                    interaction.getStringParameter("player")
                        ?: return "❌ Please specify a player name or ID"
                val identifier = PlayerIdentifier.create(player).bind()
                playerUseCases.removeFromWhitelist(server.id, identifier).bind()
                "➖ Removed **$player** from whitelist on **${server.name}**"
            }

            else -> {
                "❌ Unknown subcommand: $subcommand"
            }
        }
    }

    context(_: UserContext) private suspend fun UseCaseRaise.handleServerBanlist(interaction: DiscordInteraction): String {
        val subcommand =
            interaction.getSubcommandName() ?: return "❌ Please specify a subcommand: list, add, or remove"

        val server = resolveServer(interaction)

        return when (subcommand) {
            "list" -> {
                val playerList = playerUseCases.getBanlist(server.id).bind()
                if (playerList.entries.isEmpty()) {
                    "🔨 **Banlist for ${server.name}:** Empty"
                } else {
                    val entries =
                        playerList.entries.joinToString("\n") { entry ->
                            "- **${entry.name}** (`${entry.identifier.value}`)"
                        }
                    "🔨 **Banlist for ${server.name}** (${playerList.entries.size} entries):\n$entries"
                }
            }

            "add" -> {
                val player =
                    interaction.getStringParameter("player")
                        ?: return "❌ Please specify a player name or ID"
                val identifier = PlayerIdentifier.create(player).bind()
                playerUseCases.addToBanlist(server.id, identifier).bind()
                "🔨 Banned **$player** on **${server.name}**"
            }

            "remove" -> {
                val player =
                    interaction.getStringParameter("player")
                        ?: return "❌ Please specify a player name or ID"
                val identifier = PlayerIdentifier.create(player).bind()
                playerUseCases.removeFromBanlist(server.id, identifier).bind()
                "🔓 Unbanned **$player** on **${server.name}**"
            }

            else -> {
                "❌ Unknown subcommand: $subcommand"
            }
        }
    }

    private fun formatError(error: UseCaseError): String =
        when (error) {
            is UseCaseError.GameServer<*> -> {
                when (val e = error.error) {
                    is GameServerFailure.InvalidName -> "❌ ${e.reason}"
                    is GameServerFailure.InvalidState -> "❌ Cannot ${e.action} server in state ${e.current}"
                    is GameServerFailure.AlreadyExists -> "❌ Server with Nitrado ID ${e.nitradoId.value} already exists"
                }
            }

            is UseCaseError.Repository<*> -> {
                "❌ Database error: ${error.error}"
            }

            is UseCaseError.Nitrado<*> -> {
                "❌ API error: ${error.error}"
            }

            is UseCaseError.Player<*> -> {
                "❌ Player error: ${error.error}"
            }

            is UseCaseError.Notification<*> -> {
                "❌ Notification error: ${error.error}"
            }

            is UseCaseError.Discord<*> -> {
                "❌ Discord error: ${error.error}"
            }
        }
}

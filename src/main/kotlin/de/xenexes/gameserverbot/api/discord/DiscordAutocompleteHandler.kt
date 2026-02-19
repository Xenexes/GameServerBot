package de.xenexes.gameserverbot.api.discord

import de.xenexes.gameserverbot.domain.discord.AutocompleteChoice
import de.xenexes.gameserverbot.domain.discord.DiscordAutocompleteContext
import de.xenexes.gameserverbot.domain.discord.DiscordUserId
import de.xenexes.gameserverbot.usecases.GameServerUseCases
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    name = ["gameserverbot.discord.enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
class DiscordAutocompleteHandler(
    private val gameServerUseCases: GameServerUseCases,
    private val discordHandler: DiscordHandler,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun handleAutocomplete(context: DiscordAutocompleteContext) {
        if (context.focusedOptionName != "server") {
            return
        }

        val currentInput = context.currentInput

        discordHandler(DiscordUserId(context.userId)) {
            val servers = gameServerUseCases.findAll().bind()

            val matchingServers =
                servers
                    .filter { server ->
                        currentInput.isBlank() ||
                            server.name.contains(currentInput, ignoreCase = true) ||
                            server.id.value.contains(currentInput, ignoreCase = true)
                    }.take(MAX_AUTOCOMPLETE_CHOICES)

            context.suggestChoices(
                matchingServers.map { server ->
                    AutocompleteChoice(server.name, server.id.value)
                },
            )
        }.onLeft { error ->
            logger.warn { "Failed to fetch servers for autocomplete: $error" }
            context.suggestChoices(emptyList())
        }
    }

    companion object {
        private const val MAX_AUTOCOMPLETE_CHOICES = 25
    }
}

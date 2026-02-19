package de.xenexes.gameserverbot.api.discord

import de.xenexes.gameserverbot.usecases.DiscordBotUseCases
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    name = ["gameserverbot.discord.enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
class DiscordBotService(
    private val discordBotUseCases: DiscordBotUseCases,
    private val commandRegistry: DiscordCommandRegistry,
    private val autocompleteHandler: DiscordAutocompleteHandler,
) {
    private val logger = KotlinLogging.logger {}
    private var botJob: Job? = null

    @EventListener(ApplicationReadyEvent::class)
    fun startBot() {
        logger.info { "Starting Discord bot..." }

        botJob =
            CoroutineScope(Dispatchers.Default).launch {
                initializeBot()
            }
    }

    private suspend fun initializeBot() {
        discordBotUseCases.startBot().onLeft {
            logger.error { "Failed to start Discord client: $it" }
            return
        }

        discordBotUseCases.registerConfiguredCommands().onLeft {
            logger.error { "Failed to register slash commands: $it" }
        }

        discordBotUseCases.registerCommandHandler { commandRegistry.handleCommand(it) }
        discordBotUseCases.registerAutocompleteHandler { autocompleteHandler.handleAutocomplete(it) }

        // Establish Gateway WebSocket connection (runs in background)
        discordBotUseCases.loginBot().onLeft {
            logger.error { "Failed to login to Discord Gateway: $it" }
            return
        }

        // Wait for Gateway connection to establish before checking readiness
        delay(CONNECTION_VERIFICATION_DELAY_MS)

        if (discordBotUseCases.isReady()) {
            discordBotUseCases.publishBotReady()
            logger.info { "Discord bot started successfully" }
        } else {
            logger.warn { "Discord bot connection not ready" }
        }
    }

    @PreDestroy
    fun stopBot() {
        logger.info { "Stopping Discord bot..." }
        try {
            botJob?.cancel()
            runBlocking { discordBotUseCases.stopBot() }
            logger.info { "Discord bot stopped" }
        } catch (e: Exception) {
            logger.error(e) { "Error stopping Discord bot" }
        }
    }

    companion object {
        private const val CONNECTION_VERIFICATION_DELAY_MS = 3000L
    }
}

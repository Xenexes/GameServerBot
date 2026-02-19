package de.xenexes.gameserverbot.config

import de.xenexes.gameserverbot.infrastructure.http.nitrado.NitradoProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ConfigurationValidator(
    private val nitradoProperties: NitradoProperties,
    private val discordProperties: DiscordProperties,
) {
    private val logger = KotlinLogging.logger {}

    @EventListener(ApplicationReadyEvent::class)
    fun validateConfiguration() {
        logger.info { "Validating application configuration..." }

        validateNitradoConfiguration()
        validateDiscordConfiguration()

        logger.info { "Configuration validation completed" }
    }

    private fun validateNitradoConfiguration() {
        if (nitradoProperties.apiToken.isBlank()) {
            logger.warn {
                "Nitrado API token not configured (NITRADO_API_TOKEN). " +
                    "Server management features will use stub implementation."
            }
        } else {
            logger.info { "Nitrado API configured with base URL: ${nitradoProperties.baseUrl}" }
        }
    }

    private fun validateDiscordConfiguration() {
        if (!discordProperties.enabled) {
            logger.info { "Discord notifications are disabled" }
            return
        }

        if (discordProperties.webhookUrl.isNullOrBlank()) {
            logger.warn {
                "Discord is enabled but webhook URL not configured (DISCORD_WEBHOOK_URL). " +
                    "Notifications will not be sent."
            }
        } else {
            logger.info { "Discord notifications enabled" }
        }
    }
}

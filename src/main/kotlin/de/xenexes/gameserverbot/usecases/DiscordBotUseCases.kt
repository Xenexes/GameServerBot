package de.xenexes.gameserverbot.usecases

import arrow.core.Either
import arrow.core.raise.either
import de.xenexes.gameserverbot.domain.discord.DiscordAutocompleteContext
import de.xenexes.gameserverbot.domain.discord.DiscordBotEvent
import de.xenexes.gameserverbot.domain.discord.DiscordInteraction
import de.xenexes.gameserverbot.ports.outbound.DiscordClientGateway
import de.xenexes.gameserverbot.ports.outbound.DomainEventPublisher
import de.xenexes.gameserverbot.ports.outbound.failure.DiscordFailure
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Clock

@Component
@ConditionalOnProperty(
    name = ["gameserverbot.discord.enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
class DiscordBotUseCases(
    private val discordClient: DiscordClientGateway,
    private val eventPublisher: DomainEventPublisher,
    private val clock: Clock,
    @param:Value("\${gameserverbot.discord.guild-id:}")
    private val guildId: String = "",
) {
    suspend fun startBot(): Either<DiscordFailure, Unit> = discordClient.start()

    suspend fun stopBot(): Either<DiscordFailure, Unit> = discordClient.stop()

    suspend fun isReady(): Boolean = discordClient.isReady()

    suspend fun registerConfiguredCommands(): Either<DiscordFailure, Unit> =
        either {
            if (guildId.isNotBlank()) discordClient.registerSlashCommands(guildId).bind()
        }

    suspend fun loginBot(): Either<DiscordFailure, Unit> = discordClient.login()

    suspend fun registerCommandHandler(handler: suspend (DiscordInteraction) -> Unit) =
        discordClient.onCommandInteraction(handler)

    suspend fun registerAutocompleteHandler(handler: suspend (DiscordAutocompleteContext) -> Unit) =
        discordClient.onAutocompleteInteraction(handler)

    suspend fun publishBotReady() = eventPublisher.publish(DiscordBotEvent.BotReady(occurredAt = clock.instant()))
}

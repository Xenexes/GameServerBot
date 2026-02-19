package de.xenexes.gameserverbot.domain.discord

import de.xenexes.gameserverbot.domain.shared.DomainEvent
import java.time.Instant
import java.util.UUID

sealed interface DiscordBotEvent : DomainEvent<DiscordBotId> {
    data class BotReady(
        override val aggregateId: DiscordBotId = DiscordBotId("discord-bot"),
        override val occurredAt: Instant,
        override val eventId: String = UUID.randomUUID().toString(),
        override val eventType: String = "BotReady",
    ) : DiscordBotEvent

    data class BotStopped(
        override val aggregateId: DiscordBotId = DiscordBotId("discord-bot"),
        override val occurredAt: Instant,
        override val eventId: String = UUID.randomUUID().toString(),
        override val eventType: String = "BotStopped",
    ) : DiscordBotEvent
}

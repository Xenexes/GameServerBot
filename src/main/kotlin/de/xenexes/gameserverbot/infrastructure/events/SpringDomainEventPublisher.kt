package de.xenexes.gameserverbot.infrastructure.events

import de.xenexes.gameserverbot.domain.shared.DomainEvent
import de.xenexes.gameserverbot.ports.outbound.DomainEventPublisher
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class SpringDomainEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
) : DomainEventPublisher {
    private val logger = KotlinLogging.logger {}

    override suspend fun publish(events: List<DomainEvent<*>>) {
        events.forEach { event ->
            logger.info { "Publishing domain event: ${event::class.simpleName}" }
            applicationEventPublisher.publishEvent(event)
        }
    }
}

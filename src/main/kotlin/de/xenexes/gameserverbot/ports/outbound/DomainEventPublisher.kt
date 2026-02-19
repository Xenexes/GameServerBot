package de.xenexes.gameserverbot.ports.outbound

import de.xenexes.gameserverbot.domain.shared.DomainEvent

interface DomainEventPublisher {
    suspend fun publish(events: List<DomainEvent<*>>)

    suspend fun publish(event: DomainEvent<*>) = publish(listOf(event))
}

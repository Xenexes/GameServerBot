package de.xenexes.gameserverbot.domain.shared

import java.time.Instant

interface DomainEvent<AggregateId> {
    val eventId: String
    val aggregateId: AggregateId
    val occurredAt: Instant
    val eventType: String
}

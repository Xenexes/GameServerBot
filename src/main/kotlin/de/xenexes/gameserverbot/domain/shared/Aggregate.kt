package de.xenexes.gameserverbot.domain.shared

/**
 * Marker interface for domain aggregates.
 *
 * Every aggregate must be able to produce and consume its pending domain events.
 * The storage of pending events (e.g. a MutableList in the primary constructor)
 * is left to the implementor because Kotlin data-class copy() semantics are
 * incompatible with mutable state held in an abstract base class.
 */
interface Aggregate<ID, E : DomainEvent<ID>> {
    val id: ID

    /** Returns all events accumulated since the last call and clears the internal list. */
    fun consumeEvents(): List<E>
}

package de.xenexes.gameserverbot.domain.player

import java.util.UUID

@JvmInline
value class PlayerId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "PlayerId cannot be blank" }
    }

    companion object {
        fun create(): PlayerId = PlayerId(UUID.randomUUID().toString())
    }
}

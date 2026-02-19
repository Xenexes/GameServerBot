package de.xenexes.gameserverbot.domain.gameserver

import java.util.UUID

@JvmInline
value class GameServerId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "GameServerId cannot be blank" }
    }

    companion object {
        fun create(): GameServerId = GameServerId(UUID.randomUUID().toString())
    }
}

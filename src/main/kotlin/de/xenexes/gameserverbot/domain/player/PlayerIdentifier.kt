package de.xenexes.gameserverbot.domain.player

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure

@JvmInline
value class PlayerIdentifier(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "PlayerIdentifier cannot be blank" }
    }

    val type: IdentifierType
        get() =
            when {
                value.matches(Regex("^\\d{17}$")) -> IdentifierType.STEAM_ID
                value.matches(Regex("^[a-f0-9]{8}-([a-f0-9]{4}-){3}[a-f0-9]{12}$", RegexOption.IGNORE_CASE)) ->
                    IdentifierType.MINECRAFT_UUID
                else -> IdentifierType.USERNAME
            }

    companion object {
        fun create(value: String): Either<PlayerFailure, PlayerIdentifier> =
            either {
                ensure(value.isNotBlank()) { PlayerFailure.InvalidIdentifier("Player identifier cannot be blank") }
                PlayerIdentifier(value.trim())
            }
    }
}

enum class IdentifierType {
    STEAM_ID,
    MINECRAFT_UUID,
    USERNAME,
}

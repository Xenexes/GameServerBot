package de.xenexes.gameserverbot.domain.player

data class Player(
    val id: PlayerId,
    val name: String,
    val identifier: PlayerIdentifier,
    val isOnline: Boolean = false,
) {
    companion object {
        fun create(
            name: String,
            identifier: PlayerIdentifier,
            isOnline: Boolean = false,
        ): Player =
            Player(
                id = PlayerId.create(),
                name = name,
                identifier = identifier,
                isOnline = isOnline,
            )

        fun fromListEntry(
            name: String,
            externalId: String,
        ): Player =
            Player(
                id = PlayerId.create(),
                name = name,
                identifier = PlayerIdentifier(externalId),
                isOnline = false,
            )
    }
}

package de.xenexes.gameserverbot.domain.player

data class PlayerListEntry(
    val name: String,
    val identifier: PlayerIdentifier,
    val identifierType: IdentifierType,
) {
    companion object {
        fun create(
            name: String,
            identifier: String,
            identifierType: IdentifierType,
        ): PlayerListEntry =
            PlayerListEntry(
                name = name,
                identifier = PlayerIdentifier(identifier),
                identifierType = identifierType,
            )
    }
}

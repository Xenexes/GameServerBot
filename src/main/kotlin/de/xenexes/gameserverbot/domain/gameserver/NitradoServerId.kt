package de.xenexes.gameserverbot.domain.gameserver

@JvmInline
value class NitradoServerId(
    val value: Long,
) {
    init {
        require(value > 0) { "NitradoServerId must be positive" }
    }
}

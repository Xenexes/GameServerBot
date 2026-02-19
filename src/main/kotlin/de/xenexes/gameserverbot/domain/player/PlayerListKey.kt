package de.xenexes.gameserverbot.domain.player

import de.xenexes.gameserverbot.domain.gameserver.GameServerId

data class PlayerListKey(
    val serverId: GameServerId,
    val listType: PlayerListType,
)

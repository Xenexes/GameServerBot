package de.xenexes.gameserverbot.api.events

import de.xenexes.gameserverbot.domain.gameserver.NitradoServerId
import de.xenexes.gameserverbot.domain.shared.UserContext
import de.xenexes.gameserverbot.usecases.CreateGameServerCommand
import de.xenexes.gameserverbot.usecases.GameServerUseCases
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class GameServerStartupAdapter(
    private val useCases: GameServerUseCases,
    @param:Value("\${gameserverbot.nitrado.server-id:}")
    private val serverId: String = "",
    @param:Value("\${gameserverbot.nitrado.server-name:Game Server}")
    private val serverName: String = "Game Server",
) {
    private val logger = KotlinLogging.logger {}

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        if (serverId.isBlank()) return

        CoroutineScope(Dispatchers.Default).launch {
            with(UserContext.system) {
                useCases
                    .create(
                        CreateGameServerCommand(
                            name = serverName,
                            nitradoId = NitradoServerId(serverId.toLong()),
                        ),
                    ).onRight { logger.info { "Auto-registered server '${it.name}' (Nitrado ID: $serverId)" } }
                    .onLeft { logger.info { "Server with Nitrado ID $serverId already registered: $it" } }
            }
        }
    }
}

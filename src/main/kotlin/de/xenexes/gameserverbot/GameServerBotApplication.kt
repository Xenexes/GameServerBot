package de.xenexes.gameserverbot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GameServerBotApplication

fun main(args: Array<String>) {
    runApplication<GameServerBotApplication>(*args)
}

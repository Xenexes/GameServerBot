package de.xenexes.gameserverbot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
class GameServerBotApplication

fun main(args: Array<String>) {
    runApplication<GameServerBotApplication>(*args)
}

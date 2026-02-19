package de.xenexes.gameserverbot.domain.discord

interface DiscordInteraction {
    val commandName: String

    suspend fun getUser(): DiscordUser

    suspend fun defer()

    suspend fun editResponse(message: String)

    suspend fun respondImmediately(message: String)

    fun getSubcommandName(): String?

    fun getStringParameter(name: String): String?

    fun getIntParameter(name: String): Int?
}

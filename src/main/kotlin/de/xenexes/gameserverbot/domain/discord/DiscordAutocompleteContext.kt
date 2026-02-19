package de.xenexes.gameserverbot.domain.discord

interface DiscordAutocompleteContext {
    val focusedOptionName: String?
    val currentInput: String
    val userId: String

    suspend fun suggestChoices(choices: List<AutocompleteChoice>)
}

data class AutocompleteChoice(
    val name: String,
    val value: String,
)

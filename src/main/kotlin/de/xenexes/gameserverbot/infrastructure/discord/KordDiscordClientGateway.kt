package de.xenexes.gameserverbot.infrastructure.discord

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import de.xenexes.gameserverbot.config.DiscordProperties
import de.xenexes.gameserverbot.domain.discord.AutocompleteChoice
import de.xenexes.gameserverbot.domain.discord.DiscordAutocompleteContext
import de.xenexes.gameserverbot.domain.discord.DiscordInteraction
import de.xenexes.gameserverbot.ports.outbound.DiscordClientGateway
import de.xenexes.gameserverbot.ports.outbound.EmbedField
import de.xenexes.gameserverbot.ports.outbound.failure.DiscordFailure
import dev.kord.common.Color
import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.behavior.interaction.suggestString
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.interaction.GuildAutoCompleteInteractionCreateEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.embed
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.time.Instant as KotlinxInstant

class KordDiscordClientGateway(
    private val properties: DiscordProperties,
    private val slashCommandRegistration: KordSlashCommandRegistration,
) : DiscordClientGateway {
    private val logger = KotlinLogging.logger {}
    private var kord: Kord? = null
    private var loginJob: Job? = null

    override suspend fun start(): Either<DiscordFailure, Unit> =
        either {
            val token =
                ensureNotNull(properties.botToken?.takeIf { it.isNotBlank() }) {
                    DiscordFailure.NotInitialized("Bot token not configured")
                }
            catch({
                kord = Kord(token)
                logger.info { "Kord client initialized" }
            }) { e ->
                logger.error(e) { "Failed to initialize Kord client" }
                raise(DiscordFailure.ConnectionFailed(e.message ?: "Unknown error"))
            }
        }

    override suspend fun login(): Either<DiscordFailure, Unit> =
        either {
            val kordClient = ensureNotNull(kord) { DiscordFailure.NotInitialized("Kord not initialized") }
            loginJob =
                CoroutineScope(Dispatchers.Default).launch {
                    catch({ kordClient.login() }) { e ->
                        logger.error(e) { "Discord gateway connection lost" }
                    }
                }
            logger.info { "Discord Gateway login initiated" }
        }

    override suspend fun stop(): Either<DiscordFailure, Unit> =
        either {
            catch({
                loginJob?.cancel()
                loginJob = null
                kord?.shutdown()
                kord = null
                logger.info { "Kord client stopped" }
            }) { e ->
                raise(DiscordFailure.ConnectionFailed(e.message ?: "Unknown error"))
            }
        }

    override suspend fun isReady(): Boolean = kord != null && loginJob?.isActive == true

    override suspend fun registerSlashCommands(guildId: String): Either<DiscordFailure, Unit> =
        either {
            val kordClient =
                ensureNotNull(kord) {
                    DiscordFailure.NotInitialized("Kord not initialized")
                }
            catch({
                slashCommandRegistration.registerAllCommands(kordClient, Snowflake(guildId))
                logger.info { "Slash commands registered for guild $guildId" }
            }) { e ->
                logger.error(e) { "Failed to register slash commands" }
                raise(DiscordFailure.CommandFailed(e.message ?: "Unknown error"))
            }
        }

    override suspend fun updatePresence(
        status: String,
        activityText: String,
    ): Either<DiscordFailure, Unit> =
        either {
            val kordClient =
                ensureNotNull(kord) {
                    DiscordFailure.NotInitialized("Kord not initialized")
                }
            catch({
                kordClient.editPresence {
                    this.status =
                        when (status.lowercase()) {
                            "online" -> PresenceStatus.Online
                            "idle" -> PresenceStatus.Idle
                            "dnd" -> PresenceStatus.DoNotDisturb
                            else -> PresenceStatus.Online
                        }
                    playing(activityText)
                }
            }) { e ->
                raise(DiscordFailure.ConnectionFailed(e.message ?: "Unknown error"))
            }
        }

    override suspend fun sendMessage(
        channelId: String,
        message: String,
    ): Either<DiscordFailure, Unit> =
        either {
            val kordClient =
                ensureNotNull(kord) {
                    DiscordFailure.NotInitialized("Kord not initialized")
                }
            catch({
                val channel =
                    ensureNotNull(kordClient.getChannel(Snowflake(channelId))) {
                        DiscordFailure.ChannelNotFound(channelId)
                    }
                (channel as? TextChannel)?.createMessage(message)
            }) { e ->
                raise(DiscordFailure.ConnectionFailed(e.message ?: "Unknown error"))
            }
        }

    override suspend fun sendEmbeddedMessage(
        channelId: String,
        title: String,
        description: String,
        color: Int?,
        fields: List<EmbedField>,
        timestamp: Instant?,
        footer: String?,
    ): Either<DiscordFailure, Unit> =
        either {
            val kordClient =
                ensureNotNull(kord) {
                    DiscordFailure.NotInitialized("Kord not initialized")
                }
            catch({
                val channel =
                    ensureNotNull(kordClient.getChannel(Snowflake(channelId))) {
                        DiscordFailure.ChannelNotFound(channelId)
                    }
                (channel as? TextChannel)?.createMessage {
                    embed {
                        this.title = title
                        this.description = description
                        color?.let { this.color = Color(it) }
                        timestamp?.let { this.timestamp = KotlinxInstant.fromEpochMilliseconds(it.toEpochMilli()) }
                        footer?.let { footer { text = it } }
                        fields.forEach { field ->
                            this.field {
                                name = field.name
                                value = field.value
                                inline = field.inline
                            }
                        }
                    }
                }
            }) { e ->
                raise(DiscordFailure.ConnectionFailed(e.message ?: "Unknown error"))
            }
        }

    override suspend fun onCommandInteraction(handler: suspend (DiscordInteraction) -> Unit) {
        val kordClient = kord ?: return

        kordClient.on<GuildChatInputCommandInteractionCreateEvent> {
            try {
                val interaction = KordDiscordInteraction(this, properties)
                handler(interaction)
            } catch (e: Exception) {
                logger.error(e) { "Error handling command" }
                interaction.respondPublic { content = "An error occurred while processing your command." }
            }
        }

        logger.info { "Discord command event handling configured" }
    }

    override suspend fun onAutocompleteInteraction(handler: suspend (DiscordAutocompleteContext) -> Unit) {
        val kordClient = kord ?: return

        kordClient.on<GuildAutoCompleteInteractionCreateEvent> {
            try {
                val context = KordAutocompleteContext(this)
                handler(context)
            } catch (e: Exception) {
                logger.error(e) { "Error handling autocomplete" }
            }
        }

        logger.info { "Discord autocomplete handling configured" }
    }

    private class KordAutocompleteContext(
        private val event: GuildAutoCompleteInteractionCreateEvent,
    ) : DiscordAutocompleteContext {
        override val focusedOptionName: String? =
            event.interaction.command.options.values
                .firstOrNull { it.focused }
                ?.let {
                    event.interaction.command.options.keys
                        .firstOrNull()
                }

        override val currentInput: String =
            (
                event.interaction.command.options.values
                    .firstOrNull { it.focused }
                    ?.value as? String
            ) ?: ""

        override val userId: String =
            event.interaction.user.id
                .toString()

        override suspend fun suggestChoices(choices: List<AutocompleteChoice>) {
            event.interaction.suggestString {
                choices.forEach { choice(it.name, it.value) }
            }
        }
    }
}

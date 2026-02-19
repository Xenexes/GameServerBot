package de.xenexes.gameserverbot.unit.infrastructure

import arrow.core.right
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import de.xenexes.gameserverbot.config.DiscordProperties
import de.xenexes.gameserverbot.domain.gameserver.GameServerEvent.GameServerCreatedEvent
import de.xenexes.gameserverbot.domain.gameserver.GameServerEvent.GameServerDeletedEvent
import de.xenexes.gameserverbot.domain.gameserver.GameServerEvent.GameServerStatusChangedEvent
import de.xenexes.gameserverbot.domain.gameserver.GameServerId
import de.xenexes.gameserverbot.domain.gameserver.GameServerStatus
import de.xenexes.gameserverbot.domain.gameserver.NitradoServerId
import de.xenexes.gameserverbot.domain.player.PlayerEvent
import de.xenexes.gameserverbot.domain.player.PlayerIdentifier
import de.xenexes.gameserverbot.domain.player.PlayerListKey
import de.xenexes.gameserverbot.domain.player.PlayerListType
import de.xenexes.gameserverbot.domain.shared.UserId
import de.xenexes.gameserverbot.infrastructure.discord.DiscordNotificationAdapter
import de.xenexes.gameserverbot.ports.outbound.DiscordClientGateway
import de.xenexes.gameserverbot.ports.outbound.EmbedField
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

class DiscordNotificationAdapterTest {
    private lateinit var discordClientGateway: DiscordClientGateway
    private lateinit var properties: DiscordProperties
    private lateinit var adapter: DiscordNotificationAdapter

    private val channelId = "test-channel-123"

    @BeforeEach
    fun setup() {
        discordClientGateway = mockk()
        properties =
            DiscordProperties(
                enabled = true,
                notificationChannelId = channelId,
            )
        adapter = DiscordNotificationAdapter(discordClientGateway, properties)
    }

    @Nested
    inner class NotifyStatusChanged {
        @Test
        fun `should send embed for status degradation`() =
            runTest {
                // Given
                val titleSlot = slot<String>()
                val fieldsSlot = slot<List<EmbedField>>()
                val timestampSlot = slot<Instant>()
                val footerSlot = slot<String>()
                coEvery {
                    discordClientGateway.sendEmbeddedMessage(
                        channelId = channelId,
                        title = capture(titleSlot),
                        description = any(),
                        color = any(),
                        fields = capture(fieldsSlot),
                        timestamp = capture(timestampSlot),
                        footer = capture(footerSlot),
                    )
                } returns Unit.right()

                val occurredAt = Instant.parse("2024-01-15T10:00:00Z")
                val event =
                    GameServerStatusChangedEvent(
                        aggregateId = GameServerId.create(),
                        serverName = "My 7D2D Server",
                        previousStatus = GameServerStatus.STARTED,
                        newStatus = GameServerStatus.STOPPED,
                        triggeredBy = UserId("admin"),
                        occurredAt = occurredAt,
                    )

                // When
                val result = adapter.notifyStatusChanged(event)

                // Then
                assertThat(result.isRight()).isTrue()
                assertThat(titleSlot.captured).isEqualTo("🔴 Server Health Degraded")
                assertThat(fieldsSlot.captured.first { it.name == "Server" }.value).isEqualTo("My 7D2D Server")
                assertThat(fieldsSlot.captured.first { it.name == "Previous Status" }.value).isEqualTo("Started")
                assertThat(fieldsSlot.captured.first { it.name == "New Status" }.value).isEqualTo("Stopped")
                assertThat(timestampSlot.captured).isEqualTo(occurredAt)
                assertThat(footerSlot.captured).isEqualTo("GameServerBot")
            }
    }

    @Nested
    inner class NotifyServerCreated {
        @Test
        fun `should send embed with server name and nitrado id`() =
            runTest {
                // Given
                val titleSlot = slot<String>()
                val fieldsSlot = slot<List<EmbedField>>()
                val timestampSlot = slot<Instant>()
                coEvery {
                    discordClientGateway.sendEmbeddedMessage(
                        channelId = channelId,
                        title = capture(titleSlot),
                        description = any(),
                        color = any(),
                        fields = capture(fieldsSlot),
                        timestamp = capture(timestampSlot),
                        footer = any(),
                    )
                } returns Unit.right()

                val occurredAt = Instant.parse("2024-01-15T10:00:00Z")
                val event =
                    GameServerCreatedEvent(
                        aggregateId = GameServerId.create(),
                        name = "My Game Server",
                        nitradoId = NitradoServerId(12345),
                        createdBy = UserId("admin"),
                        occurredAt = occurredAt,
                    )

                // When
                val result = adapter.notifyServerCreated(event)

                // Then
                assertThat(result.isRight()).isTrue()
                assertThat(titleSlot.captured).isEqualTo("📋 Server Registered")
                assertThat(fieldsSlot.captured.first { it.name == "Name" }.value).isEqualTo("My Game Server")
                assertThat(fieldsSlot.captured.first { it.name == "Nitrado ID" }.value).isEqualTo("12345")
                assertThat(timestampSlot.captured).isEqualTo(occurredAt)
            }
    }

    @Nested
    inner class NotifyServerDeleted {
        @Test
        fun `should send embed with server name and deleted by`() =
            runTest {
                // Given
                val titleSlot = slot<String>()
                val fieldsSlot = slot<List<EmbedField>>()
                val timestampSlot = slot<Instant>()
                coEvery {
                    discordClientGateway.sendEmbeddedMessage(
                        channelId = channelId,
                        title = capture(titleSlot),
                        description = any(),
                        color = any(),
                        fields = capture(fieldsSlot),
                        timestamp = capture(timestampSlot),
                        footer = any(),
                    )
                } returns Unit.right()

                val occurredAt = Instant.parse("2024-01-15T10:00:00Z")
                val event =
                    GameServerDeletedEvent(
                        aggregateId = GameServerId.create(),
                        serverName = "My 7D2D Server",
                        deletedBy = UserId("admin"),
                        occurredAt = occurredAt,
                    )

                // When
                val result = adapter.notifyServerDeleted(event)

                // Then
                assertThat(result.isRight()).isTrue()
                assertThat(titleSlot.captured).isEqualTo("🗑️ Server Removed")
                assertThat(fieldsSlot.captured.first { it.name == "Server Name" }.value).isEqualTo("My 7D2D Server")
                assertThat(fieldsSlot.captured.first { it.name == "Removed By" }.value).isEqualTo("admin")
                assertThat(timestampSlot.captured).isEqualTo(occurredAt)
            }
    }

    @Nested
    inner class NotifyPlayerListChanged {
        @Test
        fun `should send embed when player added to whitelist`() =
            runTest {
                // Given
                val titleSlot = slot<String>()
                val timestampSlot = slot<Instant>()
                coEvery {
                    discordClientGateway.sendEmbeddedMessage(
                        channelId = channelId,
                        title = capture(titleSlot),
                        description = any(),
                        color = any(),
                        fields = any(),
                        timestamp = capture(timestampSlot),
                        footer = any(),
                    )
                } returns Unit.right()

                val occurredAt = Instant.parse("2024-01-15T10:00:00Z")
                val event =
                    PlayerEvent.PlayerAddedToList(
                        aggregateId = PlayerListKey(GameServerId.create(), PlayerListType.WHITELIST),
                        listType = PlayerListType.WHITELIST,
                        playerName = "NewPlayer",
                        identifier = PlayerIdentifier("player-id"),
                        occurredAt = occurredAt,
                    )

                // When
                val result = adapter.notifyPlayerListChanged(event)

                // Then
                assertThat(result.isRight()).isTrue()
                assertThat(titleSlot.captured).isEqualTo("✅ Player Whitelisted")
                assertThat(timestampSlot.captured).isEqualTo(occurredAt)
            }

        @Test
        fun `should send embed when player banned`() =
            runTest {
                // Given
                val titleSlot = slot<String>()
                coEvery {
                    discordClientGateway.sendEmbeddedMessage(
                        channelId = channelId,
                        title = capture(titleSlot),
                        description = any(),
                        color = any(),
                        fields = any(),
                        timestamp = any(),
                        footer = any(),
                    )
                } returns Unit.right()

                val event =
                    PlayerEvent.PlayerAddedToList(
                        aggregateId = PlayerListKey(GameServerId.create(), PlayerListType.BANLIST),
                        listType = PlayerListType.BANLIST,
                        playerName = "Cheater",
                        identifier = PlayerIdentifier("cheater-id"),
                        occurredAt = Instant.now(),
                    )

                // When
                val result = adapter.notifyPlayerListChanged(event)

                // Then
                assertThat(result.isRight()).isTrue()
                assertThat(titleSlot.captured).isEqualTo("🔨 Player Banned")
            }
    }

    @Nested
    inner class ChannelNotConfigured {
        @Test
        fun `should return failure when channel id is not configured`() =
            runTest {
                // Given
                val adapterNoChannel =
                    DiscordNotificationAdapter(
                        discordClientGateway,
                        DiscordProperties(enabled = true, notificationChannelId = null),
                    )
                val event =
                    GameServerCreatedEvent(
                        aggregateId = GameServerId.create(),
                        name = "Test",
                        nitradoId = NitradoServerId(1),
                        createdBy = UserId("admin"),
                        occurredAt = Instant.parse("2024-01-15T10:00:00Z"),
                    )

                // When
                val result = adapterNoChannel.notifyServerCreated(event)

                // Then
                assertThat(result.isLeft()).isTrue()
                coVerify(exactly = 0) {
                    discordClientGateway.sendEmbeddedMessage(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                    )
                }
            }
    }
}

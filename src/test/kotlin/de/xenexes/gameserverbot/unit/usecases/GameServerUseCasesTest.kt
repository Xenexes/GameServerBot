package de.xenexes.gameserverbot.unit.usecases

import arrow.core.right
import de.xenexes.gameserverbot.domain.gameserver.GameServer
import de.xenexes.gameserverbot.domain.gameserver.GameServerId
import de.xenexes.gameserverbot.domain.gameserver.GameServerStatus
import de.xenexes.gameserverbot.domain.gameserver.NitradoServerId
import de.xenexes.gameserverbot.domain.shared.UserContext
import de.xenexes.gameserverbot.ports.outbound.NitradoServerInfo
import de.xenexes.gameserverbot.ports.outbound.failure.RepositoryFailure
import de.xenexes.gameserverbot.usecases.CreateGameServerCommand
import de.xenexes.gameserverbot.usecases.GameServerUseCases
import de.xenexes.gameserverbot.usecases.UseCaseError
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameServerUseCasesTest {
    private lateinit var repository: InMemoryGameServerRepositoryDouble
    private lateinit var nitradoGateway: FakeNitradoGateway
    private lateinit var eventPublisher: FakeDomainEventPublisher
    private lateinit var useCases: GameServerUseCases

    private val fixedInstant = Instant.parse("2024-01-15T10:00:00Z")
    private val fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC)
    private val testContext = UserContext.api("test-user")
    private val nitradoId = NitradoServerId(12345L)

    @BeforeEach
    fun setup() {
        repository = InMemoryGameServerRepositoryDouble()
        nitradoGateway = FakeNitradoGateway()
        eventPublisher = FakeDomainEventPublisher()
        useCases = GameServerUseCases(repository, nitradoGateway, eventPublisher, fixedClock)
    }

    @Test
    fun `findById should return server when found`() =
        runTest {
            // Given
            val server = createTestServer(GameServerStatus.STARTED)
            repository.addServer(server)

            with(testContext) {
                // When
                val result = useCases.findById(server.id)

                // Then
                assertTrue(result.isRight())
                assertEquals(server, result.getOrNull())
            }
        }

    @Test
    fun `findById should return error when not found`() =
        runTest {
            // Given
            val serverId = GameServerId.create()

            with(testContext) {
                // When
                val result = useCases.findById(serverId)

                // Then
                assertTrue(result.isLeft())
                assertIs<UseCaseError.Repository<*>>(result.leftOrNull())
            }
        }

    @Test
    fun `create should save server and publish events`() =
        runTest {
            // Given
            val command = CreateGameServerCommand("Test Server", nitradoId)

            with(testContext) {
                // When
                val result = useCases.create(command)

                // Then
                assertTrue(result.isRight())
                val server = result.getOrNull()!!
                assertEquals("Test Server", server.name)
                assertEquals(nitradoId, server.nitradoId)
                assertEquals(GameServerStatus.UNKNOWN, server.status)
                assertTrue(eventPublisher.publishedEvents.isNotEmpty())
            }
        }

    @Test
    fun `create should fail when nitrado id already exists`() =
        runTest {
            // Given
            val existingServer = createTestServer(GameServerStatus.STARTED)
            repository.addServer(existingServer)
            val command = CreateGameServerCommand("Another Server", nitradoId)

            with(testContext) {
                // When
                val result = useCases.create(command)

                // Then
                assertTrue(result.isLeft())
                assertIs<UseCaseError.GameServer<*>>(result.leftOrNull())
            }
        }

    @Test
    fun `startServer should start server when in stopped state`() =
        runTest {
            // Given
            val server = createTestServer(GameServerStatus.STOPPED)
            repository.addServer(server)

            with(testContext) {
                // When
                val result = useCases.startServer(server.id)

                // Then
                assertTrue(result.isRight())
                assertEquals(GameServerStatus.RESTARTING, result.getOrNull()?.status)
                assertTrue(nitradoGateway.startServerCalls.contains(server.nitradoId))
                assertTrue(eventPublisher.publishedEvents.isNotEmpty())
            }
        }

    @Test
    fun `startServer should fail when server is already started`() =
        runTest {
            // Given
            val server = createTestServer(GameServerStatus.STARTED)
            repository.addServer(server)

            with(testContext) {
                // When
                val result = useCases.startServer(server.id)

                // Then
                assertTrue(result.isLeft())
                assertIs<UseCaseError.GameServer<*>>(result.leftOrNull())
            }
        }

    @Test
    fun `startServer should fail when server not found`() =
        runTest {
            // Given
            val serverId = GameServerId.create()

            with(testContext) {
                // When
                val result = useCases.startServer(serverId)

                // Then
                assertTrue(result.isLeft())
                assertIs<UseCaseError.Repository<*>>(result.leftOrNull())
            }
        }

    @Test
    fun `stopServer should stop server when started`() =
        runTest {
            // Given
            val server = createTestServer(GameServerStatus.STARTED)
            repository.addServer(server)

            with(testContext) {
                // When
                val result = useCases.stopServer(server.id)

                // Then
                assertTrue(result.isRight())
                assertEquals(GameServerStatus.STOPPING, result.getOrNull()?.status)
                assertTrue(nitradoGateway.stopServerCalls.any { it.first == server.nitradoId })
            }
        }

    @Test
    fun `stopServer should fail when server is not started`() =
        runTest {
            // Given
            val server = createTestServer(GameServerStatus.STOPPED)
            repository.addServer(server)

            with(testContext) {
                // When
                val result = useCases.stopServer(server.id)

                // Then
                assertTrue(result.isLeft())
                assertIs<UseCaseError.GameServer<*>>(result.leftOrNull())
            }
        }

    @Test
    fun `restartServer should restart server when started`() =
        runTest {
            // Given
            val server = createTestServer(GameServerStatus.STARTED)
            repository.addServer(server)

            with(testContext) {
                // When
                val result = useCases.restartServer(server.id)

                // Then
                assertTrue(result.isRight())
                assertEquals(GameServerStatus.RESTARTING, result.getOrNull()?.status)
                assertTrue(nitradoGateway.restartServerCalls.any { it.first == server.nitradoId })
            }
        }

    @Test
    fun `restartServer should fail when server is stopped`() =
        runTest {
            // Given
            val server = createTestServer(GameServerStatus.STOPPED)
            repository.addServer(server)

            with(testContext) {
                // When
                val result = useCases.restartServer(server.id)

                // Then
                assertTrue(result.isLeft())
                assertIs<UseCaseError.GameServer<*>>(result.leftOrNull())
            }
        }

    @Test
    fun `findAll should return all servers`() =
        runTest {
            // Given
            val server1 = createTestServer(GameServerStatus.STARTED)
            val server2 = createTestServer(GameServerStatus.STOPPED)
            repository.addServer(server1)
            repository.addServer(server2)

            with(testContext) {
                // When
                val result = useCases.findAll()

                // Then
                assertTrue(result.isRight())
                assertEquals(2, result.getOrNull()?.size)
            }
        }

    @Test
    fun `findAll should return error when repository fails`() =
        runTest {
            // Given
            repository.findAllError = RepositoryFailure.ConnectionError(RuntimeException("DB down"))

            with(testContext) {
                // When
                val result = useCases.findAll()

                // Then
                assertTrue(result.isLeft())
                assertIs<UseCaseError.Repository<*>>(result.leftOrNull())
            }
        }

    @Test
    fun `syncServer should update server when status changed`() =
        runTest {
            // Given
            val server = createTestServer(GameServerStatus.STOPPED)
            repository.addServer(server)
            nitradoGateway.fetchServerInfoResult = createNitradoServerInfo(GameServerStatus.STARTED).right()

            with(testContext) {
                // When
                val result = useCases.syncServer(server.id)

                // Then
                assertTrue(result.isRight())
                val updated = result.getOrNull()
                assertEquals(GameServerStatus.STARTED, updated?.status)
                assertEquals("192.168.1.1", updated?.ip)
                assertTrue(eventPublisher.publishedEvents.isNotEmpty())
            }
        }

    @Test
    fun `syncServer should return null when no status change but still save metadata`() =
        runTest {
            // Given
            val server = createTestServer(GameServerStatus.STARTED)
            repository.addServer(server)
            nitradoGateway.fetchServerInfoResult = createNitradoServerInfo(GameServerStatus.STARTED).right()

            with(testContext) {
                // When
                val result = useCases.syncServer(server.id)

                // Then
                assertTrue(result.isRight())
                assertNull(result.getOrNull())
                // Metadata must be saved even without status change
                val saved = repository.findById(server.id)
                assertEquals("192.168.1.1", saved.getOrNull()?.ip)
                assertTrue(eventPublisher.publishedEvents.isEmpty())
            }
        }

    @Test
    fun `syncServer should return error when server not found`() =
        runTest {
            // Given
            val serverId = GameServerId.create()

            with(testContext) {
                // When
                val result = useCases.syncServer(serverId)

                // Then
                assertTrue(result.isLeft())
                assertIs<UseCaseError.Repository<*>>(result.leftOrNull())
            }
        }

    @Test
    fun `syncAllServers should sync all servers`() =
        runTest {
            // Given
            val server1 = createTestServer(GameServerStatus.STOPPED)
            val server2 = createTestServer(GameServerStatus.STARTED)
            repository.addServer(server1)
            repository.addServer(server2)
            nitradoGateway.fetchServerInfoResult = createNitradoServerInfo(GameServerStatus.STARTED).right()

            with(testContext) {
                // When
                val results = useCases.syncAllServers()

                // Then
                assertEquals(2, results.size)
                assertTrue(results.all { it.isRight() })
            }
        }

    @Test
    fun `syncAllServers should return empty list when findAll fails`() =
        runTest {
            // Given
            repository.findAllError = RepositoryFailure.ConnectionError(RuntimeException("Connection failed"))

            with(testContext) {
                // When
                val results = useCases.syncAllServers()

                // Then
                assertTrue(results.isEmpty())
            }
        }

    @Test
    fun `deleteServer should delete server and publish event`() =
        runTest {
            // Given
            val server = createTestServer(GameServerStatus.STARTED)
            repository.addServer(server)

            with(testContext) {
                // When
                val result = useCases.deleteServer(server.id)

                // Then
                assertTrue(result.isRight())
                assertTrue(eventPublisher.publishedEvents.isNotEmpty())
                val findResult = repository.findById(server.id)
                assertTrue(findResult.isLeft())
            }
        }

    @Test
    fun `deleteServer should return error when server not found`() =
        runTest {
            // Given
            val serverId = GameServerId.create()

            with(testContext) {
                // When
                val result = useCases.deleteServer(serverId)

                // Then
                assertTrue(result.isLeft())
                assertIs<UseCaseError.Repository<*>>(result.leftOrNull())
            }
        }

    private fun createTestServer(status: GameServerStatus): GameServer =
        GameServer.restore(
            id = GameServerId.create(),
            name = "Test Server",
            status = status,
            nitradoId = nitradoId,
            ip = "127.0.0.1",
            port = 25565,
            gameCode = "minecraft",
            gameName = "Minecraft",
            playerSlots = 20,
            location = "EU",
            createdAt = fixedInstant,
            updatedAt = fixedInstant,
        )

    private fun createNitradoServerInfo(status: GameServerStatus): NitradoServerInfo =
        NitradoServerInfo(
            nitradoId = nitradoId,
            status = status,
            ip = "192.168.1.1",
            port = 25565,
            gameCode = "minecraft",
            gameName = "Minecraft",
            slots = 20,
            location = "EU",
        )
}

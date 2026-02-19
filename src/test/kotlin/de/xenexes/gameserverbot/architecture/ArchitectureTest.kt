package de.xenexes.gameserverbot.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.architecture
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture
import com.lemonappdev.konsist.api.architecture.Layer
import com.lemonappdev.konsist.api.ext.list.withAnnotationNamed
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors

class ArchitectureTest {
    private val projectName = "gameserverbot"

    private val scopeFromProduction by lazy {
        Konsist.scopeFromProduction()
    }

    @Test
    fun `package has matching path`() {
        scopeFromProduction
            .packages
            .assertTrue(additionalMessage = "path mismatches package declaration") {
                it.hasMatchingPath
            }
    }

    @Test
    fun `controllers should reside within the api package`() {
        scopeFromProduction
            .classes()
            .withAnnotationNamed("RestController")
            .forEach { controller ->
                controller.assertTrue(
                    additionalMessage = "${controller.name} should reside in the api package",
                ) { it.resideInPackage("..api..") }
            }
    }

    @Test
    fun `repositories should reside within the infrastructure package`() {
        scopeFromProduction
            .classes()
            .withAnnotationNamed("Repository")
            .forEach { repository ->
                repository.assertTrue(
                    additionalMessage = "${repository.name} should reside in the infrastructure package",
                ) { it.resideInPackage("..infrastructure..") }
            }
    }

    @Test
    fun `package dependencies should conform with target architecture`() {
        val api = Layer("api", "..$projectName.api..")
        val domain = Layer("domain", "..$projectName.domain..")
        val infrastructure = Layer("infrastructure", "..$projectName.infrastructure..")
        val ports = Layer("ports", "..$projectName.ports..")
        val usecases = Layer("usecases", "..$projectName.usecases..")

        val arch =
            architecture {
                api.dependsOn(setOf(domain, usecases))
                api.doesNotDependOn(infrastructure)
                domain.doesNotDependOn(setOf(api, infrastructure, ports, usecases))
                infrastructure.dependsOn(setOf(domain, ports, usecases))
                ports.dependsOn(domain)
                ports.doesNotDependOn(setOf(api, infrastructure, usecases))
                usecases.dependsOn(setOf(domain, ports))
                usecases.doesNotDependOn(setOf(api, infrastructure))
            }

        scopeFromProduction.assertArchitecture(arch)
    }

    @Test
    fun `driving adapters should not depend on repositories`() {
        val apiDir =
            Paths
                .get("")
                .toAbsolutePath()
                .resolve("src/main/kotlin/de/xenexes/gameserverbot/api")

        if (!Files.exists(apiDir)) return

        val repositoryReference = Regex("""\bde\.xenexes\.gameserverbot\.ports\.[A-Za-z0-9_]*Repository\b""")
        val violatingFiles =
            Files
                .walk(apiDir)
                .use { paths ->
                    paths
                        .filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                        .filter { repositoryReference.containsMatchIn(Files.readString(it)) }
                        .map { it.toString() }
                        .collect(Collectors.toList())
                }

        assertTrue(
            violatingFiles.isEmpty(),
            "Driving adapters must not depend on repositories. Violations: ${violatingFiles.joinToString()}",
        )
    }

    @Test
    fun `use cases should be annotated with Component`() {
        scopeFromProduction
            .classes()
            .filter { it.name.endsWith("UseCases") }
            .forEach { useCase ->
                useCase.assertTrue(
                    additionalMessage = "${useCase.name} should be annotated with @Component",
                ) { it.hasAnnotationWithName("Component") }
            }
    }

    @Test
    fun `value object IDs in domain should use JvmInline annotation`() {
        scopeFromProduction
            .classes()
            .filter { it.resideInPackage("..domain..") && it.name.endsWith("Id") }
            .forEach { valueObject ->
                valueObject.assertTrue(
                    additionalMessage = "${valueObject.name} should be annotated with @JvmInline",
                ) { it.hasAnnotationWithName("JvmInline") }
            }
    }

    @Test
    fun `domain events should implement DomainEvent or extend sealed event interface`() {
        scopeFromProduction
            .classes()
            .filter {
                it.resideInPackage("..domain.gameserver..") &&
                    it.name.endsWith("Event") &&
                    it.name != "GameServerEvent"
            }.forEach { event ->
                event.assertTrue(
                    additionalMessage = "${event.name} should implement GameServerEvent",
                ) {
                    it.hasParent { parent -> parent.name.contains("GameServerEvent") }
                }
            }

        scopeFromProduction
            .classes()
            .filter {
                it.resideInPackage("..domain.player..") &&
                    it.name.endsWith("Event") &&
                    it.name != "PlayerEvent"
            }.forEach { event ->
                event.assertTrue(
                    additionalMessage = "${event.name} should implement PlayerEvent",
                ) {
                    it.hasParent { parent -> parent.name.contains("PlayerEvent") }
                }
            }

        scopeFromProduction
            .classes()
            .filter {
                it.resideInPackage("..domain.discord..") &&
                    it.hasParent { parent -> parent.name == "DiscordBotEvent" }
            }.forEach { event ->
                event.assertTrue(
                    additionalMessage = "${event.name} should be nested inside DiscordBotEvent sealed interface",
                ) {
                    !it.isTopLevel
                }
            }
    }

    @Test
    fun `domain aggregates should implement the Aggregate interface`() {
        scopeFromProduction
            .classes()
            .filter { it.resideInPackage("..domain..") }
            .filter { cls -> cls.functions().any { it.name == "consumeEvents" } }
            .forEach { aggregate ->
                aggregate.assertTrue(
                    additionalMessage = "${aggregate.name} should implement Aggregate interface",
                ) { it.hasParent { parent -> parent.name.contains("Aggregate") } }
            }
    }

    @Test
    fun `driving adapters should not depend on port interfaces directly`() {
        val apiDir =
            Paths
                .get("")
                .toAbsolutePath()
                .resolve("src/main/kotlin/de/xenexes/gameserverbot/api")

        if (!Files.exists(apiDir)) return

        // Check for port interface imports (Repository, Gateway, Service patterns)
        // Failure types are allowed as they're needed for error mapping in ControllerExceptionHandler
        val portInterfaceReference =
            Regex("""\bde\.xenexes\.gameserverbot\.ports\.[A-Za-z0-9_.]*(?:Repository|Gateway|Service)\b""")
        val violatingFiles =
            Files
                .walk(apiDir)
                .use { paths ->
                    paths
                        .filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                        // Exclude exception handlers which legitimately need failure types for error mapping
                        .filter { !it.toString().contains("ExceptionHandler") }
                        .filter { portInterfaceReference.containsMatchIn(Files.readString(it)) }
                        .map { it.toString() }
                        .collect(Collectors.toList())
                }

        assertTrue(
            violatingFiles.isEmpty(),
            "Driving adapters (api layer) must not depend on port interfaces directly. " +
                "Use cases should mediate access to ports. Violations: ${violatingFiles.joinToString()}",
        )
    }
}

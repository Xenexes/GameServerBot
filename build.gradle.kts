plugins {
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.spring") version "2.3.0"
    kotlin("plugin.serialization") version "2.3.0"
    id("org.springframework.boot") version "4.0.3"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
    id("com.google.cloud.tools.jib") version "3.5.3"
}

group = "de.xenexes"
version = "1.0.0"
description = "GameServerBot"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

val arrowVersion = "2.2.1.1"
// Kord update: Wait until https://github.com/kordlib/kord/issues/1032 is part of a new kord release
val ktorVersion = "3.2.3"
val kotlinLoggingVersion = "8.0.01"
val mockkVersion = "1.14.9"
val konsistVersion = "0.17.3"
val wiremockVersion = "3.13.2"
val assertkVersion = "0.28.1"
val resilience4jVersion = "2.3.0"
val kordVersion = "0.17.0"

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // Jackson for WebFlux (included via spring-boot-starter-webflux)
    implementation("tools.jackson.module:jackson-module-kotlin:3.1.0")

    // Arrow-kt for typed error handling
    implementation("io.arrow-kt:arrow-core:$arrowVersion")

    // Ktor HTTP Client
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")

    // kotlinx.serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:$kotlinLoggingVersion")

    // Caffeine for caching
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.3")

    // Resilience4j for circuit breaker, rate limiter, retry
    implementation("io.github.resilience4j:resilience4j-kotlin:$resilience4jVersion")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:$resilience4jVersion")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:$resilience4jVersion")
    implementation("io.github.resilience4j:resilience4j-retry:$resilience4jVersion")

    // Kord for Discord integration (Kotlin-native)
    implementation("dev.kord:kord-core:$kordVersion")
    implementation("dev.kord:kord-rest:$kordVersion")

    // Development
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // MockK for mocking
    testImplementation("io.mockk:mockk:$mockkVersion")

    // AssertK for fluent assertions
    testImplementation("com.willowtreeapps.assertk:assertk:$assertkVersion")

    // WireMock for HTTP client testing
    testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")

    // Konsist for architecture tests
    testImplementation("com.lemonappdev:konsist:$konsistVersion")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xcontext-parameters",
        )
    }
}

jib {
    from {
        image = "eclipse-temurin:21-jre"
    }
    to {
        image = "${System.getenv("DOCKER_REGISTRY") ?: "localhost"}/game-server-bot:${project.version}"
        tags = setOf("latest")
        auth {
            username = System.getenv("DOCKER_REGISTRY_USERNAME") ?: ""
            password = System.getenv("DOCKER_REGISTRY_PASSWORD") ?: ""
        }
    }
    container {
        jvmFlags = listOf("-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0")
        ports = listOf("8080")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

ktlint {
    filter {
        exclude("**/generated/**")
    }
    version.set("1.5.0") // TODO: remove once https://github.com/JLLeitschuh/ktlint-gradle/issues/816 got resolved
}

package de.xenexes.gameserverbot.infrastructure.http

import de.xenexes.gameserverbot.infrastructure.http.resilience.ResilienceProperties
import de.xenexes.gameserverbot.infrastructure.http.resilience.ResilientHttpClient
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.time.Duration

object HttpClientFactory {
    private val logger = KotlinLogging.logger {}

    fun createHttpClient(
        baseUrl: String,
        callTimeout: Duration = Duration.ofSeconds(30),
        connectTimeout: Duration = Duration.ofSeconds(10),
        additionalConfig: HttpClientConfig<*>.() -> Unit = {},
    ): HttpClient =
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    },
                )
            }

            install(Logging) {
                logger =
                    object : Logger {
                        override fun log(message: String) {
                            this@HttpClientFactory.logger.debug { message }
                        }
                    }
                level = LogLevel.INFO
            }

            install(HttpTimeout) {
                requestTimeoutMillis = callTimeout.toMillis()
                connectTimeoutMillis = connectTimeout.toMillis()
            }

            defaultRequest {
                url(baseUrl)
                contentType(ContentType.Application.Json)
            }

            additionalConfig()
        }

    fun createResilientHttpClient(
        clientName: String,
        baseUrl: String,
        callTimeout: Duration = Duration.ofSeconds(30),
        connectTimeout: Duration = Duration.ofSeconds(10),
        resilienceProperties: ResilienceProperties = ResilienceProperties(),
        additionalConfig: HttpClientConfig<*>.() -> Unit = {},
    ): ResilientHttpClient {
        val httpClient =
            createHttpClient(
                baseUrl = baseUrl,
                callTimeout = callTimeout,
                connectTimeout = connectTimeout,
                additionalConfig = additionalConfig,
            )
        return ResilientHttpClient(
            client = httpClient,
            clientName = clientName,
            properties = resilienceProperties,
        )
    }
}

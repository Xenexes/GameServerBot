package de.xenexes.gameserverbot.infrastructure.http.resilience

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.kotlin.circuitbreaker.executeSuspendFunction
import io.github.resilience4j.kotlin.ratelimiter.executeSuspendFunction
import io.github.resilience4j.kotlin.retry.executeSuspendFunction
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse

class ResilientHttpClient(
    private val client: HttpClient,
    private val clientName: String,
    private val properties: ResilienceProperties = ResilienceProperties(),
) {
    private val logger = KotlinLogging.logger {}

    private val circuitBreaker: CircuitBreaker = createCircuitBreaker()
    private val rateLimiter: RateLimiter = createRateLimiter()
    private val retry: Retry = createRetry()

    suspend fun get(
        urlString: String,
        block: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse = executeWithResilience { client.get(urlString, block) }

    suspend fun post(
        urlString: String,
        block: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse = executeWithResilience { client.post(urlString, block) }

    suspend fun put(
        urlString: String,
        block: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse = executeWithResilience { client.put(urlString, block) }

    suspend fun delete(
        urlString: String,
        block: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse = executeWithResilience { client.delete(urlString, block) }

    private suspend fun <T> executeWithResilience(block: suspend () -> T): T =
        rateLimiter.executeSuspendFunction {
            circuitBreaker.executeSuspendFunction {
                retry.executeSuspendFunction {
                    block()
                }
            }
        }

    private fun createCircuitBreaker(): CircuitBreaker {
        val config =
            CircuitBreakerConfig
                .custom()
                .failureRateThreshold(properties.circuitBreaker.failureRateThreshold)
                .slidingWindowSize(properties.circuitBreaker.slidingWindowSize)
                .minimumNumberOfCalls(properties.circuitBreaker.minimumNumberOfCalls)
                .waitDurationInOpenState(properties.circuitBreaker.waitDurationInOpenState)
                .permittedNumberOfCallsInHalfOpenState(
                    properties.circuitBreaker.permittedNumberOfCallsInHalfOpenState,
                ).recordExceptions(
                    ServerResponseException::class.java,
                    java.io.IOException::class.java,
                ).build()

        return CircuitBreaker.of("$clientName-circuit-breaker", config).also {
            it.eventPublisher.onStateTransition { event ->
                logger.info { "Circuit breaker [$clientName] state transition: ${event.stateTransition}" }
            }
        }
    }

    private fun createRateLimiter(): RateLimiter {
        val config =
            RateLimiterConfig
                .custom()
                .limitForPeriod(properties.rateLimiter.limitForPeriod)
                .limitRefreshPeriod(properties.rateLimiter.limitRefreshPeriod)
                .timeoutDuration(properties.rateLimiter.timeoutDuration)
                .build()

        return RateLimiter.of("$clientName-rate-limiter", config).also {
            it.eventPublisher.onSuccess { event ->
                logger.debug { "Rate limiter [$clientName] permitted: ${event.numberOfPermits}" }
            }
        }
    }

    private fun createRetry(): Retry {
        val config =
            RetryConfig
                .custom<HttpResponse>()
                .maxAttempts(properties.retry.maxAttempts)
                .waitDuration(properties.retry.waitDuration)
                .retryOnException { exception ->
                    when (exception) {
                        is ServerResponseException -> properties.retry.retryOnServerErrors
                        is java.io.IOException -> true
                        else -> false
                    }
                }.retryOnResult { response ->
                    if (properties.retry.retryOnServerErrors) {
                        response.status.value in 500..599
                    } else {
                        false
                    }
                }.build()

        return Retry.of("$clientName-retry", config).also {
            it.eventPublisher.onRetry { event ->
                logger.warn {
                    "Retry [$clientName] attempt ${event.numberOfRetryAttempts}: ${event.lastThrowable?.message}"
                }
            }
        }
    }

    fun close() {
        client.close()
    }
}

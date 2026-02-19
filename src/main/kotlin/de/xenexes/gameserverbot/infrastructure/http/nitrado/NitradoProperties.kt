package de.xenexes.gameserverbot.infrastructure.http.nitrado

import de.xenexes.gameserverbot.infrastructure.http.resilience.CircuitBreakerProperties
import de.xenexes.gameserverbot.infrastructure.http.resilience.RateLimiterProperties
import de.xenexes.gameserverbot.infrastructure.http.resilience.ResilienceProperties
import de.xenexes.gameserverbot.infrastructure.http.resilience.RetryProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import java.time.Duration

@ConfigurationProperties(prefix = "gameserverbot.nitrado")
data class NitradoProperties(
    val baseUrl: String = "https://api.nitrado.net",
    val apiToken: String = "",
    val serverId: String? = null,
    val callTimeout: Duration = Duration.ofSeconds(30),
    val connectTimeout: Duration = Duration.ofSeconds(10),
    @NestedConfigurationProperty
    val resilience: ResilienceConfig = ResilienceConfig(),
    @NestedConfigurationProperty
    val cache: CacheConfig = CacheConfig(),
) {
    fun toResilienceProperties(): ResilienceProperties =
        ResilienceProperties(
            circuitBreaker =
                CircuitBreakerProperties(
                    failureRateThreshold = resilience.circuitBreaker.failureRateThreshold,
                    slidingWindowSize = resilience.circuitBreaker.slidingWindowSize,
                    minimumNumberOfCalls = resilience.circuitBreaker.minimumNumberOfCalls,
                    waitDurationInOpenState = resilience.circuitBreaker.waitDurationInOpenState,
                    permittedNumberOfCallsInHalfOpenState =
                        resilience.circuitBreaker.permittedNumberOfCallsInHalfOpenState,
                ),
            rateLimiter =
                RateLimiterProperties(
                    limitForPeriod = resilience.rateLimiter.limitForPeriod,
                    limitRefreshPeriod = resilience.rateLimiter.limitRefreshPeriod,
                    timeoutDuration = resilience.rateLimiter.timeoutDuration,
                ),
            retry =
                RetryProperties(
                    maxAttempts = resilience.retry.maxAttempts,
                    waitDuration = resilience.retry.waitDuration,
                    retryOnServerErrors = resilience.retry.retryOnServerErrors,
                ),
        )
}

data class ResilienceConfig(
    @NestedConfigurationProperty
    val circuitBreaker: CircuitBreakerConfig = CircuitBreakerConfig(),
    @NestedConfigurationProperty
    val rateLimiter: RateLimiterConfig = RateLimiterConfig(),
    @NestedConfigurationProperty
    val retry: RetryConfig = RetryConfig(),
)

data class CircuitBreakerConfig(
    val failureRateThreshold: Float = 50f,
    val slidingWindowSize: Int = 10,
    val minimumNumberOfCalls: Int = 5,
    val waitDurationInOpenState: Duration = Duration.ofSeconds(30),
    val permittedNumberOfCallsInHalfOpenState: Int = 3,
)

data class RateLimiterConfig(
    val limitForPeriod: Int = 10,
    val limitRefreshPeriod: Duration = Duration.ofSeconds(1),
    val timeoutDuration: Duration = Duration.ofSeconds(5),
)

data class RetryConfig(
    val maxAttempts: Int = 3,
    val waitDuration: Duration = Duration.ofMillis(500),
    val retryOnServerErrors: Boolean = true,
)

data class CacheConfig(
    val serverStatusTtl: Duration = Duration.ofSeconds(30),
    val servicesTtl: Duration = Duration.ofMinutes(5),
    val gameListTtl: Duration = Duration.ofMinutes(5),
    val playersTtl: Duration = Duration.ofSeconds(30),
    val playerListTtl: Duration = Duration.ofMinutes(1),
    val maximumSize: Long = 100,
)

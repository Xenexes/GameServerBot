package de.xenexes.gameserverbot.infrastructure.http.resilience

import java.time.Duration

data class CircuitBreakerProperties(
    val failureRateThreshold: Float = 50f,
    val slidingWindowSize: Int = 10,
    val minimumNumberOfCalls: Int = 5,
    val waitDurationInOpenState: Duration = Duration.ofSeconds(30),
    val permittedNumberOfCallsInHalfOpenState: Int = 3,
)

data class RateLimiterProperties(
    val limitForPeriod: Int = 10,
    val limitRefreshPeriod: Duration = Duration.ofSeconds(1),
    val timeoutDuration: Duration = Duration.ofSeconds(5),
)

data class RetryProperties(
    val maxAttempts: Int = 3,
    val waitDuration: Duration = Duration.ofMillis(500),
    val retryOnServerErrors: Boolean = true,
)

data class ResilienceProperties(
    val circuitBreaker: CircuitBreakerProperties = CircuitBreakerProperties(),
    val rateLimiter: RateLimiterProperties = RateLimiterProperties(),
    val retry: RetryProperties = RetryProperties(),
)

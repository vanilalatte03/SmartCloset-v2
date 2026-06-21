package com.smartcloset.weather.infrastructure.kma;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * KMA provider 호출에 Resilience4j retry와 process-local circuit breaker를 적용한다.
 */
final class KmaProviderResilience {

    private final Retry retry;
    private final CircuitBreaker circuitBreaker;

    KmaProviderResilience(KmaWeatherProperties properties) {
        this(
                properties.maxAttempts(),
                properties.retryBackoff(),
                properties.circuitBreakerFailureThreshold(),
                properties.circuitBreakerOpenDuration(),
                Clock.systemUTC()
        );
    }

    KmaProviderResilience(
            int maxAttempts,
            Duration retryBackoff,
            int failureThreshold,
            Duration openDuration,
            Clock clock
    ) {
        Objects.requireNonNull(clock, "clock must not be null");
        int resolvedMaxAttempts = Math.max(1, maxAttempts);
        int resolvedFailureThreshold = Math.max(1, failureThreshold);
        Duration resolvedRetryBackoff = nonNegative(retryBackoff, "retryBackoff");
        Duration resolvedOpenDuration = positive(openDuration, "openDuration");
        retry = Retry.of("kma-vilage-forecast", RetryConfig.custom()
                .maxAttempts(resolvedMaxAttempts)
                .waitDuration(resolvedRetryBackoff)
                .retryExceptions(KmaForecastClientException.class)
                .build());
        circuitBreaker = CircuitBreaker.of("kma-vilage-forecast", CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(resolvedFailureThreshold)
                .minimumNumberOfCalls(resolvedFailureThreshold)
                .failureRateThreshold(100.0f)
                .waitDurationInOpenState(resolvedOpenDuration)
                .permittedNumberOfCallsInHalfOpenState(1)
                .recordExceptions(KmaForecastClientException.class)
                .build());
    }

    <T> T execute(Supplier<T> operation) {
        Objects.requireNonNull(operation, "operation must not be null");
        Supplier<T> retried = Retry.decorateSupplier(retry, operation);
        Supplier<T> protectedCall = CircuitBreaker.decorateSupplier(circuitBreaker, retried);
        try {
            return protectedCall.get();
        } catch (CallNotPermittedException exception) {
            throw new KmaForecastClientException("KMA forecast circuit breaker is open", exception);
        }
    }

    private Duration nonNegative(Duration value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isNegative()) {
            throw new IllegalArgumentException("KMA " + name + " must not be negative");
        }
        return value;
    }

    private Duration positive(Duration value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("KMA " + name + " must be positive");
        }
        return value;
    }
}

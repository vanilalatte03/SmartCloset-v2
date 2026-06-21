package com.smartcloset.clothing.infrastructure.analysis;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * OpenAI 옷 분석 provider 호출에 Resilience4j retry와 process-local circuit breaker를 적용한다.
 */
final class ClothingAnalysisProviderResilience {

    private final Retry retry;
    private final CircuitBreaker circuitBreaker;

    ClothingAnalysisProviderResilience(ClothingAnalysisProperties properties) {
        this(
                properties.maxAttempts(),
                properties.retryBackoff(),
                properties.circuitBreakerFailureThreshold(),
                properties.circuitBreakerOpenDuration()
        );
    }

    ClothingAnalysisProviderResilience(
            int maxAttempts,
            Duration retryBackoff,
            int failureThreshold,
            Duration openDuration
    ) {
        int resolvedMaxAttempts = Math.max(1, maxAttempts);
        int resolvedFailureThreshold = Math.max(1, failureThreshold);
        Duration resolvedRetryBackoff = nonNegative(retryBackoff, "retryBackoff");
        Duration resolvedOpenDuration = positive(openDuration, "openDuration");
        retry = Retry.of("clothing-analysis-openai", RetryConfig.custom()
                .maxAttempts(resolvedMaxAttempts)
                .waitDuration(resolvedRetryBackoff)
                .retryExceptions(ClothingImageAnalysisUnavailableException.class)
                .build());
        circuitBreaker = CircuitBreaker.of("clothing-analysis-openai", CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(resolvedFailureThreshold)
                .minimumNumberOfCalls(resolvedFailureThreshold)
                .failureRateThreshold(100.0f)
                .waitDurationInOpenState(resolvedOpenDuration)
                .permittedNumberOfCallsInHalfOpenState(1)
                .recordExceptions(ClothingImageAnalysisUnavailableException.class)
                .build());
    }

    <T> T execute(Supplier<T> operation) {
        Objects.requireNonNull(operation, "operation must not be null");
        Supplier<T> retried = Retry.decorateSupplier(retry, operation);
        Supplier<T> protectedCall = CircuitBreaker.decorateSupplier(circuitBreaker, retried);
        try {
            return protectedCall.get();
        } catch (CallNotPermittedException exception) {
            throw new ClothingImageAnalysisUnavailableException(
                    "Clothing image analysis circuit breaker is open",
                    exception
            );
        }
    }

    private Duration nonNegative(Duration value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isNegative()) {
            throw new IllegalArgumentException("Clothing analysis " + name + " must not be negative");
        }
        return value;
    }

    private Duration positive(Duration value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("Clothing analysis " + name + " must be positive");
        }
        return value;
    }
}

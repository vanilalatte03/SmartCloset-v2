package com.smartcloset.clothing.infrastructure.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ClothingAnalysisPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ClothingAnalysisPropertiesConfig.class);

    @Test
    void usesDocumentedDisabledDefaults() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ClothingAnalysisProperties.class);

            ClothingAnalysisProperties properties = context.getBean(ClothingAnalysisProperties.class);

            assertThat(properties.enabled()).isFalse();
            assertThat(properties.lowConfidenceThreshold())
                    .isEqualTo(ClothingAnalysisProperties.DEFAULT_LOW_CONFIDENCE_THRESHOLD);
            assertThat(properties.dailyLimit()).isEqualTo(ClothingAnalysisProperties.DEFAULT_DAILY_LIMIT);
            assertThat(properties.timeoutSeconds()).isEqualTo(ClothingAnalysisProperties.DEFAULT_TIMEOUT_SECONDS);
            assertThat(properties.maxAttempts()).isEqualTo(ClothingAnalysisProperties.DEFAULT_MAX_ATTEMPTS);
            assertThat(properties.retryBackoff()).isEqualTo(ClothingAnalysisProperties.DEFAULT_RETRY_BACKOFF);
            assertThat(properties.circuitBreakerFailureThreshold())
                    .isEqualTo(ClothingAnalysisProperties.DEFAULT_CIRCUIT_BREAKER_FAILURE_THRESHOLD);
            assertThat(properties.circuitBreakerOpenDuration())
                    .isEqualTo(ClothingAnalysisProperties.DEFAULT_CIRCUIT_BREAKER_OPEN_DURATION);
        });
    }

    @Test
    void bindsConfiguredValues() {
        contextRunner
                .withPropertyValues(
                        "smartcloset.clothing.analysis.enabled=true",
                        "smartcloset.clothing.analysis.low-confidence-threshold=0.6",
                        "smartcloset.clothing.analysis.daily-limit=3",
                        "smartcloset.clothing.analysis.timeout-seconds=5",
                        "smartcloset.clothing.analysis.max-attempts=4",
                        "smartcloset.clothing.analysis.retry-backoff=50ms",
                        "smartcloset.clothing.analysis.circuit-breaker-failure-threshold=6",
                        "smartcloset.clothing.analysis.circuit-breaker-open-duration=45s"
                )
                .run(context -> {
                    ClothingAnalysisProperties properties = context.getBean(ClothingAnalysisProperties.class);

                    assertThat(properties.enabled()).isTrue();
                    assertThat(properties.lowConfidenceThreshold()).isEqualTo(0.6);
                    assertThat(properties.dailyLimit()).isEqualTo(3);
                    assertThat(properties.timeoutSeconds()).isEqualTo(5);
                    assertThat(properties.maxAttempts()).isEqualTo(4);
                    assertThat(properties.retryBackoff()).isEqualTo(java.time.Duration.ofMillis(50));
                    assertThat(properties.circuitBreakerFailureThreshold()).isEqualTo(6);
                    assertThat(properties.circuitBreakerOpenDuration()).isEqualTo(java.time.Duration.ofSeconds(45));
                });
    }
}

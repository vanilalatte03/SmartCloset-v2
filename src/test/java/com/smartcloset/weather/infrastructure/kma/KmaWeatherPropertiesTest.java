package com.smartcloset.weather.infrastructure.kma;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class KmaWeatherPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(KmaWeatherPropertiesConfig.class);

    @Test
    void usesDocumentedDefaults() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(KmaWeatherProperties.class);

            KmaWeatherProperties properties = context.getBean(KmaWeatherProperties.class);

            assertThat(properties.serviceKey()).isEmpty();
            assertThat(properties.nx()).isEqualTo(60);
            assertThat(properties.ny()).isEqualTo(127);
            assertThat(properties.baseUrl()).isEqualTo(KmaWeatherProperties.DEFAULT_BASE_URL);
            assertThat(properties.cacheTtl()).isEqualTo(Duration.ofMinutes(2));
            assertThat(properties.cacheMaxSize()).isEqualTo(256);
            assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(2));
            assertThat(properties.readTimeout()).isEqualTo(Duration.ofSeconds(3));
            assertThat(properties.requestTimeout()).isEqualTo(Duration.ofSeconds(5));
            assertThat(properties.staleCacheTtl()).isEqualTo(Duration.ofMinutes(30));
            assertThat(properties.maxAttempts()).isEqualTo(2);
            assertThat(properties.retryBackoff()).isEqualTo(Duration.ofMillis(200));
            assertThat(properties.circuitBreakerFailureThreshold()).isEqualTo(3);
            assertThat(properties.circuitBreakerOpenDuration()).isEqualTo(Duration.ofSeconds(30));
            assertThat(properties.fallbackEnabled()).isTrue();
        });
    }

    @Test
    void bindsConfiguredValues() {
        contextRunner
                .withPropertyValues(
                        "smartcloset.weather.kma.service-key=test-service-key",
                        "smartcloset.weather.kma.nx=61",
                        "smartcloset.weather.kma.ny=128",
                        "smartcloset.weather.kma.base-url=http://example.test/kma",
                        "smartcloset.weather.kma.cache-ttl=5m",
                        "smartcloset.weather.kma.cache-max-size=512",
                        "smartcloset.weather.kma.connect-timeout=750ms",
                        "smartcloset.weather.kma.read-timeout=2s",
                        "smartcloset.weather.kma.request-timeout=4s",
                        "smartcloset.weather.kma.stale-cache-ttl=10m",
                        "smartcloset.weather.kma.max-attempts=3",
                        "smartcloset.weather.kma.retry-backoff=25ms",
                        "smartcloset.weather.kma.circuit-breaker-failure-threshold=5",
                        "smartcloset.weather.kma.circuit-breaker-open-duration=45s",
                        "smartcloset.weather.fallback-enabled=false"
                )
                .run(context -> {
                    KmaWeatherProperties properties = context.getBean(KmaWeatherProperties.class);

                    assertThat(properties.serviceKey()).isEqualTo("test-service-key");
                    assertThat(properties.nx()).isEqualTo(61);
                    assertThat(properties.ny()).isEqualTo(128);
                    assertThat(properties.baseUrl()).isEqualTo("http://example.test/kma");
                    assertThat(properties.cacheTtl()).isEqualTo(Duration.ofMinutes(5));
                    assertThat(properties.cacheMaxSize()).isEqualTo(512);
                    assertThat(properties.connectTimeout()).isEqualTo(Duration.ofMillis(750));
                    assertThat(properties.readTimeout()).isEqualTo(Duration.ofSeconds(2));
                    assertThat(properties.requestTimeout()).isEqualTo(Duration.ofSeconds(4));
                    assertThat(properties.staleCacheTtl()).isEqualTo(Duration.ofMinutes(10));
                    assertThat(properties.maxAttempts()).isEqualTo(3);
                    assertThat(properties.retryBackoff()).isEqualTo(Duration.ofMillis(25));
                    assertThat(properties.circuitBreakerFailureThreshold()).isEqualTo(5);
                    assertThat(properties.circuitBreakerOpenDuration()).isEqualTo(Duration.ofSeconds(45));
                    assertThat(properties.fallbackEnabled()).isFalse();
                });
    }
}

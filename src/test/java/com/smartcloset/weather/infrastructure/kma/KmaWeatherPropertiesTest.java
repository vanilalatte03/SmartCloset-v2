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
                    assertThat(properties.fallbackEnabled()).isFalse();
                });
    }
}

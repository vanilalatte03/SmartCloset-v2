package com.smartcloset.weather.infrastructure.kma;

import static org.assertj.core.api.Assertions.assertThat;

import com.smartcloset.weather.application.WeatherProvider;
import com.smartcloset.weather.infrastructure.StaticWeatherProvider;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "KMA_SERVICE_KEY=",
        "KMA_NX=61",
        "KMA_NY=128",
        "KMA_BASE_URL=http://example.test/kma",
        "WEATHER_FALLBACK_ENABLED=false"
})
class KmaWeatherPropertiesApplicationContextTest {

    @Autowired
    private KmaWeatherProperties properties;

    @Autowired
    private List<WeatherProvider> weatherProviders;

    @Test
    void bindsEnvironmentVariablesThroughApplicationYaml() {
        assertThat(properties.serviceKey()).isEmpty();
        assertThat(properties.nx()).isEqualTo(61);
        assertThat(properties.ny()).isEqualTo(128);
        assertThat(properties.baseUrl()).isEqualTo("http://example.test/kma");
        assertThat(properties.fallbackEnabled()).isFalse();
    }

    @Test
    void keepsStaticWeatherProviderAsOnlyWeatherProvider() {
        assertThat(weatherProviders).hasSize(1);
        assertThat(weatherProviders.getFirst()).isInstanceOf(StaticWeatherProvider.class);
    }
}

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
    private WeatherProvider weatherProvider;

    @Autowired
    private StaticWeatherProvider staticWeatherProvider;

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
    void resolvesPrimaryWeatherProviderToKmaProviderAndKeepsStaticFallbackProvider() {
        assertThat(weatherProvider).isInstanceOf(KmaVilageForecastWeatherProvider.class);
        assertThat(staticWeatherProvider).isNotNull();
        assertThat(weatherProviders).hasSize(2);
        assertThat(weatherProviders)
                .anySatisfy(provider -> assertThat(provider).isInstanceOf(KmaVilageForecastWeatherProvider.class))
                .anySatisfy(provider -> assertThat(provider).isInstanceOf(StaticWeatherProvider.class));
    }
}

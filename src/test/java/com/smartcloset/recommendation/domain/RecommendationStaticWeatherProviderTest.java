package com.smartcloset.recommendation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.smartcloset.weather.domain.WeatherSnapshot;
import com.smartcloset.weather.domain.WeatherType;
import com.smartcloset.weather.infrastructure.StaticWeatherProvider;
import org.junit.jupiter.api.Test;

class RecommendationStaticWeatherProviderTest {

    @Test
    void returnsConfiguredMvpWeather() {
        WeatherSnapshot weather = new StaticWeatherProvider().getCurrentWeather(1L);

        assertThat(weather.condition().temperature()).isEqualTo(12);
        assertThat(weather.condition().weatherType()).isEqualTo(WeatherType.CLOUDY);
        assertThat(weather.condition().rainy()).isFalse();
        assertThat(weather.condition().windy()).isFalse();
        assertThat(weather.source().fallbackUsed()).isTrue();
    }
}

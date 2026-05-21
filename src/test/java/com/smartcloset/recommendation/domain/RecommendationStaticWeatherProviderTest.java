package com.smartcloset.recommendation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.smartcloset.weather.domain.WeatherCondition;
import com.smartcloset.weather.domain.WeatherType;
import com.smartcloset.weather.infrastructure.StaticWeatherProvider;
import org.junit.jupiter.api.Test;

class RecommendationStaticWeatherProviderTest {

    @Test
    void returnsConfiguredMvpWeather() {
        WeatherCondition weather = new StaticWeatherProvider().getCurrentWeather(1L);

        assertThat(weather.temperature()).isEqualTo(12);
        assertThat(weather.weatherType()).isEqualTo(WeatherType.CLOUDY);
        assertThat(weather.rainy()).isFalse();
        assertThat(weather.windy()).isFalse();
    }
}

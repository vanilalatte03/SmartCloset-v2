package com.smartcloset.weather.infrastructure;

import com.smartcloset.weather.application.WeatherProvider;
import com.smartcloset.weather.domain.WeatherCondition;
import com.smartcloset.weather.domain.WeatherType;
import org.springframework.stereotype.Component;

@Component
public class StaticWeatherProvider implements WeatherProvider {

    @Override
    public WeatherCondition getCurrentWeather(Long userId) {
        return WeatherCondition.of(12, WeatherType.CLOUDY, false, false);
    }
}

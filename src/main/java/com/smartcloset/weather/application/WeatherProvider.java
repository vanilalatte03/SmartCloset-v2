package com.smartcloset.weather.application;

import com.smartcloset.weather.domain.ForecastPeriod;
import com.smartcloset.weather.domain.WeatherSnapshot;

public interface WeatherProvider {

    WeatherSnapshot getWeather(Long userId, ForecastPeriod forecastPeriod);

    default WeatherSnapshot getCurrentWeather(Long userId) {
        return getWeather(userId, ForecastPeriod.CURRENT);
    }
}

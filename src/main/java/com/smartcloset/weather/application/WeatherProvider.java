package com.smartcloset.weather.application;

import com.smartcloset.weather.domain.WeatherCondition;

public interface WeatherProvider {

    WeatherCondition getCurrentWeather(Long userId);
}

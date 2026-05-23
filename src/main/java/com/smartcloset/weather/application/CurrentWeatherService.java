package com.smartcloset.weather.application;

import com.smartcloset.recommendation.dto.WeatherResponse;
import com.smartcloset.weather.domain.WeatherCondition;
import org.springframework.stereotype.Service;

@Service
public class CurrentWeatherService {

    private final WeatherProvider weatherProvider;

    public CurrentWeatherService(WeatherProvider weatherProvider) {
        this.weatherProvider = weatherProvider;
    }

    public WeatherResponse getCurrentWeather(Long userId) {
        WeatherCondition weather = weatherProvider.getCurrentWeather(userId);
        return WeatherResponse.from(weather);
    }
}

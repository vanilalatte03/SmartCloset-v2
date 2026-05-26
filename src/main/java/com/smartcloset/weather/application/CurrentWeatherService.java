package com.smartcloset.weather.application;

import com.smartcloset.weather.domain.WeatherSnapshot;
import com.smartcloset.weather.dto.WeatherResponse;
import org.springframework.stereotype.Service;

@Service
public class CurrentWeatherService {

    private final WeatherProvider weatherProvider;

    public CurrentWeatherService(WeatherProvider weatherProvider) {
        this.weatherProvider = weatherProvider;
    }

    public WeatherResponse getCurrentWeather(Long userId) {
        WeatherSnapshot weather = weatherProvider.getCurrentWeather(userId);
        return WeatherResponse.from(weather);
    }
}

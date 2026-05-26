package com.smartcloset.weather.infrastructure.kma;

import com.smartcloset.weather.domain.WeatherCondition;
import java.util.Objects;

record KmaMappedWeather(
        WeatherCondition condition,
        String forecastDate,
        String forecastTime
) {

    KmaMappedWeather {
        Objects.requireNonNull(condition, "condition must not be null");
        forecastDate = requireText(forecastDate, "forecastDate");
        forecastTime = requireText(forecastTime, "forecastTime");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}

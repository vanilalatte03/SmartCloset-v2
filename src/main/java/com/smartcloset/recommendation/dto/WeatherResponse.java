package com.smartcloset.recommendation.dto;

import com.smartcloset.recommendation.domain.RecommendationResult;
import com.smartcloset.weather.domain.WeatherCondition;
import com.smartcloset.weather.domain.WeatherType;

public record WeatherResponse(
        int temperature,
        WeatherType weatherType,
        boolean rainy,
        boolean windy
) {

    public static WeatherResponse from(WeatherCondition weather) {
        return new WeatherResponse(
                weather.temperature(),
                weather.weatherType(),
                weather.rainy(),
                weather.windy()
        );
    }

    public static WeatherResponse from(RecommendationResult recommendationResult) {
        return new WeatherResponse(
                recommendationResult.getWeatherTemperature(),
                recommendationResult.getWeatherType(),
                recommendationResult.isRainy(),
                recommendationResult.isWindy()
        );
    }
}

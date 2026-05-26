package com.smartcloset.recommendation.dto;

import com.smartcloset.recommendation.domain.RecommendationResult;
import com.smartcloset.weather.domain.WeatherType;

public record WeatherResponse(
        int temperature,
        WeatherType weatherType,
        boolean rainy,
        boolean windy
) {

    public static WeatherResponse from(RecommendationResult recommendationResult) {
        return new WeatherResponse(
                recommendationResult.getWeatherTemperature(),
                recommendationResult.getWeatherType(),
                recommendationResult.isRainy(),
                recommendationResult.isWindy()
        );
    }
}

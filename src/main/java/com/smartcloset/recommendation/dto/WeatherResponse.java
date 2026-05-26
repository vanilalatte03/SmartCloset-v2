package com.smartcloset.recommendation.dto;

import com.smartcloset.recommendation.domain.RecommendationResult;
import com.smartcloset.weather.domain.WeatherCondition;
import com.smartcloset.weather.domain.WeatherSnapshot;
import com.smartcloset.weather.domain.WeatherType;

public record WeatherResponse(
        int temperature,
        WeatherType weatherType,
        boolean rainy,
        boolean windy,
        WeatherLocationSnapshotResponse location,
        WeatherSourceResponse source
) {

    public static WeatherResponse from(WeatherSnapshot weather) {
        return new WeatherResponse(
                weather.condition().temperature(),
                weather.condition().weatherType(),
                weather.condition().rainy(),
                weather.condition().windy(),
                WeatherLocationSnapshotResponse.from(weather.location()),
                WeatherSourceResponse.from(weather.source())
        );
    }

    public static WeatherResponse from(RecommendationResult recommendationResult) {
        return new WeatherResponse(
                recommendationResult.getWeatherTemperature(),
                recommendationResult.getWeatherType(),
                recommendationResult.isRainy(),
                recommendationResult.isWindy(),
                null,
                null
        );
    }
}

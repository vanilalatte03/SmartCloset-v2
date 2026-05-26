package com.smartcloset.recommendation.dto;

import com.smartcloset.recommendation.domain.RecommendationResult;
import com.smartcloset.weather.dto.WeatherLocationSnapshotResponse;
import com.smartcloset.weather.dto.WeatherSourceResponse;
import com.smartcloset.weather.domain.WeatherType;

public record WeatherResponse(
        int temperature,
        WeatherType weatherType,
        boolean rainy,
        boolean windy,
        WeatherLocationSnapshotResponse location,
        WeatherSourceResponse source
) {

    public static WeatherResponse from(RecommendationResult recommendationResult) {
        return new WeatherResponse(
                recommendationResult.getWeatherTemperature(),
                recommendationResult.getWeatherType(),
                recommendationResult.isRainy(),
                recommendationResult.isWindy(),
                new WeatherLocationSnapshotResponse(
                        recommendationResult.getWeatherLocationCode(),
                        recommendationResult.getWeatherLocationName(),
                        recommendationResult.getWeatherLocationFullName(),
                        recommendationResult.getWeatherLocationNx(),
                        recommendationResult.getWeatherLocationNy(),
                        recommendationResult.getWeatherLocationSource()
                ),
                new WeatherSourceResponse(
                        recommendationResult.getWeatherProvider(),
                        recommendationResult.isWeatherKmaUsed(),
                        recommendationResult.isWeatherFallbackUsed(),
                        recommendationResult.getWeatherBaseDate(),
                        recommendationResult.getWeatherBaseTime(),
                        recommendationResult.getWeatherForecastDate(),
                        recommendationResult.getWeatherForecastTime()
                )
        );
    }
}

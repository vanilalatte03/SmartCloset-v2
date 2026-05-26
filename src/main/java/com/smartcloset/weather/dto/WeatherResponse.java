package com.smartcloset.weather.dto;

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
}

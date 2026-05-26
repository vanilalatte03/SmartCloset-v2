package com.smartcloset.weather.domain;

import java.util.Objects;

public record WeatherSnapshot(
        WeatherCondition condition,
        WeatherLocationSnapshot location,
        WeatherSource source
) {

    public WeatherSnapshot {
        Objects.requireNonNull(condition, "condition must not be null");
        Objects.requireNonNull(location, "location must not be null");
        Objects.requireNonNull(source, "source must not be null");
    }
}

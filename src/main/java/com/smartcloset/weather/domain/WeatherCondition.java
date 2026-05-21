package com.smartcloset.weather.domain;

import java.util.Objects;

public record WeatherCondition(int temperature, WeatherType weatherType, boolean rainy, boolean windy) {

    public WeatherCondition {
        Objects.requireNonNull(weatherType, "weatherType must not be null");
    }

    public static WeatherCondition of(int temperature, WeatherType weatherType, boolean rainy, boolean windy) {
        return new WeatherCondition(temperature, weatherType, rainy, windy);
    }
}

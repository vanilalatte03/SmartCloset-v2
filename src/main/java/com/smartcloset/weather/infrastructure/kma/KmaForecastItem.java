package com.smartcloset.weather.infrastructure.kma;

import java.util.Objects;

public record KmaForecastItem(String fcstDate, String fcstTime, String category, String fcstValue) {

    public KmaForecastItem {
        Objects.requireNonNull(fcstDate, "fcstDate must not be null");
        Objects.requireNonNull(fcstTime, "fcstTime must not be null");
        Objects.requireNonNull(category, "category must not be null");
    }
}

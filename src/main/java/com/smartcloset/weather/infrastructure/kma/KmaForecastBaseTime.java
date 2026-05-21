package com.smartcloset.weather.infrastructure.kma;

import java.util.Objects;

public record KmaForecastBaseTime(String baseDate, String baseTime) {

    public KmaForecastBaseTime {
        Objects.requireNonNull(baseDate, "baseDate must not be null");
        Objects.requireNonNull(baseTime, "baseTime must not be null");
    }
}

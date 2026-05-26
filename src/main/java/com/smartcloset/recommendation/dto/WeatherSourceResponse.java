package com.smartcloset.recommendation.dto;

import com.smartcloset.weather.domain.WeatherProviderType;
import com.smartcloset.weather.domain.WeatherSource;

public record WeatherSourceResponse(
        WeatherProviderType provider,
        boolean kmaUsed,
        boolean fallbackUsed,
        String baseDate,
        String baseTime,
        String forecastDate,
        String forecastTime
) {

    public static WeatherSourceResponse from(WeatherSource source) {
        if (source == null) {
            return null;
        }
        return new WeatherSourceResponse(
                source.provider(),
                source.kmaUsed(),
                source.fallbackUsed(),
                source.baseDate(),
                source.baseTime(),
                source.forecastDate(),
                source.forecastTime()
        );
    }
}

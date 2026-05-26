package com.smartcloset.weather.domain;

import java.util.Objects;

public record WeatherSource(
        WeatherProviderType provider,
        boolean kmaUsed,
        boolean fallbackUsed,
        String baseDate,
        String baseTime,
        String forecastDate,
        String forecastTime
) {

    public WeatherSource {
        Objects.requireNonNull(provider, "provider must not be null");
        if (kmaUsed == fallbackUsed) {
            throw new IllegalArgumentException("exactly one of kmaUsed and fallbackUsed must be true");
        }
        baseDate = normalizeNullableText(baseDate);
        baseTime = normalizeNullableText(baseTime);
        forecastDate = normalizeNullableText(forecastDate);
        forecastTime = normalizeNullableText(forecastTime);
    }

    public static WeatherSource kma(
            String baseDate,
            String baseTime,
            String forecastDate,
            String forecastTime
    ) {
        return new WeatherSource(
                WeatherProviderType.KMA_VILAGE_FORECAST,
                true,
                false,
                baseDate,
                baseTime,
                forecastDate,
                forecastTime
        );
    }

    public static WeatherSource fallback(String baseDate, String baseTime) {
        return fallback(baseDate, baseTime, null, null);
    }

    public static WeatherSource fallback(
            String baseDate,
            String baseTime,
            String forecastDate,
            String forecastTime
    ) {
        return new WeatherSource(
                WeatherProviderType.STATIC_FALLBACK,
                false,
                true,
                baseDate,
                baseTime,
                forecastDate,
                forecastTime
        );
    }

    private static String normalizeNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}

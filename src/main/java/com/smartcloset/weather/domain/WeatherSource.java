package com.smartcloset.weather.domain;

import java.util.Objects;

/**
 * 날씨 값이 KMA에서 왔는지 fallback에서 왔는지와 KMA base/forecast 시각을 표현한다.
 *
 * <p>raw KMA JSON은 저장하지 않고, 사용자에게 신뢰도를 설명하는 데 필요한 최소 metadata만 보관한다.</p>
 */
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

    /**
     * KMA getVilageFcst에서 온 날씨 source metadata를 생성한다.
     */
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

    /**
     * base 시각만 알 수 있는 fallback 날씨 source metadata를 생성한다.
     */
    public static WeatherSource fallback(String baseDate, String baseTime) {
        return fallback(baseDate, baseTime, null, null);
    }

    /**
     * forecast 시각까지 포함한 fallback 날씨 source metadata를 생성한다.
     */
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

package com.smartcloset.weather.domain;

import java.util.Objects;

/**
 * 추천 점수 계산에 필요한 최소 날씨 조건이다.
 */
public record WeatherCondition(int temperature, WeatherType weatherType, boolean rainy, boolean windy) {

    public WeatherCondition {
        Objects.requireNonNull(weatherType, "weatherType must not be null");
    }

    /**
     * 추천 규칙에서 사용하는 기온, 날씨 유형, 비/바람 flag를 하나의 조건으로 묶는다.
     */
    public static WeatherCondition of(int temperature, WeatherType weatherType, boolean rainy, boolean windy) {
        return new WeatherCondition(temperature, weatherType, rainy, windy);
    }
}

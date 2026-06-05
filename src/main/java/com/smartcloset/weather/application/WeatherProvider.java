package com.smartcloset.weather.application;

import com.smartcloset.weather.domain.ForecastPeriod;
import com.smartcloset.weather.domain.WeatherSnapshot;

/**
 * 추천과 현재 날씨 조회가 의존하는 날씨 provider boundary다.
 *
 * <p>구현체는 외부 KMA provider와 local fallback provider로 나뉘지만, caller는 userId와 forecast period만 넘긴다.</p>
 */
public interface WeatherProvider {

    /**
     * 사용자 저장 위치와 요청 예보 시간대를 기준으로 날씨 snapshot을 조회한다.
     */
    WeatherSnapshot getWeather(Long userId, ForecastPeriod forecastPeriod);

    /**
     * 현재 날씨 조회 API에서 사용하는 CURRENT forecast period shortcut이다.
     */
    default WeatherSnapshot getCurrentWeather(Long userId) {
        return getWeather(userId, ForecastPeriod.CURRENT);
    }
}

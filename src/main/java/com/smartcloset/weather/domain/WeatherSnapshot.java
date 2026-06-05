package com.smartcloset.weather.domain;

import java.util.Objects;

/**
 * 추천과 날씨 조회가 함께 사용하는 날씨 snapshot이다.
 *
 * <p>조건값, 사용자 위치, provider source를 같이 묶어 저장하면 과거 추천 이력이 현재 설정 변화에
 * 영향을 받지 않는다.</p>
 */
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

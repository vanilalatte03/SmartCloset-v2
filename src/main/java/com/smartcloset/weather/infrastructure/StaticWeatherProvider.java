package com.smartcloset.weather.infrastructure;

import com.smartcloset.weather.application.WeatherProvider;
import com.smartcloset.weather.domain.ForecastPeriod;
import com.smartcloset.weather.domain.WeatherCondition;
import com.smartcloset.weather.domain.WeatherLocationSnapshot;
import com.smartcloset.weather.domain.WeatherSnapshot;
import com.smartcloset.weather.domain.WeatherSource;
import com.smartcloset.weather.domain.WeatherType;
import com.smartcloset.location.domain.LocationSource;
import org.springframework.stereotype.Component;

/**
 * KMA provider를 사용할 수 없거나 테스트에서 고정 날씨가 필요할 때 쓰는 fallback weather provider다.
 *
 * <p>Fallback 값은 recommendation/weather 계약의 기본 흐림 날씨 snapshot을 유지한다.</p>
 */
@Component
public class StaticWeatherProvider implements WeatherProvider {

    /**
     * KMA를 사용할 수 없을 때 계약상 기본 fallback 날씨와 서울 위치 snapshot을 반환한다.
     */
    @Override
    public WeatherSnapshot getWeather(Long userId, ForecastPeriod forecastPeriod) {
        WeatherCondition condition = WeatherCondition.of(12, WeatherType.CLOUDY, false, false);
        WeatherLocationSnapshot location = new WeatherLocationSnapshot(
                "SEOUL",
                "서울특별시",
                "서울특별시",
                60,
                127,
                LocationSource.MANUAL_SEARCH
        );
        return new WeatherSnapshot(condition, location, WeatherSource.fallback(null, null));
    }
}

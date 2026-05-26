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

@Component
public class StaticWeatherProvider implements WeatherProvider {

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

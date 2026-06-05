package com.smartcloset.weather.application;

import com.smartcloset.weather.domain.WeatherSnapshot;
import com.smartcloset.weather.dto.WeatherResponse;
import org.springframework.stereotype.Service;

/**
 * 현재 사용자 위치 기준 날씨를 조회해 API 응답 DTO로 변환한다.
 *
 * <p>날씨 조회만 수행하며 추천 결과나 착용 이력은 생성하지 않는다.</p>
 */
@Service
public class CurrentWeatherService {

    private final WeatherProvider weatherProvider;

    public CurrentWeatherService(WeatherProvider weatherProvider) {
        this.weatherProvider = weatherProvider;
    }

    /**
     * 현재 사용자 저장 위치의 CURRENT forecast period 날씨 snapshot을 API DTO로 변환한다.
     */
    public WeatherResponse getCurrentWeather(Long userId) {
        WeatherSnapshot weather = weatherProvider.getCurrentWeather(userId);
        return WeatherResponse.from(weather);
    }
}

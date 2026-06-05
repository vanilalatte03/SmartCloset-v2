package com.smartcloset.weather.controller;

import com.smartcloset.common.response.ApiResponse;
import com.smartcloset.security.CurrentUserPrincipal;
import com.smartcloset.weather.application.CurrentWeatherService;
import com.smartcloset.weather.dto.WeatherResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 사용자의 저장 위치를 기준으로 현재 날씨 요약을 조회하는 HTTP adapter다.
 *
 * <p>날씨 조회는 추천 결과나 착용 이력을 생성하지 않는 read-only 사용자 흐름이다.</p>
 */
@RestController
@RequestMapping("/api/weather/current")
public class CurrentWeatherController {

    private final CurrentWeatherService currentWeatherService;

    public CurrentWeatherController(CurrentWeatherService currentWeatherService) {
        this.currentWeatherService = currentWeatherService;
    }

    /**
     * 현재 사용자의 저장 위치와 CURRENT forecast period로 날씨 요약을 조회한다.
     */
    @GetMapping
    public ApiResponse<WeatherResponse> getCurrentWeather(
            @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        return ApiResponse.of(currentWeatherService.getCurrentWeather(principal.userId()));
    }
}

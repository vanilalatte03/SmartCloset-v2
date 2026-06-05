package com.smartcloset.user.controller;

import com.smartcloset.common.response.ApiResponse;
import com.smartcloset.security.CurrentUserPrincipal;
import com.smartcloset.user.application.UserLocationService;
import com.smartcloset.user.dto.UpdateUserLocationRequest;
import com.smartcloset.user.dto.UserLocationResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 현재 인증 사용자의 추천 기준 위치를 조회하고 교체하는 HTTP adapter다.
 *
 * <p>위치 값은 내부 catalog/KMA grid 기준으로 저장되며 raw browser GPS 좌표는 저장하지 않는다.</p>
 */
@RestController
@RequestMapping("/api/users/me/location")
public class UserLocationController {

    private final UserLocationService userLocationService;

    public UserLocationController(UserLocationService userLocationService) {
        this.userLocationService = userLocationService;
    }

    /**
     * 추천과 날씨 조회에 사용할 현재 사용자의 저장 위치 snapshot을 반환한다.
     */
    @GetMapping
    public ApiResponse<UserLocationResponse> getUserLocation(
            @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        return ApiResponse.of(userLocationService.getUserLocation(principal.userId()));
    }

    /**
     * 위치 catalog에서 선택한 행정구역 snapshot으로 현재 사용자의 추천 기준 위치를 교체한다.
     */
    @PutMapping
    public ApiResponse<UserLocationResponse> updateUserLocation(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody UpdateUserLocationRequest request
    ) {
        return ApiResponse.of(userLocationService.updateUserLocation(principal.userId(), request));
    }
}

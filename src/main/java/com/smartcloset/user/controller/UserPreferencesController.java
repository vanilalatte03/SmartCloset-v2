package com.smartcloset.user.controller;

import com.smartcloset.common.response.ApiResponse;
import com.smartcloset.security.CurrentUserPrincipal;
import com.smartcloset.user.application.UserPreferencesService;
import com.smartcloset.user.dto.UpdateUserPreferencesRequest;
import com.smartcloset.user.dto.UserPreferencesResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 현재 인증 사용자의 추천 선호도를 조회하고 전체 교체하는 HTTP adapter다.
 *
 * <p>선호 색상, 소재, 스타일 태그는 추천 점수의 preference 축에만 반영된다.</p>
 */
@RestController
@RequestMapping("/api/users/me/preferences")
public class UserPreferencesController {

    private final UserPreferencesService userPreferencesService;

    public UserPreferencesController(UserPreferencesService userPreferencesService) {
        this.userPreferencesService = userPreferencesService;
    }

    /**
     * 현재 사용자의 추천 개인화 선호 배열을 반환한다.
     */
    @GetMapping
    public ApiResponse<UserPreferencesResponse> getUserPreferences(
            @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        return ApiResponse.of(userPreferencesService.getUserPreferences(principal.userId()));
    }

    /**
     * 선호 색상, 소재, 스타일 태그 배열을 전체 교체한다.
     */
    @PutMapping
    public ApiResponse<UserPreferencesResponse> updateUserPreferences(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody UpdateUserPreferencesRequest request
    ) {
        return ApiResponse.of(userPreferencesService.updateUserPreferences(principal.userId(), request));
    }
}

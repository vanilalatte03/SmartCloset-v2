package com.smartcloset.user.presentation;

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

@RestController
@RequestMapping("/api/users/me/preferences")
public class UserPreferencesController {

    private final UserPreferencesService userPreferencesService;

    public UserPreferencesController(UserPreferencesService userPreferencesService) {
        this.userPreferencesService = userPreferencesService;
    }

    @GetMapping
    public ApiResponse<UserPreferencesResponse> getUserPreferences(
            @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        return ApiResponse.of(userPreferencesService.getUserPreferences(principal.userId()));
    }

    @PutMapping
    public ApiResponse<UserPreferencesResponse> updateUserPreferences(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody UpdateUserPreferencesRequest request
    ) {
        return ApiResponse.of(userPreferencesService.updateUserPreferences(principal.userId(), request));
    }
}

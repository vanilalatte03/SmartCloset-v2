package com.smartcloset.user.presentation;

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

@RestController
@RequestMapping("/api/users/me/location")
public class UserLocationController {

    private final UserLocationService userLocationService;

    public UserLocationController(UserLocationService userLocationService) {
        this.userLocationService = userLocationService;
    }

    @GetMapping
    public ApiResponse<UserLocationResponse> getUserLocation(
            @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        return ApiResponse.of(userLocationService.getUserLocation(principal.userId()));
    }

    @PutMapping
    public ApiResponse<UserLocationResponse> updateUserLocation(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody UpdateUserLocationRequest request
    ) {
        return ApiResponse.of(userLocationService.updateUserLocation(principal.userId(), request));
    }
}

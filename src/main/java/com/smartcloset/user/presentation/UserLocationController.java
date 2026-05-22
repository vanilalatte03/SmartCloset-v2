package com.smartcloset.user.presentation;

import com.smartcloset.common.response.ApiResponse;
import com.smartcloset.user.application.UserLocationService;
import com.smartcloset.user.dto.UpdateUserLocationRequest;
import com.smartcloset.user.dto.UserLocationResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/location")
public class UserLocationController {

    private final UserLocationService userLocationService;

    public UserLocationController(UserLocationService userLocationService) {
        this.userLocationService = userLocationService;
    }

    @GetMapping
    public ApiResponse<UserLocationResponse> getUserLocation(@RequestParam Long userId) {
        return ApiResponse.of(userLocationService.getUserLocation(userId));
    }

    @PutMapping
    public ApiResponse<UserLocationResponse> updateUserLocation(
            @RequestParam Long userId,
            @Valid @RequestBody UpdateUserLocationRequest request
    ) {
        return ApiResponse.of(userLocationService.updateUserLocation(userId, request));
    }
}

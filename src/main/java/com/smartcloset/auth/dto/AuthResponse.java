package com.smartcloset.auth.dto;

import com.smartcloset.user.dto.CurrentUserResponse;

public record AuthResponse(
        String accessToken,
        String tokenType,
        CurrentUserResponse user
) {

    public static AuthResponse bearer(String accessToken, CurrentUserResponse user) {
        return new AuthResponse(accessToken, "Bearer", user);
    }
}

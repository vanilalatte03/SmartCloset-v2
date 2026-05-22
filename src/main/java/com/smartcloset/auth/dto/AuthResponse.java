package com.smartcloset.auth.dto;

import com.smartcloset.user.dto.CurrentUserResponse;

public record AuthResponse(
        String accessToken,
        String tokenType,
        CurrentUserResponse user
) {

    private static final String BEARER = "Bearer";

    public static AuthResponse bearer(String accessToken, CurrentUserResponse user) {
        return new AuthResponse(accessToken, BEARER, user);
    }
}

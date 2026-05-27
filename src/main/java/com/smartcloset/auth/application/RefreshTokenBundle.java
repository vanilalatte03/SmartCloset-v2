package com.smartcloset.auth.application;

import com.smartcloset.auth.dto.AuthResponse;

public record RefreshTokenBundle(AuthResponse authResponse, String refreshToken) {
}

package com.smartcloset.auth.application;

import com.smartcloset.auth.dto.AuthResponse;

/**
 * Access token 응답과 새 refresh token 원문을 함께 운반하는 application 결과다.
 *
 * <p>Refresh token 원문은 controller에서 HttpOnly cookie로 쓰기 위한 값이며 JSON response로 노출하지 않는다.</p>
 */
public record RefreshTokenBundle(AuthResponse authResponse, String refreshToken) {
}

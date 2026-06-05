package com.smartcloset.security;

import com.smartcloset.user.domain.UserRole;
import java.util.Objects;

/**
 * JWT 검증 후 controller와 service 호출에 전달되는 현재 사용자 identity다.
 *
 * <p>보호 API는 query parameter userId 대신 이 principal의 userId를 기준으로 사용자 데이터를 격리한다.</p>
 */
public record CurrentUserPrincipal(Long userId, String email, UserRole role) {

    public CurrentUserPrincipal {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(role, "role must not be null");
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        if (email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
    }
}

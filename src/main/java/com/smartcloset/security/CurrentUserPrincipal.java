package com.smartcloset.security;

import com.smartcloset.user.domain.UserRole;
import java.util.Objects;

public record CurrentUserPrincipal(Long userId, String email, UserRole role) {

    public CurrentUserPrincipal {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(role, "role must not be null");
    }
}

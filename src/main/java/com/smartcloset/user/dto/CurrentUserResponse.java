package com.smartcloset.user.dto;

import com.smartcloset.user.domain.User;
import com.smartcloset.user.domain.UserRole;
import java.time.LocalDateTime;
import java.util.List;

public record CurrentUserResponse(
        String email,
        String name,
        UserRole role,
        boolean emailVerified,
        boolean passwordLoginEnabled,
        List<String> authProviders,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static CurrentUserResponse from(User user) {
        return from(user, user.isPasswordLoginEnabled() ? List.of("PASSWORD") : List.of());
    }

    public static CurrentUserResponse from(User user, List<String> authProviders) {
        return new CurrentUserResponse(
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.isEmailVerified(),
                user.isPasswordLoginEnabled(),
                List.copyOf(authProviders),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}

package com.smartcloset.user.dto;

import com.smartcloset.user.domain.User;
import com.smartcloset.user.domain.UserRole;
import java.time.LocalDateTime;

public record CurrentUserResponse(
        String email,
        String name,
        UserRole role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static CurrentUserResponse from(User user) {
        return new CurrentUserResponse(
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}

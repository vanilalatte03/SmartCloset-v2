package com.smartcloset.user.application;

import com.smartcloset.user.domain.User;
import java.time.LocalDateTime;
import java.util.Objects;

public record UserLocationSnapshot(
        Long userId,
        String code,
        String name,
        int nx,
        int ny,
        LocalDateTime updatedAt
) {

    public UserLocationSnapshot {
        Objects.requireNonNull(userId, "userId must not be null");
        code = requireText(code, "code");
        name = requireText(name, "name");
        if (nx <= 0) {
            throw new IllegalArgumentException("nx must be positive");
        }
        if (ny <= 0) {
            throw new IllegalArgumentException("ny must be positive");
        }
    }

    public static UserLocationSnapshot from(User user) {
        Objects.requireNonNull(user, "user must not be null");
        return new UserLocationSnapshot(
                user.getId(),
                user.getLocationCode(),
                user.getLocationName(),
                user.getLocationNx(),
                user.getLocationNy(),
                user.getUpdatedAt()
        );
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}

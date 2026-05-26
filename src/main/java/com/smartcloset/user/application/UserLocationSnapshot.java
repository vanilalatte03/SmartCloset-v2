package com.smartcloset.user.application;

import com.smartcloset.location.domain.LocationSource;
import com.smartcloset.user.domain.User;
import java.time.LocalDateTime;
import java.util.Objects;

public record UserLocationSnapshot(
        Long userId,
        String code,
        String name,
        String fullName,
        String region1,
        String region2,
        String region3,
        int nx,
        int ny,
        LocationSource source,
        LocalDateTime updatedAt
) {

    public UserLocationSnapshot(
            Long userId,
            String code,
            String name,
            int nx,
            int ny,
            LocalDateTime updatedAt
    ) {
        this(userId, code, name, name, name, null, null, nx, ny, LocationSource.MANUAL_SEARCH, updatedAt);
    }

    public UserLocationSnapshot {
        Objects.requireNonNull(userId, "userId must not be null");
        code = requireText(code, "code");
        name = requireText(name, "name");
        fullName = requireText(fullName == null ? name : fullName, "fullName");
        region1 = requireText(region1 == null ? name : region1, "region1");
        region2 = normalizeNullableText(region2);
        region3 = normalizeNullableText(region3);
        Objects.requireNonNull(source, "source must not be null");
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
                user.getLocationFullName(),
                user.getLocationRegion1(),
                user.getLocationRegion2(),
                user.getLocationRegion3(),
                user.getLocationNx(),
                user.getLocationNy(),
                user.getLocationSource(),
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

    private static String normalizeNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}

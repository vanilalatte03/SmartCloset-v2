package com.smartcloset.user.dto;

import com.smartcloset.user.domain.User;
import java.time.LocalDateTime;

public record UserLocationResponse(
        Long userId,
        String code,
        String name,
        int nx,
        int ny,
        LocalDateTime updatedAt
) {

    public static UserLocationResponse from(User user) {
        return new UserLocationResponse(
                user.getId(),
                user.getLocationCode(),
                user.getLocationName(),
                user.getLocationNx(),
                user.getLocationNy(),
                user.getUpdatedAt()
        );
    }
}

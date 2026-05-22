package com.smartcloset.user.dto;

import com.smartcloset.user.domain.User;
import java.time.LocalDateTime;

public record UserLocationResponse(
        String code,
        String name,
        int nx,
        int ny,
        LocalDateTime updatedAt
) {

    public static UserLocationResponse from(User user) {
        return new UserLocationResponse(
                user.getLocationCode(),
                user.getLocationName(),
                user.getLocationNx(),
                user.getLocationNy(),
                user.getUpdatedAt()
        );
    }
}

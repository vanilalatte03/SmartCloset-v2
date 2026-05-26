package com.smartcloset.user.dto;

import com.smartcloset.location.domain.LocationSource;
import com.smartcloset.user.domain.User;
import java.time.LocalDateTime;

public record UserLocationResponse(
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

    public static UserLocationResponse from(User user) {
        return new UserLocationResponse(
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
}

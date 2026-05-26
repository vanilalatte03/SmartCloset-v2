package com.smartcloset.location.dto;

import com.smartcloset.location.domain.LocationOption;
import java.math.BigDecimal;

public record LocationOptionResponse(
        String code,
        String name,
        String fullName,
        String region1,
        String region2,
        String region3,
        int nx,
        int ny,
        BigDecimal latitude,
        BigDecimal longitude
) {

    public static LocationOptionResponse from(LocationOption location) {
        return new LocationOptionResponse(
                location.code(),
                location.name(),
                location.fullName(),
                location.region1(),
                location.region2(),
                location.region3(),
                location.nx(),
                location.ny(),
                location.latitude(),
                location.longitude()
        );
    }
}

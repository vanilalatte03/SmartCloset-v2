package com.smartcloset.location.dto;

import com.smartcloset.location.domain.LocationOption;

public record LocationOptionResponse(
        String code,
        String name,
        int nx,
        int ny
) {

    public static LocationOptionResponse from(LocationOption location) {
        return new LocationOptionResponse(
                location.code(),
                location.name(),
                location.nx(),
                location.ny()
        );
    }
}

package com.smartcloset.location.dto;

import com.smartcloset.location.domain.LocationGrid;

public record LocationGridResponse(
        int nx,
        int ny
) {

    public static LocationGridResponse from(LocationGrid grid) {
        return new LocationGridResponse(grid.nx(), grid.ny());
    }
}

package com.smartcloset.location.dto;

import com.smartcloset.location.domain.LocationGrid;
import com.smartcloset.location.domain.LocationOption;
import java.util.List;

public record LocationResolveResponse(
        LocationGridResponse grid,
        LocationOptionResponse nearest,
        List<LocationOptionResponse> candidates
) {

    public static LocationResolveResponse of(LocationGrid grid, List<LocationOption> candidates) {
        return new LocationResolveResponse(
                LocationGridResponse.from(grid),
                candidates.isEmpty() ? null : LocationOptionResponse.from(candidates.getFirst()),
                candidates.stream()
                        .map(LocationOptionResponse::from)
                        .toList()
        );
    }
}

package com.smartcloset.user.dto;

import com.smartcloset.location.domain.LocationSource;
import jakarta.validation.constraints.NotBlank;

public record UpdateUserLocationRequest(
        @NotBlank(message = "locationCode is required")
        String locationCode,
        LocationSource source
) {

    public LocationSource resolvedSource() {
        return source == null ? LocationSource.MANUAL_SEARCH : source;
    }
}

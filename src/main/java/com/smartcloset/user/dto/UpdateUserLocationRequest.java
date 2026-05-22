package com.smartcloset.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserLocationRequest(
        @NotBlank(message = "locationCode is required")
        String locationCode
) {
}

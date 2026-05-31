package com.smartcloset.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCurrentUserRequest(
        @NotBlank
        @Size(max = 50)
        String name
) {
}

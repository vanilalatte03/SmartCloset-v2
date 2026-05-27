package com.smartcloset.user.dto;

import jakarta.validation.constraints.NotBlank;

public record AccountDeletionRequest(
        @NotBlank
        String confirmation,

        String password
) {
}

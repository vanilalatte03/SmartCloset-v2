package com.smartcloset.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record EmailVerificationConfirmRequest(
        @NotBlank
        String token
) {
}

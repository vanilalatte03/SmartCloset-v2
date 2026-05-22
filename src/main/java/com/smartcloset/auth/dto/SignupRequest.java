package com.smartcloset.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8)
        String password,

        @NotBlank
        @Size(max = 50)
        String name
) {
}

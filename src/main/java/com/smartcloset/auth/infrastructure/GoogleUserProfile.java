package com.smartcloset.auth.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleUserProfile(
        String sub,
        String email,
        @JsonProperty("email_verified") Boolean emailVerified,
        String name
) {
}

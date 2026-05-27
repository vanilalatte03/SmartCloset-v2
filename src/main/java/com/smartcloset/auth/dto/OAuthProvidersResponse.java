package com.smartcloset.auth.dto;

public record OAuthProvidersResponse(GoogleProvider google) {

    public static OAuthProvidersResponse google(boolean enabled, String loginUrl) {
        return new OAuthProvidersResponse(new GoogleProvider(enabled, enabled ? loginUrl : null));
    }

    public record GoogleProvider(boolean enabled, String loginUrl) {
    }
}

package com.smartcloset.auth.infrastructure;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smartcloset.security.oauth2")
public record GoogleOAuthProperties(
        String frontendCallbackUrl,
        Google google
) {

    private static final List<String> SCOPES = List.of("openid", "email", "profile");

    public boolean googleEnabled() {
        return google != null
                && hasText(google.clientId())
                && hasText(google.clientSecret())
                && hasText(google.redirectUri());
    }

    public List<String> scopes() {
        return SCOPES;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record Google(
            String clientId,
            String clientSecret,
            String redirectUri,
            String authorizationUri,
            String tokenUri,
            String userInfoUri
    ) {
    }
}

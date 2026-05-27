package com.smartcloset.auth.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smartcloset.security.oauth2.state-cookie")
public record OAuthStateCookieProperties(
        String name,
        boolean secure,
        String sameSite,
        String domain,
        String path,
        Duration maxAge
) {

    public OAuthStateCookieProperties {
        if (name == null || name.isBlank()) {
            name = "smartcloset.oauth2State";
        }
        if (sameSite == null || sameSite.isBlank()) {
            sameSite = "Lax";
        }
        if (path == null || path.isBlank()) {
            path = "/api/auth/oauth2";
        }
        if (maxAge == null) {
            maxAge = Duration.ofMinutes(5);
        }
    }
}

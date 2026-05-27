package com.smartcloset.auth.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smartcloset.security.refresh-token.cookie")
public record RefreshTokenCookieProperties(
        String name,
        boolean secure,
        String sameSite,
        String domain,
        String path,
        Duration maxAge
) {

    public RefreshTokenCookieProperties {
        if (name == null || name.isBlank()) {
            name = "smartcloset.refreshToken";
        }
        if (sameSite == null || sameSite.isBlank()) {
            sameSite = "Lax";
        }
        if (path == null || path.isBlank()) {
            path = "/api/auth";
        }
        if (maxAge == null) {
            maxAge = Duration.ofDays(14);
        }
    }
}

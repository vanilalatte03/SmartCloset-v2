package com.smartcloset.auth.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Refresh token cookie의 이름, 보안 속성, scope, 수명을 환경별로 바인딩한다.
 *
 * <p>Cookie path 기본값은 refresh/login/logout endpoint가 있는 auth 경계로 제한한다.</p>
 */
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

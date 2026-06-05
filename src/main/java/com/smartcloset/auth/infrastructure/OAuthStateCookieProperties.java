package com.smartcloset.auth.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OAuth2 state cookie의 이름, 보안 속성, scope, 짧은 수명을 환경별로 바인딩한다.
 *
 * <p>State cookie는 OAuth callback 검증에만 쓰이므로 refresh cookie와 별도 path/name을 가진다.</p>
 */
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

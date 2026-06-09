package com.smartcloset.auth.infrastructure;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Google OAuth2 login과 frontend callback redirect에 필요한 설정을 바인딩한다.
 *
 * <p>필수 Google client 설정이 비어 있으면 provider disabled 상태로 취급한다.</p>
 */
@ConfigurationProperties(prefix = "smartcloset.security.oauth2")
public record GoogleOAuthProperties(
        String frontendCallbackUrl,
        Google google
) {

    private static final List<String> SCOPES = List.of("openid", "email", "profile");
    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(3);
    public static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(5);

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
            String userInfoUri,
            Duration connectTimeout,
            Duration readTimeout
    ) {

        public Google {
            connectTimeout = timeoutOrDefault(connectTimeout, DEFAULT_CONNECT_TIMEOUT, "connectTimeout");
            readTimeout = timeoutOrDefault(readTimeout, DEFAULT_READ_TIMEOUT, "readTimeout");
        }
    }

    private static Duration timeoutOrDefault(Duration value, Duration defaultValue, String name) {
        Duration resolved = value == null ? defaultValue : value;
        if (resolved.isZero() || resolved.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return resolved;
    }
}

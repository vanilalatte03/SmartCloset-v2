package com.smartcloset.auth.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

class GoogleOAuthPropertiesTest {

    @Test
    void usesDocumentedGoogleTimeoutDefaults() {
        GoogleOAuthProperties properties = bindProperties(new MockEnvironment());

        assertThat(properties.google().connectTimeout()).isEqualTo(GoogleOAuthProperties.DEFAULT_CONNECT_TIMEOUT);
        assertThat(properties.google().readTimeout()).isEqualTo(GoogleOAuthProperties.DEFAULT_READ_TIMEOUT);
    }

    @Test
    void bindsConfiguredGoogleTimeouts() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("smartcloset.security.oauth2.google.connect-timeout", "750ms")
                .withProperty("smartcloset.security.oauth2.google.read-timeout", "2s");

        GoogleOAuthProperties properties = bindProperties(environment);

        assertThat(properties.google().connectTimeout()).isEqualTo(Duration.ofMillis(750));
        assertThat(properties.google().readTimeout()).isEqualTo(Duration.ofSeconds(2));
    }

    private GoogleOAuthProperties bindProperties(MockEnvironment environment) {
        environment
                .withProperty("smartcloset.security.oauth2.frontend-callback-url", "http://localhost:5173/auth/callback")
                .withProperty("smartcloset.security.oauth2.google.client-id", "google-client-id")
                .withProperty("smartcloset.security.oauth2.google.client-secret", "google-client-secret")
                .withProperty("smartcloset.security.oauth2.google.redirect-uri",
                        "http://localhost:8080/api/auth/oauth2/callback/google")
                .withProperty("smartcloset.security.oauth2.google.authorization-uri",
                        "https://accounts.google.com/o/oauth2/v2/auth")
                .withProperty("smartcloset.security.oauth2.google.token-uri", "https://oauth2.googleapis.com/token")
                .withProperty("smartcloset.security.oauth2.google.user-info-uri",
                        "https://openidconnect.googleapis.com/v1/userinfo");
        return Binder.get(environment)
                .bind("smartcloset.security.oauth2", GoogleOAuthProperties.class)
                .orElseThrow(IllegalStateException::new);
    }
}

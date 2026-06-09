package com.smartcloset.auth.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class GoogleOAuthClientTest {

    @Test
    void fetchUserProfileConvertsProviderTimeoutToOAuthUnavailable() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/token", exchange -> {
            try {
                Thread.sleep(3_000L);
                byte[] response = "{\"access_token\":\"slow-token\"}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            GoogleOAuthProperties properties = new GoogleOAuthProperties(
                    "http://localhost:5173/auth/callback",
                    new GoogleOAuthProperties.Google(
                            "google-client-id",
                            "google-client-secret",
                            "http://localhost:8080/api/auth/oauth2/callback/google",
                            "https://accounts.google.com/o/oauth2/v2/auth",
                            baseUrl + "/token",
                            baseUrl + "/userinfo",
                            Duration.ofMillis(200),
                            Duration.ofMillis(100)
                    )
            );
            GoogleOAuthClient client = new GoogleOAuthClient(properties);

            long startedAt = System.nanoTime();

            assertThatThrownBy(() -> client.fetchUserProfile("slow-code", properties))
                    .isInstanceOf(SmartClosetException.class)
                    .extracting(exception -> ((SmartClosetException) exception).errorCode())
                    .isEqualTo(ErrorCode.OAUTH2_PROVIDER_UNAVAILABLE);
            assertThat(Duration.ofNanos(System.nanoTime() - startedAt)).isLessThan(Duration.ofSeconds(2));
        } finally {
            server.stop(0);
        }
    }
}

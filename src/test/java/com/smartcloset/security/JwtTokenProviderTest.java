package com.smartcloset.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloset.user.domain.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String SECRET = "test-secret-with-enough-entropy-for-hs256";
    private static final Instant NOW = Instant.parse("2026-05-22T01:00:00Z");

    @Test
    void createsAndParsesAccessTokenWithSubjectClaimsAndTwoHourExpiration() throws Exception {
        JwtTokenProvider provider = providerAt(NOW);
        CurrentUserPrincipal principal = new CurrentUserPrincipal(42L, "demo@example.com", UserRole.USER);

        String token = provider.createAccessToken(principal);

        String[] parts = token.split("\\.");
        Map<String, Object> header = decodeJson(parts[0]);
        Map<String, Object> claims = decodeJson(parts[1]);
        CurrentUserPrincipal parsed = provider.parseAccessToken(token);

        assertThat(header).containsEntry("alg", "HS256")
                .containsEntry("typ", "JWT");
        assertThat(claims).containsEntry("sub", "42")
                .containsEntry("email", "demo@example.com")
                .containsEntry("role", "USER");
        assertThat(((Number) claims.get("iat")).longValue()).isEqualTo(NOW.getEpochSecond());
        assertThat(((Number) claims.get("exp")).longValue())
                .isEqualTo(NOW.plus(JwtTokenProvider.ACCESS_TOKEN_TTL).getEpochSecond());
        assertThat(parsed).isEqualTo(principal);
    }

    @Test
    void rejectsMalformedToken() {
        JwtTokenProvider provider = providerAt(NOW);

        assertThatThrownBy(() -> provider.parseAccessToken("not-a-jwt"))
                .isInstanceOf(JwtTokenException.class)
                .hasMessageContaining("three parts");
    }

    @Test
    void rejectsInvalidSignature() {
        JwtTokenProvider provider = providerAt(NOW);
        String token = provider.createAccessToken(new CurrentUserPrincipal(1L, "demo@example.com", UserRole.USER));
        String tamperedToken = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> provider.parseAccessToken(tamperedToken))
                .isInstanceOf(JwtTokenException.class)
                .hasMessageContaining("signature");
    }

    @Test
    void rejectsExpiredToken() {
        JwtTokenProvider issuer = providerAt(NOW);
        String token = issuer.createAccessToken(new CurrentUserPrincipal(1L, "demo@example.com", UserRole.USER));
        JwtTokenProvider verifier = providerAt(NOW.plus(JwtTokenProvider.ACCESS_TOKEN_TTL));

        assertThatThrownBy(() -> verifier.parseAccessToken(token))
                .isInstanceOf(JwtTokenException.class)
                .hasMessageContaining("expired");
    }

    private JwtTokenProvider providerAt(Instant instant) {
        return new JwtTokenProvider(SECRET, Clock.fixed(instant, ZoneOffset.UTC), OBJECT_MAPPER);
    }

    private Map<String, Object> decodeJson(String base64UrlJson) throws Exception {
        byte[] json = Base64.getUrlDecoder().decode(base64UrlJson);
        return OBJECT_MAPPER.readValue(json, new TypeReference<>() {
        });
    }
}

package com.smartcloset.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    private static final String SECRET = "test-jwt-secret-with-enough-length";
    private static final Instant NOW = Instant.parse("2026-05-22T01:00:00Z");
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createsHs256AccessTokenWithSubjectClaimsAndTwoHourExpiration() throws Exception {
        JwtTokenProvider provider = providerAt(NOW);
        CurrentUserPrincipal principal = new CurrentUserPrincipal(42L, "demo@example.com", UserRole.USER);

        String token = provider.createAccessToken(principal);

        CurrentUserPrincipal parsed = provider.parseAccessToken(token);
        Map<String, Object> header = decodePart(token, 0);
        Map<String, Object> payload = decodePart(token, 1);
        long issuedAt = ((Number) payload.get("iat")).longValue();
        long expiresAt = ((Number) payload.get("exp")).longValue();

        assertThat(parsed).isEqualTo(principal);
        assertThat(header).containsEntry("alg", "HS256")
                .containsEntry("typ", "JWT");
        assertThat(payload).containsEntry("sub", "42")
                .containsEntry("email", "demo@example.com")
                .containsEntry("role", "USER");
        assertThat(expiresAt - issuedAt).isEqualTo(JwtTokenProvider.ACCESS_TOKEN_TTL.toSeconds());
    }

    @Test
    void rejectsTokenWithInvalidSignature() {
        JwtTokenProvider provider = providerAt(NOW);
        String token = provider.createAccessToken(new CurrentUserPrincipal(42L, "demo@example.com", UserRole.USER));
        char replacement = token.charAt(token.length() - 1) == 'x' ? 'y' : 'x';
        String tampered = token.substring(0, token.length() - 1) + replacement;

        JwtTokenException exception = assertThrows(JwtTokenException.class, () -> provider.parseAccessToken(tampered));

        assertThat(exception.reason()).isEqualTo(JwtTokenFailureReason.INVALID);
    }

    @Test
    void rejectsExpiredToken() {
        String token = providerAt(NOW)
                .createAccessToken(new CurrentUserPrincipal(42L, "demo@example.com", UserRole.USER));
        JwtTokenProvider expiredVerifier = providerAt(NOW.plus(JwtTokenProvider.ACCESS_TOKEN_TTL).plusSeconds(1));

        JwtTokenException exception = assertThrows(JwtTokenException.class, () -> expiredVerifier.parseAccessToken(token));

        assertThat(exception.reason()).isEqualTo(JwtTokenFailureReason.EXPIRED);
    }

    private JwtTokenProvider providerAt(Instant instant) {
        return new JwtTokenProvider(SECRET, objectMapper, Clock.fixed(instant, ZoneOffset.UTC));
    }

    private Map<String, Object> decodePart(String token, int partIndex) throws Exception {
        String encoded = token.split("\\.")[partIndex];
        byte[] decoded = Base64.getUrlDecoder().decode(encoded);
        return objectMapper.readValue(decoded, MAP_TYPE);
    }
}

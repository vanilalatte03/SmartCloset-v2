package com.smartcloset.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloset.user.domain.UserRole;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    public static final Duration ACCESS_TOKEN_TTL = Duration.ofHours(2);

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final byte[] secretBytes;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public JwtTokenProvider(
            @Value("${smartcloset.security.jwt.secret}") String secret
    ) {
        this(secret, new ObjectMapper(), Clock.systemUTC());
    }

    JwtTokenProvider(String secret, ObjectMapper objectMapper, Clock clock) {
        Objects.requireNonNull(secret, "secret must not be null");
        if (secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret must not be blank");
        }
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public String createAccessToken(CurrentUserPrincipal principal) {
        Objects.requireNonNull(principal, "principal must not be null");

        Instant issuedAt = Instant.now(clock);
        Instant expiresAt = issuedAt.plus(ACCESS_TOKEN_TTL);
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", principal.userId().toString());
        payload.put("email", principal.email());
        payload.put("role", principal.role().name());
        payload.put("iat", issuedAt.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        String signingInput = base64UrlEncodeJson(header) + "." + base64UrlEncodeJson(payload);
        return signingInput + "." + sign(signingInput);
    }

    public CurrentUserPrincipal parseAccessToken(String token) {
        Objects.requireNonNull(token, "token must not be null");

        String[] parts = token.split("\\.", -1);
        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
            throw JwtTokenException.invalid("JWT access token must contain header, payload, and signature");
        }

        Map<String, Object> header = readJson(parts[0], "JWT header");
        if (!"HS256".equals(asString(header.get("alg"), "alg"))) {
            throw JwtTokenException.invalid("JWT algorithm must be HS256");
        }

        String signingInput = parts[0] + "." + parts[1];
        String expectedSignature = sign(signingInput);
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.US_ASCII),
                parts[2].getBytes(StandardCharsets.US_ASCII))) {
            throw JwtTokenException.invalid("JWT signature is invalid");
        }

        Map<String, Object> payload = readJson(parts[1], "JWT payload");
        Instant expiresAt = Instant.ofEpochSecond(asLong(payload.get("exp"), "exp"));
        if (!expiresAt.isAfter(Instant.now(clock))) {
            throw JwtTokenException.expired();
        }

        Long userId = parseUserId(asString(payload.get("sub"), "sub"));
        String email = asString(payload.get("email"), "email");
        UserRole role = parseRole(asString(payload.get("role"), "role"));
        return new CurrentUserPrincipal(userId, email, role);
    }

    private String base64UrlEncodeJson(Map<String, Object> value) {
        try {
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize JWT content", exception);
        }
    }

    private Map<String, Object> readJson(String encodedJson, String label) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(encodedJson);
            return objectMapper.readValue(decoded, MAP_TYPE);
        } catch (IllegalArgumentException | IOException exception) {
            throw JwtTokenException.invalid("Failed to parse " + label, exception);
        }
    }

    private String sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secretBytes, HMAC_SHA256));
            byte[] signature = mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to sign JWT access token", exception);
        }
    }

    private String asString(Object value, String claim) {
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue;
        }
        throw JwtTokenException.invalid("JWT claim is missing or invalid: " + claim);
    }

    private long asLong(Object value, String claim) {
        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }
        throw JwtTokenException.invalid("JWT claim is missing or invalid: " + claim);
    }

    private Long parseUserId(String subject) {
        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException exception) {
            throw JwtTokenException.invalid("JWT subject must be a user id", exception);
        }
    }

    private UserRole parseRole(String role) {
        try {
            return UserRole.valueOf(role);
        } catch (IllegalArgumentException exception) {
            throw JwtTokenException.invalid("JWT role is unsupported", exception);
        }
    }
}

package com.smartcloset.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloset.user.domain.UserRole;
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

    static final Duration ACCESS_TOKEN_TTL = Duration.ofHours(2);
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String JWT_ALGORITHM = "HS256";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final Clock clock;

    @Autowired
    public JwtTokenProvider(@Value("${smartcloset.security.jwt.secret}") String secret) {
        this(secret, Clock.systemUTC(), new ObjectMapper());
    }

    JwtTokenProvider(String secret, Clock clock, ObjectMapper objectMapper) {
        this.secret = requireSecret(secret).getBytes(StandardCharsets.UTF_8);
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public String createAccessToken(CurrentUserPrincipal principal) {
        Objects.requireNonNull(principal, "principal must not be null");
        Instant issuedAt = Instant.now(clock);
        Instant expiresAt = issuedAt.plus(ACCESS_TOKEN_TTL);

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", JWT_ALGORITHM);
        header.put("typ", "JWT");

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", principal.userId().toString());
        claims.put("email", principal.email());
        claims.put("role", principal.role().name());
        claims.put("iat", issuedAt.getEpochSecond());
        claims.put("exp", expiresAt.getEpochSecond());

        String signingInput = base64UrlJson(header) + "." + base64UrlJson(claims);
        return signingInput + "." + base64Url(hmac(signingInput));
    }

    public CurrentUserPrincipal parseAccessToken(String token) {
        try {
            String[] parts = splitToken(token);
            String signingInput = parts[0] + "." + parts[1];
            if (!MessageDigest.isEqual(base64UrlDecode(parts[2]), hmac(signingInput))) {
                throw new JwtTokenException("Invalid JWT signature");
            }

            Map<String, Object> header = readJson(parts[0]);
            if (!JWT_ALGORITHM.equals(header.get("alg"))) {
                throw new JwtTokenException("Unsupported JWT algorithm");
            }

            Map<String, Object> claims = readJson(parts[1]);
            Instant expiresAt = Instant.ofEpochSecond(longClaim(claims, "exp"));
            if (!expiresAt.isAfter(Instant.now(clock))) {
                throw new JwtTokenException("JWT has expired");
            }

            return new CurrentUserPrincipal(
                    Long.valueOf(stringClaim(claims, "sub")),
                    stringClaim(claims, "email"),
                    UserRole.valueOf(stringClaim(claims, "role"))
            );
        } catch (JwtTokenException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new JwtTokenException("Invalid JWT", exception);
        }
    }

    private String[] splitToken(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtTokenException("JWT must not be blank");
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) {
            throw new JwtTokenException("JWT must have three parts");
        }
        return parts;
    }

    private String base64UrlJson(Map<String, Object> value) {
        try {
            return base64Url(objectMapper.writeValueAsBytes(value));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to write JWT JSON", exception);
        }
    }

    private Map<String, Object> readJson(String base64UrlJson) {
        try {
            return objectMapper.readValue(base64UrlDecode(base64UrlJson), MAP_TYPE);
        } catch (Exception exception) {
            throw new JwtTokenException("Invalid JWT JSON", exception);
        }
    }

    private byte[] hmac(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret, HMAC_SHA256));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to sign JWT", exception);
        }
    }

    private String stringClaim(Map<String, Object> claims, String claimName) {
        Object value = claims.get(claimName);
        if (!(value instanceof String stringValue) || stringValue.isBlank()) {
            throw new JwtTokenException("Missing JWT claim: " + claimName);
        }
        return stringValue;
    }

    private long longClaim(Map<String, Object> claims, String claimName) {
        Object value = claims.get(claimName);
        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }
        throw new JwtTokenException("Missing JWT claim: " + claimName);
    }

    private static String requireSecret(String secret) {
        Objects.requireNonNull(secret, "JWT secret must not be null");
        if (secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret must not be blank");
        }
        return secret;
    }

    private static String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    static byte[] base64UrlDecode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }
}

package com.smartcloset.auth.application;

import com.smartcloset.auth.domain.RefreshSession;
import com.smartcloset.auth.repository.RefreshSessionRepository;
import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import com.smartcloset.user.domain.User;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Refresh token 원문을 발급/회전하되 DB에는 HMAC hash만 저장한다.
 *
 * <p>토큰 원문은 한 번만 반환되어 HttpOnly cookie로 내려가고, 이후 검증은 hash 조회로 수행된다.</p>
 */
@Service
public class RefreshTokenService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final int TOKEN_BYTE_LENGTH = 32;

    private final RefreshSessionRepository refreshSessionRepository;
    private final byte[] secretBytes;
    private final long ttlDays;
    private final SecureRandom secureRandom;
    private final Clock clock;

    @Autowired
    public RefreshTokenService(
            RefreshSessionRepository refreshSessionRepository,
            @Value("${smartcloset.security.jwt.secret}") String secret,
            @Value("${smartcloset.security.refresh-token.ttl-days}") long ttlDays
    ) {
        this(refreshSessionRepository, secret, ttlDays, new SecureRandom(), Clock.systemUTC());
    }

    RefreshTokenService(
            RefreshSessionRepository refreshSessionRepository,
            String secret,
            long ttlDays,
            SecureRandom secureRandom,
            Clock clock
    ) {
        this.refreshSessionRepository = refreshSessionRepository;
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("refresh token hash secret must not be blank");
        }
        if (ttlDays <= 0) {
            throw new IllegalArgumentException("refresh token ttlDays must be positive");
        }
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.ttlDays = ttlDays;
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * 새 refresh session을 생성하고 raw token은 호출자에게만 반환한다.
     */
    public IssuedRefreshToken issue(User user) {
        String refreshToken = generateToken();
        String tokenHash = hash(refreshToken);
        LocalDateTime issuedAt = now();
        RefreshSession session = RefreshSession.issue(user, tokenHash, issuedAt, issuedAt.plusDays(ttlDays));
        refreshSessionRepository.save(session);
        return new IssuedRefreshToken(refreshToken, tokenHash);
    }

    /**
     * 현재 token을 revoke하고 새 token hash를 replacedByTokenHash에 남겨 재사용 추적이 가능하게 한다.
     */
    public RotatedRefreshToken rotate(String refreshToken) {
        String currentHash = hash(refreshToken);
        RefreshSession currentSession = refreshSessionRepository.findByTokenHash(currentHash)
                .orElseThrow(() -> new SmartClosetException(ErrorCode.INVALID_TOKEN));
        LocalDateTime now = now();
        if (!currentSession.isActive(now)) {
            throw new SmartClosetException(ErrorCode.INVALID_TOKEN);
        }

        IssuedRefreshToken next = issue(currentSession.getUser());
        currentSession.replace(next.tokenHash(), now);
        return new RotatedRefreshToken(currentSession.getUser(), next.refreshToken());
    }

    /**
     * token hash가 존재할 때만 revoke해 logout endpoint의 멱등성을 유지한다.
     */
    public void revokeIfPresent(String refreshToken) {
        String tokenHash = hash(refreshToken);
        refreshSessionRepository.findByTokenHash(tokenHash)
                .ifPresent(session -> session.revoke(now()));
    }

    /**
     * 비밀번호 재설정이나 계정 보안 이벤트 후 사용자의 모든 활성 refresh session을 폐기한다.
     */
    public void revokeAll(User user) {
        LocalDateTime revokedAt = now();
        refreshSessionRepository.findByUserIdAndRevokedAtIsNull(user.getId())
                .forEach(session -> session.revoke(revokedAt));
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 서버 secret 기반 HMAC을 사용해 같은 token이면 같은 hash가 나오지만 원문은 복원할 수 없게 한다.
     */
    private String hash(String token) {
        if (token == null || token.isBlank()) {
            throw new SmartClosetException(ErrorCode.INVALID_TOKEN);
        }
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secretBytes, HMAC_SHA256));
            byte[] digest = mac.doFinal(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to hash refresh token", exception);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public record IssuedRefreshToken(String refreshToken, String tokenHash) {
    }

    public record RotatedRefreshToken(User user, String refreshToken) {
    }
}

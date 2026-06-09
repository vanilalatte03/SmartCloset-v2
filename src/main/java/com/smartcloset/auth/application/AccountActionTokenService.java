package com.smartcloset.auth.application;

import com.smartcloset.auth.domain.AccountActionToken;
import com.smartcloset.auth.domain.AccountActionTokenPurpose;
import com.smartcloset.auth.repository.AccountActionTokenRepository;
import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import com.smartcloset.user.domain.User;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이메일 인증과 비밀번호 재설정에 쓰는 single-use 토큰을 관리한다.
 *
 * <p>Refresh token과 마찬가지로 원문 토큰은 메일 발송용으로만 사용하고 DB에는 HMAC hash만 저장한다.</p>
 */
@Service
public class AccountActionTokenService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final int TOKEN_BYTE_LENGTH = 32;

    private final AccountActionTokenRepository accountActionTokenRepository;
    private final byte[] secretBytes;
    private final Duration emailVerificationTtl;
    private final Duration passwordResetTtl;
    private final SecureRandom secureRandom;
    private final Clock clock;

    @Autowired
    public AccountActionTokenService(
            AccountActionTokenRepository accountActionTokenRepository,
            @Value("${smartcloset.security.jwt.secret}") String secret,
            @Value("${smartcloset.security.account-action.email-verification-ttl-hours}") long emailVerificationTtlHours,
            @Value("${smartcloset.security.account-action.password-reset-ttl-minutes}") long passwordResetTtlMinutes
    ) {
        this(
                accountActionTokenRepository,
                secret,
                Duration.ofHours(emailVerificationTtlHours),
                Duration.ofMinutes(passwordResetTtlMinutes),
                new SecureRandom(),
                Clock.systemUTC()
        );
    }

    AccountActionTokenService(
            AccountActionTokenRepository accountActionTokenRepository,
            String secret,
            Duration emailVerificationTtl,
            Duration passwordResetTtl,
            SecureRandom secureRandom,
            Clock clock
    ) {
        this.accountActionTokenRepository = accountActionTokenRepository;
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("account action token hash secret must not be blank");
        }
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.emailVerificationTtl = requirePositive(emailVerificationTtl, "emailVerificationTtl");
        this.passwordResetTtl = requirePositive(passwordResetTtl, "passwordResetTtl");
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * 목적별 TTL을 적용한 새 계정 액션 토큰을 발급한다.
     */
    @Transactional
    public IssuedAccountActionToken issue(User user, AccountActionTokenPurpose purpose) {
        String token = generateToken();
        String tokenHash = hash(token);
        LocalDateTime expiresAt = now().plus(ttlFor(purpose));
        AccountActionToken accountActionToken = AccountActionToken.issue(user, purpose, tokenHash, expiresAt);
        accountActionTokenRepository.save(accountActionToken);
        return new IssuedAccountActionToken(token, tokenHash);
    }

    /**
     * 토큰 목적, 만료, 사용 여부를 한 transaction 안에서 검증하고 사용 처리한다.
     */
    @Transactional
    public User consume(String token, AccountActionTokenPurpose purpose) {
        String tokenHash = hash(token);
        AccountActionToken accountActionToken = accountActionTokenRepository.findByTokenHashForConsume(tokenHash)
                .orElseThrow(() -> new SmartClosetException(ErrorCode.ACCOUNT_TOKEN_INVALID));
        LocalDateTime now = now();
        if (!accountActionToken.canConsume(purpose, now)) {
            throw new SmartClosetException(ErrorCode.ACCOUNT_TOKEN_INVALID);
        }
        accountActionToken.markUsed(now);
        return accountActionToken.getUser();
    }

    private Duration ttlFor(AccountActionTokenPurpose purpose) {
        return switch (purpose) {
            case EMAIL_VERIFICATION -> emailVerificationTtl;
            case PASSWORD_RESET -> passwordResetTtl;
        };
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        if (token == null || token.isBlank()) {
            throw new SmartClosetException(ErrorCode.ACCOUNT_TOKEN_INVALID);
        }
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secretBytes, HMAC_SHA256));
            byte[] digest = mac.doFinal(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to hash account action token", exception);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private Duration requirePositive(Duration duration, String fieldName) {
        Objects.requireNonNull(duration, fieldName + " must not be null");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return duration;
    }

    public record IssuedAccountActionToken(String token, String tokenHash) {
    }
}

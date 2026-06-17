package com.smartcloset.auth.application;

import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * MVP10 범위의 process-local login 실패 시도 제한이다.
 *
 * <p>Redis나 proxy/CDN 연동 없이 현재 애플리케이션 process 안에서 email/client key별 window를
 * 관리한다. 분산 rate limit은 별도 운영 범위다.</p>
 */
@Component
public class LoginAttemptThrottle {

    private static final String UNKNOWN_CLIENT = "unknown";

    private final LoginAttemptThrottleProperties properties;
    private final Clock clock;
    private final ConcurrentMap<LoginAttemptKey, AttemptWindow> attempts = new ConcurrentHashMap<>();

    @Autowired
    public LoginAttemptThrottle(LoginAttemptThrottleProperties properties) {
        this(properties, Clock.systemDefaultZone());
    }

    LoginAttemptThrottle(LoginAttemptThrottleProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * 인증 비용을 쓰기 전에 현재 요청을 window에 기록하고, 허용 횟수 초과 시 stable error code로 실패한다.
     */
    public void checkAndRecordAttempt(String email, String clientIdentifier) {
        if (!properties.enabled()) {
            return;
        }

        int maxFailures = properties.maxFailures();
        if (maxFailures < 1) {
            throw new SmartClosetException(ErrorCode.LOGIN_ATTEMPT_LIMIT_EXCEEDED);
        }

        Instant now = clock.instant();
        Duration window = properties.window();
        cleanupExpiredAttempts(now, window);

        boolean exceeded = false;
        for (LoginAttemptKey key : keys(email, clientIdentifier)) {
            AttemptWindow updated = attempts.compute(key, (ignored, current) -> {
                if (current == null || current.isExpired(now, window)) {
                    return new AttemptWindow(now, 1);
                }
                return current.increment();
            });
            exceeded = exceeded || updated.failures() > maxFailures;
        }
        if (exceeded) {
            throw new SmartClosetException(ErrorCode.LOGIN_ATTEMPT_LIMIT_EXCEEDED);
        }
    }

    /**
     * 정상 로그인 성공 시 같은 email/client key와 client key의 실패 window를 제거한다.
     */
    public void recordSuccess(String email, String clientIdentifier) {
        clearAttempts(email, clientIdentifier);
    }

    /**
     * 인증 실패가 아닌 서버 오류 경로에서는 현재 요청 예약 기록을 제거한다.
     */
    public void clearAttempts(String email, String clientIdentifier) {
        if (!properties.enabled()) {
            return;
        }
        keys(email, clientIdentifier).forEach(attempts::remove);
    }

    int attemptCountFor(String email, String clientIdentifier) {
        AttemptWindow window = attempts.get(emailClientKey(email, clientIdentifier));
        return window == null ? 0 : window.failures();
    }

    int clientAttemptCountFor(String clientIdentifier) {
        AttemptWindow window = attempts.get(clientKey(clientIdentifier));
        return window == null ? 0 : window.failures();
    }

    private void cleanupExpiredAttempts(Instant now, Duration window) {
        attempts.entrySet().removeIf(entry -> entry.getValue().isExpired(now, window));
    }

    private List<LoginAttemptKey> keys(String email, String clientIdentifier) {
        return List.of(emailClientKey(email, clientIdentifier), clientKey(clientIdentifier));
    }

    private LoginAttemptKey emailClientKey(String email, String clientIdentifier) {
        return new LoginAttemptKey("email-client", normalizeEmail(email), normalizeClientIdentifier(clientIdentifier));
    }

    private LoginAttemptKey clientKey(String clientIdentifier) {
        return new LoginAttemptKey("client", "", normalizeClientIdentifier(clientIdentifier));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeClientIdentifier(String clientIdentifier) {
        if (clientIdentifier == null || clientIdentifier.isBlank()) {
            return UNKNOWN_CLIENT;
        }
        return clientIdentifier.trim();
    }

    private record LoginAttemptKey(String scope, String email, String clientIdentifier) {
    }

    private record AttemptWindow(Instant startedAt, int failures) {

        private boolean isExpired(Instant now, Duration window) {
            return !now.isBefore(startedAt.plus(window));
        }

        private AttemptWindow increment() {
            return new AttemptWindow(startedAt, failures + 1);
        }
    }
}

package com.smartcloset.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.smartcloset.auth.application.RefreshTokenService;
import com.smartcloset.auth.domain.RefreshSession;
import com.smartcloset.auth.repository.RefreshSessionRepository;
import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class RefreshTokenConcurrencyTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshSessionRepository refreshSessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private ExecutorService executorService;
    private Long createdUserId;

    @AfterEach
    void cleanup() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        if (createdUserId != null) {
            refreshSessionRepository.deleteAll(refreshSessionRepository.findByUserId(createdUserId));
            userRepository.findById(createdUserId).ifPresent(userRepository::delete);
        }
    }

    @Test
    void concurrentRotateAllowsOnlyOneSuccessForSameRefreshToken() throws Exception {
        User user = userRepository.save(User.create(
                "refresh-concurrent-" + System.nanoTime() + "@example.com",
                passwordEncoder.encode("password123!"),
                "Concurrent Refresh"
        ));
        createdUserId = user.getId();
        RefreshTokenService.IssuedRefreshToken issued = refreshTokenService.issue(user);
        executorService = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<RotationAttempt> first = executorService.submit(rotationAttempt(issued.refreshToken(), ready, start));
        Future<RotationAttempt> second = executorService.submit(rotationAttempt(issued.refreshToken(), ready, start));

        assertThat(ready.await(3, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        List<RotationAttempt> attempts = List.of(first.get(3, TimeUnit.SECONDS), second.get(3, TimeUnit.SECONDS));

        assertThat(attempts).filteredOn(RotationAttempt::success).hasSize(1);
        assertThat(attempts).filteredOn(attempt -> !attempt.success())
                .singleElement()
                .satisfies(attempt -> assertThat(attempt.errorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));

        List<RefreshSession> userSessions = refreshSessionRepository.findByUserId(user.getId());
        assertThat(userSessions).hasSize(2);

        RefreshSession originalSession = userSessions.stream()
                .filter(session -> session.getTokenHash().equals(issued.tokenHash()))
                .findFirst()
                .orElseThrow();
        RefreshSession activeSession = userSessions.stream()
                .filter(session -> session.getRevokedAt() == null)
                .findFirst()
                .orElseThrow();

        assertThat(originalSession.getRevokedAt()).isNotNull();
        assertThat(originalSession.getReplacedByTokenHash()).isEqualTo(activeSession.getTokenHash());
        assertThat(activeSession.getReplacedByTokenHash()).isNull();
        assertThat(attempts).filteredOn(RotationAttempt::success)
                .singleElement()
                .satisfies(attempt -> {
                    assertThat(attempt.refreshToken()).isNotBlank();
                    assertThat(attempt.refreshToken()).isNotEqualTo(issued.refreshToken());
                    assertThat(activeSession.getTokenHash()).isNotEqualTo(attempt.refreshToken());
                });
    }

    private Callable<RotationAttempt> rotationAttempt(
            String refreshToken,
            CountDownLatch ready,
            CountDownLatch start
    ) {
        return () -> {
            ready.countDown();
            if (!start.await(3, TimeUnit.SECONDS)) {
                throw new IllegalStateException("concurrent refresh start signal timed out");
            }
            try {
                RefreshTokenService.RotatedRefreshToken rotated = refreshTokenService.rotate(refreshToken);
                return RotationAttempt.success(rotated.refreshToken());
            } catch (SmartClosetException exception) {
                return RotationAttempt.failure(exception.errorCode());
            }
        };
    }

    private record RotationAttempt(boolean success, String refreshToken, ErrorCode errorCode) {

        static RotationAttempt success(String refreshToken) {
            return new RotationAttempt(true, refreshToken, null);
        }

        static RotationAttempt failure(ErrorCode errorCode) {
            return new RotationAttempt(false, null, errorCode);
        }
    }
}

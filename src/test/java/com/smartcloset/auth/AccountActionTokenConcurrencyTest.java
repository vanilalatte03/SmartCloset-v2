package com.smartcloset.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.smartcloset.auth.application.AccountActionTokenService;
import com.smartcloset.auth.application.AuthService;
import com.smartcloset.auth.application.RefreshTokenService;
import com.smartcloset.auth.domain.AccountActionTokenPurpose;
import com.smartcloset.auth.dto.EmailVerificationConfirmRequest;
import com.smartcloset.auth.dto.PasswordResetConfirmRequest;
import com.smartcloset.auth.repository.AccountActionTokenRepository;
import com.smartcloset.auth.repository.RefreshSessionRepository;
import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import java.util.ArrayList;
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
class AccountActionTokenConcurrencyTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private AccountActionTokenService accountActionTokenService;

    @Autowired
    private AccountActionTokenRepository accountActionTokenRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshSessionRepository refreshSessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final List<Long> createdUserIds = new ArrayList<>();
    private ExecutorService executorService;

    @AfterEach
    void cleanup() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        for (Long userId : createdUserIds) {
            refreshSessionRepository.deleteAll(refreshSessionRepository.findByUserId(userId));
            accountActionTokenRepository.deleteAll(accountActionTokenRepository.findByUserId(userId));
            userRepository.findById(userId).ifPresent(userRepository::delete);
        }
    }

    @Test
    void concurrentEmailVerificationConfirmAllowsOnlyOneSuccessForSameToken() throws Exception {
        User user = userRepository.save(User.createPasswordSignup(
                "verify-concurrent-" + System.nanoTime() + "@example.com",
                passwordEncoder.encode("password123!"),
                "Concurrent Verify"
        ));
        createdUserIds.add(user.getId());
        AccountActionTokenService.IssuedAccountActionToken token =
                accountActionTokenService.issue(user, AccountActionTokenPurpose.EMAIL_VERIFICATION);

        List<ActionAttempt> attempts = runTwoAttempts(verificationAttempt(token.token()));

        assertThat(attempts).filteredOn(ActionAttempt::success).hasSize(1);
        assertThat(attempts).filteredOn(attempt -> !attempt.success())
                .singleElement()
                .satisfies(attempt -> assertThat(attempt.errorCode()).isEqualTo(ErrorCode.ACCOUNT_TOKEN_INVALID));
        assertThat(userRepository.findById(user.getId()).orElseThrow().isEmailVerified()).isTrue();
        assertSingleUsedToken(user.getId(), token.token());
    }

    @Test
    void concurrentPasswordResetConfirmAllowsOnlyOneSuccessForSameToken() throws Exception {
        User user = userRepository.save(User.create(
                "reset-concurrent-" + System.nanoTime() + "@example.com",
                passwordEncoder.encode("oldPassword123!"),
                "Concurrent Reset"
        ));
        createdUserIds.add(user.getId());
        refreshTokenService.issue(user);
        AccountActionTokenService.IssuedAccountActionToken token =
                accountActionTokenService.issue(user, AccountActionTokenPurpose.PASSWORD_RESET);

        List<ActionAttempt> attempts = runTwoAttempts(passwordResetAttempt(token.token()));

        assertThat(attempts).filteredOn(ActionAttempt::success).hasSize(1);
        assertThat(attempts).filteredOn(attempt -> !attempt.success())
                .singleElement()
                .satisfies(attempt -> assertThat(attempt.errorCode()).isEqualTo(ErrorCode.ACCOUNT_TOKEN_INVALID));
        User resetUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("newPassword123!", resetUser.getPasswordHash())).isTrue();
        assertThat(refreshSessionRepository.findByUserId(user.getId()))
                .singleElement()
                .satisfies(session -> assertThat(session.getRevokedAt()).isNotNull());
        assertSingleUsedToken(user.getId(), token.token());
    }

    private List<ActionAttempt> runTwoAttempts(AttemptFactory attemptFactory) throws Exception {
        executorService = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<ActionAttempt> first = executorService.submit(attemptFactory.create(ready, start));
        Future<ActionAttempt> second = executorService.submit(attemptFactory.create(ready, start));

        assertThat(ready.await(3, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        return List.of(first.get(3, TimeUnit.SECONDS), second.get(3, TimeUnit.SECONDS));
    }

    private AttemptFactory verificationAttempt(String token) {
        return (ready, start) -> () -> {
            awaitStart(ready, start);
            try {
                authService.confirmEmailVerification(new EmailVerificationConfirmRequest(token));
                return ActionAttempt.succeeded();
            } catch (SmartClosetException exception) {
                return ActionAttempt.failure(exception.errorCode());
            }
        };
    }

    private AttemptFactory passwordResetAttempt(String token) {
        return (ready, start) -> () -> {
            awaitStart(ready, start);
            try {
                authService.confirmPasswordReset(new PasswordResetConfirmRequest(token, "newPassword123!"));
                return ActionAttempt.succeeded();
            } catch (SmartClosetException exception) {
                return ActionAttempt.failure(exception.errorCode());
            }
        };
    }

    private void awaitStart(CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        if (!start.await(3, TimeUnit.SECONDS)) {
            throw new IllegalStateException("concurrent account token start signal timed out");
        }
    }

    private void assertSingleUsedToken(Long userId, String rawToken) {
        assertThat(accountActionTokenRepository.findByUserId(userId))
                .singleElement()
                .satisfies(token -> {
                    assertThat(token.getTokenHash()).isNotBlank();
                    assertThat(token.getTokenHash()).isNotEqualTo(rawToken);
                    assertThat(token.getUsedAt()).isNotNull();
                });
    }

    private interface AttemptFactory {

        Callable<ActionAttempt> create(CountDownLatch ready, CountDownLatch start);
    }

    private record ActionAttempt(boolean success, ErrorCode errorCode) {

        static ActionAttempt succeeded() {
            return new ActionAttempt(true, null);
        }

        static ActionAttempt failure(ErrorCode errorCode) {
            return new ActionAttempt(false, errorCode);
        }
    }
}

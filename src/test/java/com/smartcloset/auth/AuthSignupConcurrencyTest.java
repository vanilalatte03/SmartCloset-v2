package com.smartcloset.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.smartcloset.auth.application.AuthService;
import com.smartcloset.auth.domain.AccountActionTokenPurpose;
import com.smartcloset.auth.dto.SignupRequest;
import com.smartcloset.auth.repository.AccountActionTokenRepository;
import com.smartcloset.clothing.repository.ClothingItemRepository;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@ActiveProfiles("test")
@SpringBootTest
class AuthSignupConcurrencyTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClothingItemRepository clothingItemRepository;

    @Autowired
    private AccountActionTokenRepository accountActionTokenRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private ExecutorService executorService;
    private TransactionTemplate transactionTemplate;
    private String createdEmail;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    void cleanup() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        if (createdEmail != null) {
            transactionTemplate.executeWithoutResult(status -> {
                userRepository.findByEmail(createdEmail).ifPresent(user -> {
                    accountActionTokenRepository.deleteByUserId(user.getId());
                    clothingItemRepository.deleteByUserId(user.getId());
                    userRepository.delete(user);
                });
            });
        }
    }

    @Test
    void concurrentSignupForSameEmailAllowsOnlyOneSuccess() throws Exception {
        String email = "signup-race-" + System.nanoTime() + "@example.com";
        createdEmail = email;
        executorService = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<SignupAttempt> first = executorService.submit(signupAttempt(email, "Race User A", ready, start));
        Future<SignupAttempt> second = executorService.submit(signupAttempt(email, "Race User B", ready, start));

        assertThat(ready.await(3, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        List<SignupAttempt> attempts = List.of(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));

        assertThat(attempts).filteredOn(SignupAttempt::success).hasSize(1);
        assertThat(attempts).filteredOn(attempt -> !attempt.success())
                .singleElement()
                .satisfies(attempt -> assertThat(attempt.errorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS));

        User user = userRepository.findByEmail(email).orElseThrow();
        assertThat(clothingItemRepository.countByUserId(user.getId())).isEqualTo(5);
        assertThat(accountActionTokenRepository.findByUserId(user.getId()))
                .singleElement()
                .satisfies(token -> assertThat(token.getPurpose())
                        .isEqualTo(AccountActionTokenPurpose.EMAIL_VERIFICATION));
    }

    private Callable<SignupAttempt> signupAttempt(
            String email,
            String name,
            CountDownLatch ready,
            CountDownLatch start
    ) {
        return () -> {
            ready.countDown();
            if (!start.await(3, TimeUnit.SECONDS)) {
                throw new IllegalStateException("concurrent signup start signal timed out");
            }
            try {
                authService.signup(new SignupRequest(email, "password123!", name));
                return SignupAttempt.succeeded(email);
            } catch (SmartClosetException exception) {
                return SignupAttempt.failure(exception.errorCode());
            }
        };
    }

    private record SignupAttempt(boolean success, String email, ErrorCode errorCode) {

        static SignupAttempt succeeded(String email) {
            return new SignupAttempt(true, email, null);
        }

        static SignupAttempt failure(ErrorCode errorCode) {
            return new SignupAttempt(false, null, errorCode);
        }
    }
}

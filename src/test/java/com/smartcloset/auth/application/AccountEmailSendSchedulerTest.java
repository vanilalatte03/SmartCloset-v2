package com.smartcloset.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smartcloset.auth.domain.AccountActionTokenPurpose;
import com.smartcloset.auth.dto.EmailVerificationRequest;
import com.smartcloset.auth.dto.PasswordResetRequest;
import com.smartcloset.auth.dto.SignupRequest;
import com.smartcloset.auth.infrastructure.EmailSender;
import com.smartcloset.auth.repository.AccountActionTokenRepository;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@ActiveProfiles("test")
@SpringBootTest
@Import(AccountEmailSendSchedulerTest.EmailSenderConfig.class)
class AccountEmailSendSchedulerTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountActionTokenRepository accountActionTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private RecordingEmailSender emailSender;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        emailSender.clear();
    }

    @Test
    void signupSendsEmailVerificationOnlyAfterCommit() {
        String email = uniqueEmail("signup-after-commit");

        transactionTemplate.executeWithoutResult(status -> {
            authService.signup(new SignupRequest(email, "password123!", "Signup After Commit"));

            assertThat(emailSender.emailVerificationEvents()).isEmpty();
            User pendingUser = userRepository.findByEmail(email).orElseThrow();
            assertThat(accountActionTokenRepository.findByUserId(pendingUser.getId()))
                    .singleElement()
                    .satisfies(token -> assertThat(token.getPurpose())
                            .isEqualTo(AccountActionTokenPurpose.EMAIL_VERIFICATION));
        });

        assertThat(emailSender.emailVerificationEvents())
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.email()).isEqualTo(email);
                    assertThat(event.token()).isNotBlank();
                });
    }

    @Test
    void rollbackDoesNotSendSignupEmailVerification() {
        String email = uniqueEmail("signup-rollback");

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            authService.signup(new SignupRequest(email, "password123!", "Signup Rollback"));
            throw new RollbackProbeException();
        })).isInstanceOf(RollbackProbeException.class);

        assertThat(userRepository.findByEmail(email)).isEmpty();
        assertThat(emailSender.emailVerificationEvents()).isEmpty();
    }

    @Test
    void emailVerificationRequestSendsOnlyAfterCommit() {
        String email = uniqueEmail("verification-request");
        userRepository.save(User.createPasswordSignup(email, passwordEncoder.encode("password123!"), "Verify User"));

        transactionTemplate.executeWithoutResult(status -> {
            authService.requestEmailVerification(new EmailVerificationRequest(email));

            assertThat(emailSender.emailVerificationEvents()).isEmpty();
        });

        assertThat(emailSender.emailVerificationEvents())
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.email()).isEqualTo(email);
                    assertThat(event.token()).isNotBlank();
                });
    }

    @Test
    void passwordResetRequestSendsOnlyAfterCommit() {
        String email = uniqueEmail("password-reset-request");
        userRepository.save(User.create(email, passwordEncoder.encode("password123!"), "Reset User"));

        transactionTemplate.executeWithoutResult(status -> {
            authService.requestPasswordReset(new PasswordResetRequest(email));

            assertThat(emailSender.passwordResetEvents()).isEmpty();
        });

        assertThat(emailSender.passwordResetEvents())
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.email()).isEqualTo(email);
                    assertThat(event.token()).isNotBlank();
                });
    }

    @Test
    void senderFailureAfterCommitDoesNotRollbackSignup() {
        String email = uniqueEmail("sender-failure");
        emailSender.failEmailVerification();

        transactionTemplate.executeWithoutResult(status ->
                authService.signup(new SignupRequest(email, "password123!", "Sender Failure"))
        );

        User savedUser = userRepository.findByEmail(email).orElseThrow();
        assertThat(accountActionTokenRepository.findByUserId(savedUser.getId()))
                .singleElement()
                .satisfies(token -> assertThat(token.getPurpose())
                        .isEqualTo(AccountActionTokenPurpose.EMAIL_VERIFICATION));
        assertThat(emailSender.emailVerificationEvents()).singleElement();
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    @TestConfiguration
    static class EmailSenderConfig {

        @Bean
        @Primary
        RecordingEmailSender recordingEmailSender() {
            return new RecordingEmailSender();
        }
    }

    static class RecordingEmailSender implements EmailSender {

        private final List<EmailEvent> emailVerificationEvents = new ArrayList<>();
        private final List<EmailEvent> passwordResetEvents = new ArrayList<>();
        private boolean failEmailVerification;

        @Override
        public void sendEmailVerification(String email, String token) {
            emailVerificationEvents.add(new EmailEvent(email, token));
            if (failEmailVerification) {
                throw new IllegalStateException("email verification sender failed");
            }
        }

        @Override
        public void sendPasswordReset(String email, String token) {
            passwordResetEvents.add(new EmailEvent(email, token));
        }

        List<EmailEvent> emailVerificationEvents() {
            return emailVerificationEvents;
        }

        List<EmailEvent> passwordResetEvents() {
            return passwordResetEvents;
        }

        void failEmailVerification() {
            failEmailVerification = true;
        }

        void clear() {
            emailVerificationEvents.clear();
            passwordResetEvents.clear();
            failEmailVerification = false;
        }
    }

    record EmailEvent(String email, String token) {
    }

    private static class RollbackProbeException extends RuntimeException {
    }
}

package com.smartcloset.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.smartcloset.auth.dto.SignupRequest;
import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import com.smartcloset.security.JwtTokenProvider;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceUniqueViolationTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AccountOnboardingService accountOnboardingService;
    private AccountActionTokenService accountActionTokenService;
    private AccountEmailSendScheduler accountEmailSendScheduler;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        accountOnboardingService = mock(AccountOnboardingService.class);
        accountActionTokenService = mock(AccountActionTokenService.class);
        accountEmailSendScheduler = mock(AccountEmailSendScheduler.class);
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                mock(JwtTokenProvider.class),
                accountOnboardingService,
                mock(RefreshTokenService.class),
                accountActionTokenService,
                accountEmailSendScheduler,
                mock(AuthProviderService.class)
        );
    }

    @Test
    void signupConvertsUsersEmailUniqueViolationOnFlush() {
        SignupRequest request = new SignupRequest("race@example.com", "password123!", "Race User");
        DataIntegrityViolationException duplicateEmail = new DataIntegrityViolationException(
                "Unique index or primary key violation: PUBLIC.UK_USERS_EMAIL_INDEX ON PUBLIC.USERS(EMAIL)");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(duplicateEmail);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOfSatisfying(SmartClosetException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS));

        verifyNoInteractions(accountOnboardingService, accountActionTokenService, accountEmailSendScheduler);
    }

    @Test
    void signupDoesNotConvertUnrelatedDataIntegrityViolation() {
        SignupRequest request = new SignupRequest("other@example.com", "password123!", "Other User");
        DataIntegrityViolationException unrelated = new DataIntegrityViolationException(
                "Check constraint violation: PUBLIC.CK_USERS_NAME");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(unrelated);

        assertThatThrownBy(() -> authService.signup(request)).isSameAs(unrelated);

        verifyNoInteractions(accountOnboardingService, accountActionTokenService, accountEmailSendScheduler);
    }
}

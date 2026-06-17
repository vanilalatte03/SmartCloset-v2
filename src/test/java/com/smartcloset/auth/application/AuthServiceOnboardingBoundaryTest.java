package com.smartcloset.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.smartcloset.auth.dto.AuthResponse;
import com.smartcloset.auth.dto.LoginRequest;
import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import com.smartcloset.security.JwtTokenProvider;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class AuthServiceOnboardingBoundaryTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AccountOnboardingService accountOnboardingService;
    private RefreshTokenService refreshTokenService;
    private JwtTokenProvider jwtTokenProvider;
    private AuthProviderService authProviderService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        accountOnboardingService = mock(AccountOnboardingService.class);
        refreshTokenService = mock(RefreshTokenService.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        authProviderService = mock(AuthProviderService.class);
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtTokenProvider,
                accountOnboardingService,
                refreshTokenService,
                mock(AccountActionTokenService.class),
                mock(AccountEmailSendScheduler.class),
                authProviderService
        );
    }

    @Test
    void loginDoesNotRunDefaultClothingOnboardingForExistingUser() {
        User user = persistedUser("login-boundary@example.com");
        LoginRequest request = new LoginRequest(user.getEmail(), "password123!");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(any())).thenReturn("access-token");
        when(authProviderService.providersFor(user)).thenReturn(List.of("PASSWORD"));

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.user().email()).isEqualTo(user.getEmail());
        verifyNoInteractions(accountOnboardingService);
    }

    @Test
    void loginRunsDummyPasswordMatchWhenEmailIsMissing() {
        LoginRequest request = new LoginRequest("missing-login@example.com", "password123!");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOfSatisfying(SmartClosetException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));

        verify(passwordEncoder).matches(eq(request.password()), eq(AuthService.DUMMY_PASSWORD_HASH));
        verifyNoInteractions(accountOnboardingService, refreshTokenService, jwtTokenProvider, authProviderService);
    }

    @Test
    void dummyPasswordHashIsValidBcryptHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        assertThat(encoder.matches("smartcloset-dummy-password", AuthService.DUMMY_PASSWORD_HASH)).isTrue();
    }

    @Test
    void refreshDoesNotRunDefaultClothingOnboarding() {
        User user = persistedUser("refresh-boundary@example.com");
        when(refreshTokenService.rotate("refresh-token"))
                .thenReturn(new RefreshTokenService.RotatedRefreshToken(user, "next-refresh-token"));
        when(jwtTokenProvider.createAccessToken(any())).thenReturn("refreshed-access-token");
        when(authProviderService.providersFor(user)).thenReturn(List.of("PASSWORD"));

        RefreshTokenBundle bundle = authService.refresh("refresh-token");

        assertThat(bundle.authResponse().accessToken()).isEqualTo("refreshed-access-token");
        assertThat(bundle.refreshToken()).isEqualTo("next-refresh-token");
        verifyNoInteractions(accountOnboardingService);
    }

    private User persistedUser(String email) {
        User user = User.create(email, "password-hash", "Boundary User");
        ReflectionTestUtils.setField(user, "id", 180L);
        return user;
    }
}

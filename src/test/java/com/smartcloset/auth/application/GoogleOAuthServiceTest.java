package com.smartcloset.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smartcloset.auth.domain.OAuthProvider;
import com.smartcloset.auth.infrastructure.GoogleOAuthClient;
import com.smartcloset.auth.infrastructure.GoogleOAuthProperties;
import com.smartcloset.auth.infrastructure.GoogleUserProfile;
import com.smartcloset.auth.repository.SocialAccountRepository;
import com.smartcloset.auth.repository.RefreshSessionRepository;
import com.smartcloset.clothing.application.DefaultClothingPresetSeeder;
import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import com.smartcloset.security.JwtTokenProvider;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class GoogleOAuthServiceTest {

    private FakeGoogleOAuthClient googleOAuthClient;
    private GoogleOAuthService googleOAuthService;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DefaultClothingPresetSeeder defaultClothingPresetSeeder;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshSessionRepository refreshSessionRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        googleOAuthClient = new FakeGoogleOAuthClient();
        googleOAuthService = new GoogleOAuthService(
                enabledProperties(),
                googleOAuthClient,
                socialAccountRepository,
                userRepository,
                defaultClothingPresetSeeder,
                refreshTokenService,
                jwtTokenProvider,
                transactionManager,
                Clock.fixed(Instant.parse("2026-05-27T03:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void callbackCreatesGoogleOnlyUserWithVerifiedEmailAndRefreshSession() {
        googleOAuthClient.profile("new-code", new GoogleUserProfile(
                "google-sub-1",
                "google-new@example.com",
                true,
                "Google New"
        ));

        RefreshTokenBundle bundle = googleOAuthService.callback("new-code");

        User user = userRepository.findByEmail("google-new@example.com").orElseThrow();
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.isPasswordLoginEnabled()).isFalse();
        assertThat(bundle.authResponse().accessToken()).isNotBlank();
        assertThat(bundle.authResponse().user().authProviders()).containsExactly("GOOGLE");
        assertThat(bundle.refreshToken()).isNotBlank();
        assertThat(refreshSessionRepository.findAll())
                .singleElement()
                .satisfies(session -> {
                    assertThat(session.getUser().getId()).isEqualTo(user.getId());
                    assertThat(session.getTokenHash()).isNotEqualTo(bundle.refreshToken());
                    assertThat(session.getRevokedAt()).isNull();
                });
        assertThat(socialAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "google-sub-1"))
                .isPresent()
                .get()
                .satisfies(account -> {
                    assertThat(account.getUser().getId()).isEqualTo(user.getId());
                    assertThat(account.getEmail()).isEqualTo("google-new@example.com");
                });
    }

    @Test
    void callbackLinksExistingEmailUserAndMarksEmailVerified() {
        User existing = userRepository.save(User.createPasswordSignup(
                "google-existing@example.com",
                passwordEncoder.encode("password123!"),
                "Existing User"
        ));
        googleOAuthClient.profile("existing-code", new GoogleUserProfile(
                "google-sub-2",
                "google-existing@example.com",
                true,
                "Google Existing"
        ));

        RefreshTokenBundle bundle = googleOAuthService.callback("existing-code");

        User linked = userRepository.findByEmail("google-existing@example.com").orElseThrow();
        assertThat(linked.getId()).isEqualTo(existing.getId());
        assertThat(linked.isEmailVerified()).isTrue();
        assertThat(linked.isPasswordLoginEnabled()).isTrue();
        assertThat(bundle.authResponse().user().authProviders()).containsExactly("PASSWORD", "GOOGLE");
        assertThat(socialAccountRepository.existsByUserIdAndProvider(existing.getId(), OAuthProvider.GOOGLE)).isTrue();
    }

    @Test
    void callbackRejectsGoogleProfileWithoutVerifiedEmail() {
        googleOAuthClient.profile("unverified-code", new GoogleUserProfile(
                "google-sub-3",
                "google-unverified@example.com",
                false,
                "Google Unverified"
        ));

        assertThatThrownBy(() -> googleOAuthService.callback("unverified-code"))
                .isInstanceOf(SmartClosetException.class)
                .extracting(exception -> ((SmartClosetException) exception).errorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
        assertThat(userRepository.findByEmail("google-unverified@example.com")).isEmpty();
    }

    private GoogleOAuthProperties enabledProperties() {
        return new GoogleOAuthProperties(
                "http://localhost:5173/auth/callback",
                new GoogleOAuthProperties.Google(
                        "google-client-id",
                        "google-client-secret",
                        "http://localhost:8080/api/auth/oauth2/callback/google",
                        "https://accounts.google.com/o/oauth2/v2/auth",
                        "https://oauth2.googleapis.com/token",
                        "https://openidconnect.googleapis.com/v1/userinfo",
                        null,
                        null
                )
        );
    }

    private static class FakeGoogleOAuthClient extends GoogleOAuthClient {

        private final Map<String, GoogleUserProfile> profiles = new HashMap<>();

        FakeGoogleOAuthClient() {
            super();
        }

        void profile(String code, GoogleUserProfile profile) {
            profiles.put(code, profile);
        }

        @Override
        public GoogleUserProfile fetchUserProfile(String code, GoogleOAuthProperties properties) {
            return profiles.get(code);
        }
    }
}

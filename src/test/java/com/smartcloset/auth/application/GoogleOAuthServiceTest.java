package com.smartcloset.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloset.auth.domain.OAuthProvider;
import com.smartcloset.auth.domain.SocialAccount;
import com.smartcloset.auth.infrastructure.GoogleOAuthClient;
import com.smartcloset.auth.infrastructure.GoogleOAuthProperties;
import com.smartcloset.auth.infrastructure.GoogleUserProfile;
import com.smartcloset.auth.repository.RefreshSessionRepository;
import com.smartcloset.auth.repository.SocialAccountRepository;
import com.smartcloset.clothing.application.DefaultClothingPresetSeeder;
import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import com.smartcloset.security.JwtTokenProvider;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.SimpleTransactionStatus;

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
        assertThat(refreshSessionRepository.findByUserIdAndRevokedAtIsNull(user.getId()))
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

    @Test
    void callbackRetriesAsLoginWhenSocialAccountUniqueConflictOccurs() {
        FakeGoogleOAuthClient googleOAuthClient = new FakeGoogleOAuthClient();
        GoogleUserProfile profile = new GoogleUserProfile(
                "google-sub-social-conflict",
                "google-social-conflict@example.com",
                true,
                "Google Social Conflict"
        );
        googleOAuthClient.profile("social-conflict-code", profile);
        SocialAccountRepository socialAccountRepository = mock(SocialAccountRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        DefaultClothingPresetSeeder defaultClothingPresetSeeder = mock(DefaultClothingPresetSeeder.class);
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        User existing = User.createGoogleUser(profile.email(), profile.name());
        ReflectionTestUtils.setField(existing, "id", 17901L);
        SocialAccount existingAccount = SocialAccount.link(
                existing,
                OAuthProvider.GOOGLE,
                profile.sub(),
                profile.email(),
                LocalDateTime.parse("2026-05-27T03:00:00")
        );
        when(socialAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, profile.sub()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingAccount));
        when(userRepository.findByEmail(profile.email())).thenReturn(Optional.of(existing));
        when(socialAccountRepository.save(any(SocialAccount.class)))
                .thenThrow(new DataIntegrityViolationException("uk_social_accounts_provider_user duplicate"));
        when(refreshTokenService.issue(existing)).thenReturn(new RefreshTokenService.IssuedRefreshToken(
                "retry-refresh-token",
                "retry-refresh-token-hash"
        ));
        when(jwtTokenProvider.createAccessToken(any())).thenReturn("retry-access-token");
        GoogleOAuthService service = newGoogleOAuthService(
                googleOAuthClient,
                socialAccountRepository,
                userRepository,
                defaultClothingPresetSeeder,
                refreshTokenService,
                jwtTokenProvider
        );

        RefreshTokenBundle bundle = service.callback("social-conflict-code");

        assertThat(bundle.authResponse().accessToken()).isEqualTo("retry-access-token");
        assertThat(bundle.authResponse().user().authProviders()).containsExactly("GOOGLE");
        assertThat(bundle.refreshToken()).isEqualTo("retry-refresh-token");
        verify(defaultClothingPresetSeeder).seedIfEmpty(existing);
        verify(refreshTokenService).issue(existing);
    }

    @Test
    void callbackRetriesByEmailWhenUserEmailUniqueConflictOccurs() {
        FakeGoogleOAuthClient googleOAuthClient = new FakeGoogleOAuthClient();
        GoogleUserProfile profile = new GoogleUserProfile(
                "google-sub-email-conflict",
                "google-email-conflict@example.com",
                true,
                "Google Email Conflict"
        );
        googleOAuthClient.profile("email-conflict-code", profile);
        SocialAccountRepository socialAccountRepository = mock(SocialAccountRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        DefaultClothingPresetSeeder defaultClothingPresetSeeder = mock(DefaultClothingPresetSeeder.class);
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        User existing = User.createGoogleUser(profile.email(), profile.name());
        ReflectionTestUtils.setField(existing, "id", 17902L);
        SocialAccount linkedAccount = SocialAccount.link(
                existing,
                OAuthProvider.GOOGLE,
                profile.sub(),
                profile.email(),
                LocalDateTime.parse("2026-05-27T03:00:00")
        );
        when(socialAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, profile.sub()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail(profile.email()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("uk_users_email duplicate"));
        when(socialAccountRepository.save(any(SocialAccount.class))).thenReturn(linkedAccount);
        when(refreshTokenService.issue(existing)).thenReturn(new RefreshTokenService.IssuedRefreshToken(
                "email-retry-refresh-token",
                "email-retry-refresh-token-hash"
        ));
        when(jwtTokenProvider.createAccessToken(any())).thenReturn("email-retry-access-token");
        GoogleOAuthService service = newGoogleOAuthService(
                googleOAuthClient,
                socialAccountRepository,
                userRepository,
                defaultClothingPresetSeeder,
                refreshTokenService,
                jwtTokenProvider
        );

        RefreshTokenBundle bundle = service.callback("email-conflict-code");

        assertThat(bundle.authResponse().accessToken()).isEqualTo("email-retry-access-token");
        assertThat(bundle.refreshToken()).isEqualTo("email-retry-refresh-token");
        verify(defaultClothingPresetSeeder).seedIfEmpty(existing);
        verify(refreshTokenService).issue(existing);
        verify(socialAccountRepository).save(any(SocialAccount.class));
    }

    @Test
    void callbackDoesNotRetryUnrelatedDataIntegrityViolation() {
        FakeGoogleOAuthClient googleOAuthClient = new FakeGoogleOAuthClient();
        GoogleUserProfile profile = new GoogleUserProfile(
                "google-sub-unrelated-conflict",
                "google-unrelated-conflict@example.com",
                true,
                "Google Unrelated Conflict"
        );
        googleOAuthClient.profile("unrelated-conflict-code", profile);
        SocialAccountRepository socialAccountRepository = mock(SocialAccountRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        DefaultClothingPresetSeeder defaultClothingPresetSeeder = mock(DefaultClothingPresetSeeder.class);
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        User existing = User.createGoogleUser(profile.email(), profile.name());
        ReflectionTestUtils.setField(existing, "id", 17903L);
        when(socialAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, profile.sub()))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail(profile.email())).thenReturn(Optional.of(existing));
        when(socialAccountRepository.save(any(SocialAccount.class)))
                .thenThrow(new DataIntegrityViolationException("fk_refresh_sessions_user violation"));
        GoogleOAuthService service = newGoogleOAuthService(
                googleOAuthClient,
                socialAccountRepository,
                userRepository,
                defaultClothingPresetSeeder,
                refreshTokenService,
                jwtTokenProvider
        );

        assertThatThrownBy(() -> service.callback("unrelated-conflict-code"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_refresh_sessions_user");
        verify(defaultClothingPresetSeeder, never()).seedIfEmpty(any());
        verify(refreshTokenService, never()).issue(any());
    }

    private GoogleOAuthService newGoogleOAuthService(
            GoogleOAuthClient googleOAuthClient,
            SocialAccountRepository socialAccountRepository,
            UserRepository userRepository,
            DefaultClothingPresetSeeder defaultClothingPresetSeeder,
            RefreshTokenService refreshTokenService,
            JwtTokenProvider jwtTokenProvider
    ) {
        return new GoogleOAuthService(
                enabledProperties(),
                googleOAuthClient,
                socialAccountRepository,
                userRepository,
                defaultClothingPresetSeeder,
                refreshTokenService,
                jwtTokenProvider,
                new NoOpTransactionManager(),
                Clock.fixed(Instant.parse("2026-05-27T03:00:00Z"), ZoneOffset.UTC)
        );
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

    private static class NoOpTransactionManager implements PlatformTransactionManager {

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }
}

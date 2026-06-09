package com.smartcloset.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.smartcloset.auth.domain.OAuthProvider;
import com.smartcloset.auth.infrastructure.GoogleOAuthClient;
import com.smartcloset.auth.infrastructure.GoogleOAuthProperties;
import com.smartcloset.auth.infrastructure.GoogleUserProfile;
import com.smartcloset.auth.repository.RefreshSessionRepository;
import com.smartcloset.auth.repository.SocialAccountRepository;
import com.smartcloset.clothing.application.DefaultClothingPresetSeeder;
import com.smartcloset.security.JwtTokenProvider;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ActiveProfiles("test")
@SpringBootTest
class GoogleOAuthTransactionBoundaryTest {

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DefaultClothingPresetSeeder defaultClothingPresetSeeder;

    @Autowired
    private RefreshSessionRepository refreshSessionRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Value("${smartcloset.security.jwt.secret}")
    private String jwtSecret;

    @Value("${smartcloset.security.refresh-token.ttl-days}")
    private long refreshTokenTtlDays;

    @Test
    void callbackFetchesGoogleProfileOutsideTransactionAndIssuesRefreshInsideWriteTransaction() {
        String suffix = UUID.randomUUID().toString();
        String email = "google-boundary-" + suffix + "@example.com";
        String providerUserId = "google-boundary-" + suffix;
        RecordingGoogleOAuthClient googleOAuthClient = new RecordingGoogleOAuthClient(new GoogleUserProfile(
                providerUserId,
                email,
                true,
                "Google Boundary"
        ));
        RecordingRefreshTokenService refreshTokenService = new RecordingRefreshTokenService(
                refreshSessionRepository,
                jwtSecret,
                refreshTokenTtlDays
        );
        GoogleOAuthService googleOAuthService = new GoogleOAuthService(
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

        RefreshTokenBundle bundle = googleOAuthService.callback("boundary-code");

        assertThat(bundle.authResponse().accessToken()).isNotBlank();
        assertThat(bundle.refreshToken()).isNotBlank();
        assertThat(googleOAuthClient.fetchTransactionActive()).isFalse();
        assertThat(refreshTokenService.issueTransactionActive()).isTrue();
        User user = userRepository.findByEmail(email).orElseThrow();
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(socialAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, providerUserId))
                .isPresent();
        assertThat(refreshSessionRepository.findByUserIdAndRevokedAtIsNull(user.getId()))
                .singleElement()
                .satisfies(session -> assertThat(session.getUser().getId()).isEqualTo(user.getId()));
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

    private static class RecordingGoogleOAuthClient extends GoogleOAuthClient {

        private final GoogleUserProfile profile;
        private Boolean fetchTransactionActive;

        RecordingGoogleOAuthClient(GoogleUserProfile profile) {
            super();
            this.profile = profile;
        }

        @Override
        public GoogleUserProfile fetchUserProfile(String code, GoogleOAuthProperties properties) {
            fetchTransactionActive = TransactionSynchronizationManager.isActualTransactionActive();
            return profile;
        }

        Boolean fetchTransactionActive() {
            return fetchTransactionActive;
        }
    }

    private static class RecordingRefreshTokenService extends RefreshTokenService {

        private Boolean issueTransactionActive;

        RecordingRefreshTokenService(RefreshSessionRepository refreshSessionRepository, String secret, long ttlDays) {
            super(
                    refreshSessionRepository,
                    secret,
                    ttlDays,
                    new SecureRandom(),
                    Clock.fixed(Instant.parse("2026-05-27T03:00:00Z"), ZoneOffset.UTC)
            );
        }

        @Override
        public IssuedRefreshToken issue(User user) {
            issueTransactionActive = TransactionSynchronizationManager.isActualTransactionActive();
            return super.issue(user);
        }

        Boolean issueTransactionActive() {
            return issueTransactionActive;
        }
    }
}

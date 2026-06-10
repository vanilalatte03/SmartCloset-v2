package com.smartcloset.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.smartcloset.auth.domain.OAuthProvider;
import com.smartcloset.auth.domain.SocialAccount;
import com.smartcloset.auth.infrastructure.GoogleOAuthClient;
import com.smartcloset.auth.infrastructure.GoogleOAuthProperties;
import com.smartcloset.auth.infrastructure.GoogleUserProfile;
import com.smartcloset.auth.repository.RefreshSessionRepository;
import com.smartcloset.auth.repository.SocialAccountRepository;
import com.smartcloset.clothing.application.DefaultClothingPresetSeeder;
import com.smartcloset.security.JwtTokenProvider;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

@ActiveProfiles("test")
@SpringBootTest
class GoogleOAuthConcurrencyTest {

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

    @Test
    @DirtiesContext
    void concurrentCallbacksForSameGoogleProfileConvergeToSingleAccountAndIssueSessions() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String email = "google-concurrent-" + suffix + "@example.com";
        String providerUserId = "google-concurrent-" + suffix;
        BarrierGoogleOAuthClient googleOAuthClient = new BarrierGoogleOAuthClient(new GoogleUserProfile(
                providerUserId,
                email,
                true,
                "Google Concurrent"
        ));
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
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Callable<RefreshTokenBundle> callback = () -> googleOAuthService.callback("concurrent-code");
            Future<RefreshTokenBundle> first = executor.submit(callback);
            Future<RefreshTokenBundle> second = executor.submit(callback);

            List<RefreshTokenBundle> bundles = List.of(
                    first.get(10, TimeUnit.SECONDS),
                    second.get(10, TimeUnit.SECONDS)
            );

            User user = userRepository.findByEmail(email).orElseThrow();
            assertThat(user.isEmailVerified()).isTrue();
            assertThat(user.isPasswordLoginEnabled()).isFalse();
            assertThat(bundles).allSatisfy(bundle -> {
                assertThat(bundle.authResponse().accessToken()).isNotBlank();
                assertThat(bundle.authResponse().user().email()).isEqualTo(email);
                assertThat(bundle.authResponse().user().authProviders()).containsExactly("GOOGLE");
                assertThat(bundle.refreshToken()).isNotBlank();
            });
            assertThat(userRepository.findAll().stream()
                    .filter(candidate -> candidate.getEmail().equals(email)))
                    .hasSize(1);
            assertThat(socialAccountRepository.findAll().stream()
                    .filter(account -> account.getProvider() == OAuthProvider.GOOGLE)
                    .filter(account -> account.getProviderUserId().equals(providerUserId)))
                    .extracting(SocialAccount::getUser)
                    .extracting(User::getId)
                    .containsExactly(user.getId());
            assertThat(refreshSessionRepository.findByUserIdAndRevokedAtIsNull(user.getId()))
                    .hasSize(2);
        } finally {
            executor.shutdownNow();
        }
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

    private static class BarrierGoogleOAuthClient extends GoogleOAuthClient {

        private final CyclicBarrier barrier = new CyclicBarrier(2);
        private final GoogleUserProfile profile;

        BarrierGoogleOAuthClient(GoogleUserProfile profile) {
            super();
            this.profile = profile;
        }

        @Override
        public GoogleUserProfile fetchUserProfile(String code, GoogleOAuthProperties properties) {
            try {
                barrier.await(5, TimeUnit.SECONDS);
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to align concurrent OAuth callbacks", exception);
            }
            return profile;
        }
    }
}

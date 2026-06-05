package com.smartcloset.auth.application;

import com.smartcloset.auth.domain.OAuthProvider;
import com.smartcloset.auth.domain.SocialAccount;
import com.smartcloset.auth.dto.OAuthProvidersResponse;
import com.smartcloset.auth.infrastructure.GoogleOAuthClient;
import com.smartcloset.auth.infrastructure.GoogleOAuthProperties;
import com.smartcloset.auth.infrastructure.GoogleUserProfile;
import com.smartcloset.auth.repository.SocialAccountRepository;
import com.smartcloset.clothing.application.DefaultClothingPresetSeeder;
import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import com.smartcloset.security.CurrentUserPrincipal;
import com.smartcloset.security.JwtTokenProvider;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.dto.CurrentUserResponse;
import com.smartcloset.user.repository.UserRepository;
import java.net.URI;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Google OAuth 로그인 URL 생성과 callback 처리를 담당한다.
 *
 * <p>Google에서 verified email을 확인한 프로필만 계정 생성/연결에 사용하며,
 * 기존 password 계정과 같은 이메일이면 Google social account를 연결한다.</p>
 */
@Service
public class GoogleOAuthService {

    private static final String GOOGLE_LOGIN_PATH = "/api/auth/oauth2/google";
    private static final int OAUTH_STATE_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final GoogleOAuthProperties properties;
    private final GoogleOAuthClient googleOAuthClient;
    private final SocialAccountRepository socialAccountRepository;
    private final UserRepository userRepository;
    private final DefaultClothingPresetSeeder defaultClothingPresetSeeder;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;
    private final Clock clock;

    @Autowired
    public GoogleOAuthService(
            GoogleOAuthProperties properties,
            GoogleOAuthClient googleOAuthClient,
            SocialAccountRepository socialAccountRepository,
            UserRepository userRepository,
            DefaultClothingPresetSeeder defaultClothingPresetSeeder,
            RefreshTokenService refreshTokenService,
            JwtTokenProvider jwtTokenProvider
    ) {
        this(properties, googleOAuthClient, socialAccountRepository, userRepository, defaultClothingPresetSeeder,
                refreshTokenService, jwtTokenProvider, Clock.systemUTC());
    }

    GoogleOAuthService(
            GoogleOAuthProperties properties,
            GoogleOAuthClient googleOAuthClient,
            SocialAccountRepository socialAccountRepository,
            UserRepository userRepository,
            DefaultClothingPresetSeeder defaultClothingPresetSeeder,
            RefreshTokenService refreshTokenService,
            JwtTokenProvider jwtTokenProvider,
            Clock clock
    ) {
        this.properties = properties;
        this.googleOAuthClient = googleOAuthClient;
        this.socialAccountRepository = socialAccountRepository;
        this.userRepository = userRepository;
        this.defaultClothingPresetSeeder = defaultClothingPresetSeeder;
        this.refreshTokenService = refreshTokenService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Google OAuth 설정 상태와 프론트가 사용할 로그인 시작 path를 반환한다.
     */
    public OAuthProvidersResponse providers() {
        return OAuthProvidersResponse.google(properties.googleEnabled(), GOOGLE_LOGIN_PATH);
    }

    /**
     * OAuth callback 검증용 state 값을 예측 불가능한 URL-safe 문자열로 생성한다.
     */
    public String createState() {
        byte[] bytes = new byte[OAUTH_STATE_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * controller가 발급한 state 값을 Google authorization URL에 넣어 CSRF를 방지한다.
     */
    public URI authorizationUri(String state) {
        ensureGoogleEnabled();
        if (state == null || state.isBlank()) {
            throw new SmartClosetException(ErrorCode.INVALID_REQUEST);
        }
        GoogleOAuthProperties.Google google = properties.google();
        String scope = String.join(" ", properties.scopes());
        return UriComponentsBuilder.fromUriString(google.authorizationUri())
                .queryParam("response_type", "code")
                .queryParam("client_id", google.clientId())
                .queryParam("redirect_uri", google.redirectUri())
                .queryParam("scope", scope)
                .queryParam("state", state)
                .build()
                .encode()
                .toUri();
    }

    /**
     * authorization code를 Google profile로 교환한 뒤 SmartCloset 세션 token 쌍을 발급한다.
     */
    @Transactional
    public RefreshTokenBundle callback(String code) {
        ensureGoogleEnabled();
        GoogleUserProfile profile = googleOAuthClient.fetchUserProfile(code, properties);
        User user = findOrCreateVerifiedGoogleUser(profile);
        defaultClothingPresetSeeder.seedIfEmpty(user);
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issue(user);
        CurrentUserPrincipal principal = new CurrentUserPrincipal(user.getId(), user.getEmail(), user.getRole());
        String accessToken = jwtTokenProvider.createAccessToken(principal);
        List<String> providers = authProvidersForGoogleUser(user);
        return new RefreshTokenBundle(
                com.smartcloset.auth.dto.AuthResponse.bearer(
                        accessToken,
                        CurrentUserResponse.from(user, providers)
                ),
                refreshToken.refreshToken()
        );
    }

    /**
     * OAuth callback 성공 후 브라우저를 돌려보낼 프론트 callback URI를 반환한다.
     */
    public URI frontendCallbackUri() {
        return URI.create(properties.frontendCallbackUrl());
    }

    private User findOrCreateVerifiedGoogleUser(GoogleUserProfile profile) {
        validateVerifiedProfile(profile);
        return socialAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, profile.sub())
                .map(SocialAccount::getUser)
                .orElseGet(() -> linkByEmailOrCreate(profile));
    }

    /**
     * 같은 이메일의 local 계정이 있으면 인증 완료로 표시하고 Google provider를 연결한다.
     */
    private User linkByEmailOrCreate(GoogleUserProfile profile) {
        User user = userRepository.findByEmail(profile.email())
                .map(existing -> {
                    existing.markEmailVerified();
                    return existing;
                })
                .orElseGet(() -> userRepository.save(User.createGoogleUser(profile.email(), profileName(profile))));
        socialAccountRepository.save(SocialAccount.link(
                user,
                OAuthProvider.GOOGLE,
                profile.sub(),
                profile.email(),
                LocalDateTime.now(clock)
        ));
        return user;
    }

    private List<String> authProvidersForGoogleUser(User user) {
        if (user.isPasswordLoginEnabled()) {
            return List.of("PASSWORD", "GOOGLE");
        }
        return List.of("GOOGLE");
    }

    private String profileName(GoogleUserProfile profile) {
        if (profile.name() != null && !profile.name().isBlank()) {
            return profile.name();
        }
        return profile.email();
    }

    private void validateVerifiedProfile(GoogleUserProfile profile) {
        if (profile == null
                || profile.sub() == null
                || profile.sub().isBlank()
                || profile.email() == null
                || profile.email().isBlank()
                || !Boolean.TRUE.equals(profile.emailVerified())) {
            throw new SmartClosetException(ErrorCode.UNAUTHORIZED);
        }
    }

    private void ensureGoogleEnabled() {
        if (!properties.googleEnabled()) {
            throw new SmartClosetException(ErrorCode.OAUTH2_PROVIDER_UNAVAILABLE);
        }
    }
}

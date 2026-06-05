package com.smartcloset.auth.application;

import com.smartcloset.auth.domain.AccountActionTokenPurpose;
import com.smartcloset.auth.dto.AuthResponse;
import com.smartcloset.auth.dto.EmailVerificationConfirmRequest;
import com.smartcloset.auth.dto.EmailVerificationConfirmResponse;
import com.smartcloset.auth.dto.EmailVerificationRequest;
import com.smartcloset.auth.dto.EmailVerificationRequestedResponse;
import com.smartcloset.auth.dto.LoginRequest;
import com.smartcloset.auth.dto.PasswordResetConfirmRequest;
import com.smartcloset.auth.dto.PasswordResetConfirmResponse;
import com.smartcloset.auth.dto.PasswordResetRequest;
import com.smartcloset.auth.dto.PasswordResetRequestedResponse;
import com.smartcloset.auth.dto.SignupRequest;
import com.smartcloset.auth.dto.SignupResponse;
import com.smartcloset.auth.infrastructure.EmailSender;
import com.smartcloset.clothing.application.DefaultClothingPresetSeeder;
import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import com.smartcloset.security.CurrentUserPrincipal;
import com.smartcloset.security.JwtTokenProvider;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.dto.CurrentUserResponse;
import com.smartcloset.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 비밀번호 기반 인증과 계정 액션 토큰 흐름을 조율한다.
 *
 * <p>Access token은 응답 body에 담고, refresh token은 controller에서 HttpOnly cookie로만
 * 전달되도록 {@link RefreshTokenBundle}에 분리해 반환한다.</p>
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final DefaultClothingPresetSeeder defaultClothingPresetSeeder;
    private final RefreshTokenService refreshTokenService;
    private final AccountActionTokenService accountActionTokenService;
    private final EmailSender emailSender;
    private final AuthProviderService authProviderService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            DefaultClothingPresetSeeder defaultClothingPresetSeeder,
            RefreshTokenService refreshTokenService,
            AccountActionTokenService accountActionTokenService,
            EmailSender emailSender,
            AuthProviderService authProviderService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.defaultClothingPresetSeeder = defaultClothingPresetSeeder;
        this.refreshTokenService = refreshTokenService;
        this.accountActionTokenService = accountActionTokenService;
        this.emailSender = emailSender;
        this.authProviderService = authProviderService;
    }

    /**
     * password signup은 이메일 인증 전까지 access token을 발급하지 않는다.
     */
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new SmartClosetException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = userRepository.save(User.createPasswordSignup(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.name()
        ));
        defaultClothingPresetSeeder.seedIfEmpty(user);
        issueEmailVerification(user);
        return SignupResponse.emailVerificationRequired(user.getEmail());
    }

    /**
     * 로그인은 비밀번호, password login 가능 여부, 이메일 인증 여부를 모두 통과해야 한다.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new SmartClosetException(ErrorCode.UNAUTHORIZED));
        if (!user.isPasswordLoginEnabled()) {
            throw new SmartClosetException(ErrorCode.PASSWORD_LOGIN_DISABLED);
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new SmartClosetException(ErrorCode.UNAUTHORIZED);
        }
        if (!user.isEmailVerified()) {
            throw new SmartClosetException(ErrorCode.EMAIL_VERIFICATION_REQUIRED);
        }
        defaultClothingPresetSeeder.seedIfEmpty(user);
        return authResponse(user);
    }

    /**
     * refresh token cookie 발급이 필요한 login endpoint에서 사용하는 경로다.
     */
    @Transactional
    public RefreshTokenBundle loginWithRefreshSession(LoginRequest request) {
        AuthResponse response = login(request);
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new SmartClosetException(ErrorCode.UNAUTHORIZED));
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issue(user);
        return new RefreshTokenBundle(response, refreshToken.refreshToken());
    }

    /**
     * refresh는 token rotation을 수행하므로 새 refresh token을 함께 반환한다.
     */
    @Transactional
    public RefreshTokenBundle refresh(String refreshToken) {
        RefreshTokenService.RotatedRefreshToken rotated = refreshTokenService.rotate(refreshToken);
        User user = rotated.user();
        defaultClothingPresetSeeder.seedIfEmpty(user);
        return new RefreshTokenBundle(authResponse(user), rotated.refreshToken());
    }

    /**
     * 전달된 refresh token이 있으면 해당 session을 revoke한다. 빈 token은 logout 멱등성을 위해 무시한다.
     */
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revokeIfPresent(refreshToken);
        }
    }

    /**
     * 존재하지 않거나 이미 인증된 이메일에도 같은 성공 응답을 돌려 계정 존재 여부를 숨긴다.
     */
    @Transactional
    public EmailVerificationRequestedResponse requestEmailVerification(EmailVerificationRequest request) {
        userRepository.findByEmail(request.email())
                .filter(user -> !user.isEmailVerified())
                .ifPresent(this::issueEmailVerification);
        return EmailVerificationRequestedResponse.success();
    }

    /**
     * 이메일 인증 token을 single-use로 소비하고 계정을 인증 완료 상태로 전환한다.
     */
    @Transactional
    public EmailVerificationConfirmResponse confirmEmailVerification(EmailVerificationConfirmRequest request) {
        User user = accountActionTokenService.consume(
                request.token(),
                AccountActionTokenPurpose.EMAIL_VERIFICATION
        );
        user.markEmailVerified();
        return EmailVerificationConfirmResponse.success();
    }

    /**
     * 비밀번호 재설정 요청도 계정 존재 여부가 드러나지 않도록 항상 동일한 성공 응답을 사용한다.
     */
    @Transactional
    public PasswordResetRequestedResponse requestPasswordReset(PasswordResetRequest request) {
        userRepository.findByEmail(request.email())
                .filter(User::isPasswordLoginEnabled)
                .ifPresent(this::issuePasswordReset);
        return PasswordResetRequestedResponse.success();
    }

    /**
     * 비밀번호가 바뀌면 기존 refresh session을 모두 폐기해 오래된 세션을 끊는다.
     */
    @Transactional
    public PasswordResetConfirmResponse confirmPasswordReset(PasswordResetConfirmRequest request) {
        User user = accountActionTokenService.consume(request.token(), AccountActionTokenPurpose.PASSWORD_RESET);
        if (!user.isPasswordLoginEnabled()) {
            throw new SmartClosetException(ErrorCode.PASSWORD_LOGIN_DISABLED);
        }
        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenService.revokeAll(user);
        return PasswordResetConfirmResponse.success();
    }

    private AuthResponse authResponse(User user) {
        CurrentUserPrincipal principal = new CurrentUserPrincipal(user.getId(), user.getEmail(), user.getRole());
        String accessToken = jwtTokenProvider.createAccessToken(principal);
        return AuthResponse.bearer(accessToken, CurrentUserResponse.from(user, authProviderService.providersFor(user)));
    }

    private void issueEmailVerification(User user) {
        AccountActionTokenService.IssuedAccountActionToken token =
                accountActionTokenService.issue(user, AccountActionTokenPurpose.EMAIL_VERIFICATION);
        emailSender.sendEmailVerification(user.getEmail(), token.token());
    }

    private void issuePasswordReset(User user) {
        AccountActionTokenService.IssuedAccountActionToken token =
                accountActionTokenService.issue(user, AccountActionTokenPurpose.PASSWORD_RESET);
        emailSender.sendPasswordReset(user.getEmail(), token.token());
    }
}

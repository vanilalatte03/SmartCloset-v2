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

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final DefaultClothingPresetSeeder defaultClothingPresetSeeder;
    private final RefreshTokenService refreshTokenService;
    private final AccountActionTokenService accountActionTokenService;
    private final EmailSender emailSender;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            DefaultClothingPresetSeeder defaultClothingPresetSeeder,
            RefreshTokenService refreshTokenService,
            AccountActionTokenService accountActionTokenService,
            EmailSender emailSender
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.defaultClothingPresetSeeder = defaultClothingPresetSeeder;
        this.refreshTokenService = refreshTokenService;
        this.accountActionTokenService = accountActionTokenService;
        this.emailSender = emailSender;
    }

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

    @Transactional
    public RefreshTokenBundle loginWithRefreshSession(LoginRequest request) {
        AuthResponse response = login(request);
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new SmartClosetException(ErrorCode.UNAUTHORIZED));
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issue(user);
        return new RefreshTokenBundle(response, refreshToken.refreshToken());
    }

    @Transactional
    public RefreshTokenBundle refresh(String refreshToken) {
        RefreshTokenService.RotatedRefreshToken rotated = refreshTokenService.rotate(refreshToken);
        return new RefreshTokenBundle(authResponse(rotated.user()), rotated.refreshToken());
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revokeIfPresent(refreshToken);
        }
    }

    @Transactional
    public EmailVerificationRequestedResponse requestEmailVerification(EmailVerificationRequest request) {
        userRepository.findByEmail(request.email())
                .filter(user -> !user.isEmailVerified())
                .ifPresent(this::issueEmailVerification);
        return EmailVerificationRequestedResponse.success();
    }

    @Transactional
    public EmailVerificationConfirmResponse confirmEmailVerification(EmailVerificationConfirmRequest request) {
        User user = accountActionTokenService.consume(
                request.token(),
                AccountActionTokenPurpose.EMAIL_VERIFICATION
        );
        user.markEmailVerified();
        return EmailVerificationConfirmResponse.success();
    }

    @Transactional
    public PasswordResetRequestedResponse requestPasswordReset(PasswordResetRequest request) {
        userRepository.findByEmail(request.email())
                .filter(User::isPasswordLoginEnabled)
                .ifPresent(this::issuePasswordReset);
        return PasswordResetRequestedResponse.success();
    }

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
        return AuthResponse.bearer(accessToken, CurrentUserResponse.from(user));
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

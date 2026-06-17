package com.smartcloset.auth.controller;

import com.smartcloset.auth.application.AuthService;
import com.smartcloset.auth.application.LoginAttemptThrottle;
import com.smartcloset.auth.application.RefreshTokenBundle;
import com.smartcloset.auth.dto.AuthResponse;
import com.smartcloset.auth.dto.EmailVerificationConfirmRequest;
import com.smartcloset.auth.dto.EmailVerificationConfirmResponse;
import com.smartcloset.auth.dto.EmailVerificationRequest;
import com.smartcloset.auth.dto.EmailVerificationRequestedResponse;
import com.smartcloset.auth.dto.LoginRequest;
import com.smartcloset.auth.dto.LogoutResponse;
import com.smartcloset.auth.dto.PasswordResetConfirmRequest;
import com.smartcloset.auth.dto.PasswordResetConfirmResponse;
import com.smartcloset.auth.dto.PasswordResetRequest;
import com.smartcloset.auth.dto.PasswordResetRequestedResponse;
import com.smartcloset.auth.dto.SignupRequest;
import com.smartcloset.auth.dto.SignupResponse;
import com.smartcloset.auth.infrastructure.RefreshTokenCookieReader;
import com.smartcloset.auth.infrastructure.RefreshTokenCookieWriter;
import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import com.smartcloset.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 비밀번호 인증, 이메일 인증, 비밀번호 재설정, refresh session endpoint를 제공하는 HTTP adapter다.
 *
 * <p>Refresh token은 JSON body에 노출하지 않고 {@link RefreshTokenCookieWriter}를 통해
 * HttpOnly cookie로만 내려보낸다.</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginAttemptThrottle loginAttemptThrottle;
    private final RefreshTokenCookieReader refreshTokenCookieReader;
    private final RefreshTokenCookieWriter refreshTokenCookieWriter;

    public AuthController(
            AuthService authService,
            LoginAttemptThrottle loginAttemptThrottle,
            RefreshTokenCookieReader refreshTokenCookieReader,
            RefreshTokenCookieWriter refreshTokenCookieWriter
    ) {
        this.authService = authService;
        this.loginAttemptThrottle = loginAttemptThrottle;
        this.refreshTokenCookieReader = refreshTokenCookieReader;
        this.refreshTokenCookieWriter = refreshTokenCookieWriter;
    }

    /**
     * 비밀번호 가입을 접수하고 이메일 인증이 필요한 응답을 CREATED로 반환한다.
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(authService.signup(request)));
    }

    /**
     * 로그인 성공 시 access token은 JSON body에, refresh token은 HttpOnly cookie에 나눠 담는다.
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        String clientIdentifier = clientIdentifier(httpRequest);
        loginAttemptThrottle.checkAndRecordAttempt(request.email(), clientIdentifier);
        RefreshTokenBundle bundle;
        try {
            bundle = authService.loginWithRefreshSession(request);
        } catch (SmartClosetException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            loginAttemptThrottle.clearAttempts(request.email(), clientIdentifier);
            throw exception;
        }
        loginAttemptThrottle.recordSuccess(request.email(), clientIdentifier);
        refreshTokenCookieWriter.write(response, bundle.refreshToken());
        return ApiResponse.of(bundle.authResponse());
    }

    /**
     * refresh token rotation 결과를 다시 cookie로 내려 세션 탈취 재사용 가능성을 줄인다.
     */
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = refreshTokenCookieReader.read(request)
                .orElseThrow(() -> new SmartClosetException(ErrorCode.UNAUTHORIZED));
        RefreshTokenBundle bundle = authService.refresh(refreshToken);
        refreshTokenCookieWriter.write(response, bundle.refreshToken());
        return ApiResponse.of(bundle.authResponse());
    }

    /**
     * 서버 session revoke와 브라우저 cookie 만료를 함께 수행한다. cookie가 없어도 성공 응답을 유지한다.
     */
    @PostMapping("/logout")
    public ApiResponse<LogoutResponse> logout(HttpServletRequest request, HttpServletResponse response) {
        refreshTokenCookieReader.read(request).ifPresent(authService::logout);
        refreshTokenCookieWriter.expire(response);
        return ApiResponse.of(LogoutResponse.success());
    }

    /**
     * 미인증 계정에 인증 메일을 다시 보내되 계정 존재 여부는 응답에 드러내지 않는다.
     */
    @PostMapping("/email-verification/request")
    public ApiResponse<EmailVerificationRequestedResponse> requestEmailVerification(
            @Valid @RequestBody EmailVerificationRequest request
    ) {
        return ApiResponse.of(authService.requestEmailVerification(request));
    }

    /**
     * 이메일 인증 token을 소비해 password 계정의 로그인 가능 상태를 확정한다.
     */
    @PostMapping("/email-verification/confirm")
    public ApiResponse<EmailVerificationConfirmResponse> confirmEmailVerification(
            @Valid @RequestBody EmailVerificationConfirmRequest request
    ) {
        return ApiResponse.of(authService.confirmEmailVerification(request));
    }

    /**
     * 비밀번호 재설정 메일 발송을 요청하며 계정 존재 여부가 드러나지 않는 성공 응답을 유지한다.
     */
    @PostMapping("/password-reset/request")
    public ApiResponse<PasswordResetRequestedResponse> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request
    ) {
        return ApiResponse.of(authService.requestPasswordReset(request));
    }

    /**
     * 재설정 token 검증 후 새 비밀번호를 저장하고 기존 refresh session을 폐기한다.
     */
    @PostMapping("/password-reset/confirm")
    public ApiResponse<PasswordResetConfirmResponse> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request
    ) {
        return ApiResponse.of(authService.confirmPasswordReset(request));
    }

    private String clientIdentifier(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        if (remoteAddress == null || remoteAddress.isBlank()) {
            return "unknown";
        }
        return remoteAddress;
    }
}

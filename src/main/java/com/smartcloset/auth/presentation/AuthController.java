package com.smartcloset.auth.presentation;

import com.smartcloset.auth.application.AuthService;
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

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieReader refreshTokenCookieReader;
    private final RefreshTokenCookieWriter refreshTokenCookieWriter;

    public AuthController(
            AuthService authService,
            RefreshTokenCookieReader refreshTokenCookieReader,
            RefreshTokenCookieWriter refreshTokenCookieWriter
    ) {
        this.authService = authService;
        this.refreshTokenCookieReader = refreshTokenCookieReader;
        this.refreshTokenCookieWriter = refreshTokenCookieWriter;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(authService.signup(request)));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        RefreshTokenBundle bundle = authService.loginWithRefreshSession(request);
        refreshTokenCookieWriter.write(response, bundle.refreshToken());
        return ApiResponse.of(bundle.authResponse());
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = refreshTokenCookieReader.read(request)
                .orElseThrow(() -> new SmartClosetException(ErrorCode.UNAUTHORIZED));
        RefreshTokenBundle bundle = authService.refresh(refreshToken);
        refreshTokenCookieWriter.write(response, bundle.refreshToken());
        return ApiResponse.of(bundle.authResponse());
    }

    @PostMapping("/logout")
    public ApiResponse<LogoutResponse> logout(HttpServletRequest request, HttpServletResponse response) {
        refreshTokenCookieReader.read(request).ifPresent(authService::logout);
        refreshTokenCookieWriter.expire(response);
        return ApiResponse.of(LogoutResponse.success());
    }

    @PostMapping("/email-verification/request")
    public ApiResponse<EmailVerificationRequestedResponse> requestEmailVerification(
            @Valid @RequestBody EmailVerificationRequest request
    ) {
        return ApiResponse.of(authService.requestEmailVerification(request));
    }

    @PostMapping("/email-verification/confirm")
    public ApiResponse<EmailVerificationConfirmResponse> confirmEmailVerification(
            @Valid @RequestBody EmailVerificationConfirmRequest request
    ) {
        return ApiResponse.of(authService.confirmEmailVerification(request));
    }

    @PostMapping("/password-reset/request")
    public ApiResponse<PasswordResetRequestedResponse> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request
    ) {
        return ApiResponse.of(authService.requestPasswordReset(request));
    }

    @PostMapping("/password-reset/confirm")
    public ApiResponse<PasswordResetConfirmResponse> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request
    ) {
        return ApiResponse.of(authService.confirmPasswordReset(request));
    }
}

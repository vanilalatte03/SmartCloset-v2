package com.smartcloset.auth.presentation;

import com.smartcloset.auth.application.GoogleOAuthService;
import com.smartcloset.auth.application.RefreshTokenBundle;
import com.smartcloset.auth.dto.OAuthProvidersResponse;
import com.smartcloset.auth.infrastructure.RefreshTokenCookieWriter;
import com.smartcloset.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/oauth2")
public class OAuthController {

    private final GoogleOAuthService googleOAuthService;
    private final RefreshTokenCookieWriter refreshTokenCookieWriter;

    public OAuthController(
            GoogleOAuthService googleOAuthService,
            RefreshTokenCookieWriter refreshTokenCookieWriter
    ) {
        this.googleOAuthService = googleOAuthService;
        this.refreshTokenCookieWriter = refreshTokenCookieWriter;
    }

    @GetMapping("/providers")
    public ApiResponse<OAuthProvidersResponse> providers() {
        return ApiResponse.of(googleOAuthService.providers());
    }

    @GetMapping("/google")
    public void startGoogleLogin(HttpServletResponse response) throws IOException {
        response.sendRedirect(googleOAuthService.authorizationUri().toString());
    }

    @GetMapping("/callback/google")
    public void googleCallback(
            @RequestParam @NotBlank String code,
            HttpServletResponse response
    ) throws IOException {
        RefreshTokenBundle bundle = googleOAuthService.callback(code);
        refreshTokenCookieWriter.write(response, bundle.refreshToken());
        response.sendRedirect(googleOAuthService.frontendCallbackUri().toString());
    }
}

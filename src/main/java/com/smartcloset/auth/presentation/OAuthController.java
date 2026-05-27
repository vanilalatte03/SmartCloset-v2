package com.smartcloset.auth.presentation;

import com.smartcloset.auth.application.GoogleOAuthService;
import com.smartcloset.auth.application.RefreshTokenBundle;
import com.smartcloset.auth.dto.OAuthProvidersResponse;
import com.smartcloset.auth.infrastructure.OAuthStateCookieManager;
import com.smartcloset.auth.infrastructure.RefreshTokenCookieWriter;
import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import com.smartcloset.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
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
    private final OAuthStateCookieManager oAuthStateCookieManager;
    private final RefreshTokenCookieWriter refreshTokenCookieWriter;

    public OAuthController(
            GoogleOAuthService googleOAuthService,
            OAuthStateCookieManager oAuthStateCookieManager,
            RefreshTokenCookieWriter refreshTokenCookieWriter
    ) {
        this.googleOAuthService = googleOAuthService;
        this.oAuthStateCookieManager = oAuthStateCookieManager;
        this.refreshTokenCookieWriter = refreshTokenCookieWriter;
    }

    @GetMapping("/providers")
    public ApiResponse<OAuthProvidersResponse> providers() {
        return ApiResponse.of(googleOAuthService.providers());
    }

    @GetMapping("/google")
    public void startGoogleLogin(HttpServletResponse response) throws IOException {
        String state = googleOAuthService.createState();
        String authorizationUri = googleOAuthService.authorizationUri(state).toString();
        oAuthStateCookieManager.write(response, state);
        response.sendRedirect(authorizationUri);
    }

    @GetMapping("/callback/google")
    public void googleCallback(
            @RequestParam @NotBlank String code,
            @RequestParam @NotBlank String state,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        if (!oAuthStateCookieManager.matches(request, state)) {
            oAuthStateCookieManager.expire(response);
            throw new SmartClosetException(ErrorCode.UNAUTHORIZED);
        }
        oAuthStateCookieManager.expire(response);
        RefreshTokenBundle bundle = googleOAuthService.callback(code);
        refreshTokenCookieWriter.write(response, bundle.refreshToken());
        response.sendRedirect(googleOAuthService.frontendCallbackUri().toString());
    }
}

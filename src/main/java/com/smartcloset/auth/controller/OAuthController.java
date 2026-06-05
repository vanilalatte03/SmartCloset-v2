package com.smartcloset.auth.controller;

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

/**
 * OAuth2 login 시작과 callback을 처리하는 HTTP adapter다.
 *
 * <p>SmartCloset 세션 발급 전 callback state cookie를 검증해 외부 redirect 흐름을 보호한다.</p>
 */
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

    /**
     * 프론트가 표시할 OAuth provider 활성화 상태와 로그인 시작 경로를 반환한다.
     */
    @GetMapping("/providers")
    public ApiResponse<OAuthProvidersResponse> providers() {
        return ApiResponse.of(googleOAuthService.providers());
    }

    /**
     * OAuth state를 cookie에 저장한 뒤 Google authorization endpoint로 redirect한다.
     */
    @GetMapping("/google")
    public void startGoogleLogin(HttpServletResponse response) throws IOException {
        String state = googleOAuthService.createState();
        String authorizationUri = googleOAuthService.authorizationUri(state).toString();
        oAuthStateCookieManager.write(response, state);
        response.sendRedirect(authorizationUri);
    }

    /**
     * callback state가 cookie와 일치할 때만 SmartCloset 세션을 발급하고 프론트로 돌려보낸다.
     */
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

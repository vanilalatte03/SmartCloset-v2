package com.smartcloset.auth.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smartcloset.auth.application.GoogleOAuthService;
import com.smartcloset.auth.infrastructure.OAuthStateCookieManager;
import com.smartcloset.auth.infrastructure.OAuthStateCookieProperties;
import com.smartcloset.auth.infrastructure.RefreshTokenCookieWriter;
import com.smartcloset.common.exception.GlobalExceptionHandler;
import jakarta.servlet.http.Cookie;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OAuthControllerStateTest {

    private GoogleOAuthService googleOAuthService;
    private RefreshTokenCookieWriter refreshTokenCookieWriter;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        googleOAuthService = mock(GoogleOAuthService.class);
        refreshTokenCookieWriter = mock(RefreshTokenCookieWriter.class);
        OAuthStateCookieManager stateCookieManager = new OAuthStateCookieManager(
                new OAuthStateCookieProperties(
                        "smartcloset.oauth2State",
                        false,
                        "Lax",
                        "",
                        "/api/auth/oauth2",
                        Duration.ofMinutes(5)
                )
        );
        OAuthController controller = new OAuthController(
                googleOAuthService,
                stateCookieManager,
                refreshTokenCookieWriter
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void googleLoginStartStoresStateInHttpOnlyCookieBeforeRedirect() throws Exception {
        String state = "generated-state";
        String authorizationUri = "https://accounts.google.com/o/oauth2/v2/auth?state=generated-state";
        when(googleOAuthService.createState()).thenReturn(state);
        when(googleOAuthService.authorizationUri(state)).thenReturn(URI.create(authorizationUri));

        MvcResult result = mockMvc.perform(get("/api/auth/oauth2/google"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(authorizationUri))
                .andReturn();

        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .contains("smartcloset.oauth2State=generated-state")
                .contains("HttpOnly")
                .contains("SameSite=Lax")
                .contains("Max-Age=300");
    }

    @Test
    void googleCallbackRejectsMismatchedStateBeforeIssuingRefreshCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/oauth2/callback/google")
                        .param("code", "valid-code")
                        .param("state", "tampered-state")
                        .cookie(new Cookie("smartcloset.oauth2State", "stored-state")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.details").isArray())
                .andReturn();

        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .contains("smartcloset.oauth2State=")
                .contains("Max-Age=0");
        verify(googleOAuthService, never()).callback(any());
        verify(refreshTokenCookieWriter, never()).write(any(), any());
    }

    @Test
    void googleCallbackRejectsMissingStateCookieBeforeIssuingRefreshCookie() throws Exception {
        mockMvc.perform(get("/api/auth/oauth2/callback/google")
                        .param("code", "valid-code")
                        .param("state", "callback-state"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verify(googleOAuthService, never()).callback(eq("valid-code"));
        verify(refreshTokenCookieWriter, never()).write(any(), any());
    }
}

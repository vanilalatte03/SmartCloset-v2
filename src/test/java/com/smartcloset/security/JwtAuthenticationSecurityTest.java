package com.smartcloset.security;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloset.user.domain.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class JwtAuthenticationSecurityTest {

    private static final String DEFAULT_TEST_SECRET = "change-me-local-development-only";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void currentUserRequiresBearerToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void currentUserRejectsMalformedBearerTokenWithJsonError() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
                .andExpect(jsonPath("$.message").value("인증 토큰이 올바르지 않습니다."))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void currentUserRejectsExpiredBearerTokenWithJsonError() throws Exception {
        JwtTokenProvider expiredTokenProvider = new JwtTokenProvider(
                DEFAULT_TEST_SECRET,
                objectMapper,
                Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)
        );
        String expiredToken = expiredTokenProvider.createAccessToken(
                new CurrentUserPrincipal(1L, "expired@example.com", UserRole.USER));

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void currentWeatherRejectsExpiredBearerTokenWithJsonError() throws Exception {
        JwtTokenProvider expiredTokenProvider = new JwtTokenProvider(
                DEFAULT_TEST_SECRET,
                objectMapper,
                Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)
        );
        String expiredToken = expiredTokenProvider.createAccessToken(
                new CurrentUserPrincipal(1L, "expired-weather@example.com", UserRole.USER));

        mockMvc.perform(get("/api/weather/current")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
                .andExpect(jsonPath("$.details").isArray());
    }
}

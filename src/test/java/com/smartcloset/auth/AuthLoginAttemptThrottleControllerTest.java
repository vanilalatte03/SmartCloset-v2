package com.smartcloset.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloset.auth.repository.RefreshSessionRepository;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import java.util.Map;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "smartcloset.security.login-attempt.max-failures=2",
        "smartcloset.security.login-attempt.window=PT5M"
})
@Transactional
class AuthLoginAttemptThrottleControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshSessionRepository refreshSessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void repeatedLoginFailuresForSameEmailAndRemoteAddressAreRateLimited() throws Exception {
        userRepository.save(
                User.create("limited@example.com", passwordEncoder.encode("password123!"), "Limited User"));

        login("limited@example.com", "bad-password", "203.0.113.50", null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        login("limited@example.com", "bad-password", "203.0.113.50", null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        login("limited@example.com", "bad-password", "203.0.113.50", null)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("LOGIN_ATTEMPT_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void changingForwardedForHeaderDoesNotBypassRemoteAddressLimit() throws Exception {
        userRepository.save(User.create(
                "xff-limited@example.com",
                passwordEncoder.encode("password123!"),
                "Forwarded User"
        ));

        login("xff-limited@example.com", "bad-password", "203.0.113.60", "198.51.100.1")
                .andExpect(status().isUnauthorized());
        login("xff-limited@example.com", "bad-password", "203.0.113.60", "198.51.100.2")
                .andExpect(status().isUnauthorized());

        login("xff-limited@example.com", "bad-password", "203.0.113.60", "198.51.100.3")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("LOGIN_ATTEMPT_LIMIT_EXCEEDED"));
    }

    @Test
    void successfulLoginStillIssuesRefreshCookieAndClearsAttempts() throws Exception {
        userRepository.save(User.create(
                "throttle-success@example.com",
                passwordEncoder.encode("password123!"),
                "Success User"
        ));

        login("throttle-success@example.com", "bad-password", "203.0.113.70", null)
                .andExpect(status().isUnauthorized());

        ResultActions success = login("throttle-success@example.com", "password123!", "203.0.113.70", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.data.user.email").value("throttle-success@example.com"));

        Cookie refreshCookie = success.andReturn().getResponse().getCookie("smartcloset.refreshToken");
        assertThat(refreshCookie).isNotNull();
        assertThat(success.andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .contains("HttpOnly")
                .contains("SameSite=Lax");
        assertThat(refreshSessionRepository.findAll()).hasSize(1);

        login("throttle-success@example.com", "bad-password", "203.0.113.70", null)
                .andExpect(status().isUnauthorized());
        login("throttle-success@example.com", "bad-password", "203.0.113.70", null)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("LOGIN_ATTEMPT_LIMIT_EXCEEDED"));
    }

    @Test
    void successfulLoginDoesNotClearClientWideFailuresFromOtherEmails() throws Exception {
        userRepository.save(User.create(
                "known-success@example.com",
                passwordEncoder.encode("password123!"),
                "Known User"
        ));

        login("candidate-one@example.com", "password123!", "203.0.113.80", null)
                .andExpect(status().isUnauthorized());
        login("known-success@example.com", "password123!", "203.0.113.80", null)
                .andExpect(status().isOk());
        login("candidate-two@example.com", "password123!", "203.0.113.80", null)
                .andExpect(status().isUnauthorized());

        login("candidate-three@example.com", "password123!", "203.0.113.80", null)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("LOGIN_ATTEMPT_LIMIT_EXCEEDED"));
    }

    private ResultActions login(String email, String password, String remoteAddress, String forwardedFor)
            throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .with(remoteAddress(remoteAddress, forwardedFor))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", email,
                        "password", password
                ))));
    }

    private RequestPostProcessor remoteAddress(String remoteAddress, String forwardedFor) {
        return request -> {
            request.setRemoteAddr(remoteAddress);
            if (forwardedFor != null) {
                request.addHeader("X-Forwarded-For", forwardedFor);
            }
            return request;
        };
    }
}

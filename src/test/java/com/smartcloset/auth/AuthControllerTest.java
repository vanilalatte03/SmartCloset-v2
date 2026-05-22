package com.smartcloset.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class AuthControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void signupCreatesUserWithDefaultProfileAndReturnsBearerAccessToken() throws Exception {
        Map<String, Object> request = Map.of(
                "email", "signup@example.com",
                "password", "password123!",
                "name", "Signup User"
        );

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value("signup@example.com"))
                .andExpect(jsonPath("$.data.user.name").value("Signup User"))
                .andExpect(jsonPath("$.data.user.role").value("USER"))
                .andExpect(jsonPath("$.data.user.userId").doesNotExist())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist());

        User saved = userRepository.findByEmail("signup@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("password123!", saved.getPasswordHash())).isTrue();
        assertThat(saved.getLocationCode()).isEqualTo("SEOUL");
        assertThat(saved.getPreferredColorsJson()).isEqualTo("[]");
        assertThat(saved.getPreferredMaterialsJson()).isEqualTo("[]");
        assertThat(saved.getStyleTagsJson()).isEqualTo("[]");
    }

    @Test
    void signupRejectsDuplicateEmail() throws Exception {
        userRepository.save(User.create("duplicate@example.com", passwordEncoder.encode("password123!"), "First User"));
        Map<String, Object> request = Map.of(
                "email", "duplicate@example.com",
                "password", "password123!",
                "name", "Second User"
        );

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void loginReturnsBearerAccessTokenForCorrectPassword() throws Exception {
        userRepository.save(User.create("login@example.com", passwordEncoder.encode("password123!"), "Login User"));
        Map<String, Object> request = Map.of(
                "email", "login@example.com",
                "password", "password123!"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value("login@example.com"))
                .andExpect(jsonPath("$.data.user.name").value("Login User"))
                .andExpect(jsonPath("$.data.user.userId").doesNotExist())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist());
    }

    @Test
    void loginRejectsWrongPasswordOrMissingEmailAsUnauthorized() throws Exception {
        userRepository.save(User.create("wrong-password@example.com", passwordEncoder.encode("password123!"),
                "Wrong Password User"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "wrong-password@example.com",
                                "password", "bad-password"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.details").isArray());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "missing-login@example.com",
                                "password", "password123!"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.details").isArray());
    }
}

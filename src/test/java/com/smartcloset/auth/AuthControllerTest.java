package com.smartcloset.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloset.security.CurrentUserPrincipal;
import com.smartcloset.security.JwtTokenProvider;
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
import org.springframework.test.web.servlet.MvcResult;
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

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void signupCreatesUserWithDefaultProfileAndReturnsBearerToken() throws Exception {
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
                .andExpect(jsonPath("$.data.user.createdAt").exists())
                .andExpect(jsonPath("$.data.user.updatedAt").exists())
                .andExpect(jsonPath("$.data.user.userId").doesNotExist());

        User saved = userRepository.findByEmail("signup@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("password123!", saved.getPasswordHash())).isTrue();
        assertThat(saved.getPasswordHash()).isNotEqualTo("password123!");
        assertThat(saved.getLocationCode()).isEqualTo("SEOUL");
        assertThat(saved.getLocationName()).isEqualTo("서울특별시");
        assertThat(saved.getLocationNx()).isEqualTo(60);
        assertThat(saved.getLocationNy()).isEqualTo(127);
        assertThat(saved.getPreferredColorsJson()).isEqualTo("[]");
        assertThat(saved.getPreferredMaterialsJson()).isEqualTo("[]");
        assertThat(saved.getStyleTagsJson()).isEqualTo("[]");
    }

    @Test
    void signupRejectsDuplicateEmail() throws Exception {
        userRepository.save(User.create(
                "duplicate@example.com",
                passwordEncoder.encode("password123!"),
                "Existing User"
        ));
        Map<String, Object> request = Map.of(
                "email", "duplicate@example.com",
                "password", "password123!",
                "name", "Duplicate User"
        );

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void loginReturnsBearerTokenAndCurrentUserResponse() throws Exception {
        User user = userRepository.save(User.create(
                "login@example.com",
                passwordEncoder.encode("password123!"),
                "Login User"
        ));
        Map<String, Object> request = Map.of(
                "email", "login@example.com",
                "password", "password123!"
        );

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value("login@example.com"))
                .andExpect(jsonPath("$.data.user.name").value("Login User"))
                .andExpect(jsonPath("$.data.user.role").value("USER"))
                .andExpect(jsonPath("$.data.user.userId").doesNotExist())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String accessToken = body.path("data").path("accessToken").asText();
        CurrentUserPrincipal principal = jwtTokenProvider.parseAccessToken(accessToken);

        assertThat(principal.userId()).isEqualTo(user.getId());
        assertThat(principal.email()).isEqualTo("login@example.com");
        assertThat(principal.role().name()).isEqualTo("USER");
    }

    @Test
    void loginRejectsWrongPassword() throws Exception {
        userRepository.save(User.create(
                "wrong-password@example.com",
                passwordEncoder.encode("password123!"),
                "Login User"
        ));
        Map<String, Object> request = Map.of(
                "email", "wrong-password@example.com",
                "password", "wrong-password"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void loginRejectsMissingEmail() throws Exception {
        Map<String, Object> request = Map.of(
                "email", "missing-login@example.com",
                "password", "password123!"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.details").isArray());
    }
}

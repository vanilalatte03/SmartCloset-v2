package com.smartcloset.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloset.auth.application.AccountActionTokenService;
import com.smartcloset.auth.domain.AccountActionTokenPurpose;
import com.smartcloset.auth.repository.AccountActionTokenRepository;
import com.smartcloset.auth.repository.RefreshSessionRepository;
import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.domain.ClothingMaterial;
import com.smartcloset.clothing.repository.ClothingItemRepository;
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
    private ClothingItemRepository clothingItemRepository;

    @Autowired
    private RefreshSessionRepository refreshSessionRepository;

    @Autowired
    private AccountActionTokenRepository accountActionTokenRepository;

    @Autowired
    private AccountActionTokenService accountActionTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void signupCreatesUnverifiedUserWithDefaultProfileAndDoesNotReturnAccessToken() throws Exception {
        Map<String, Object> request = Map.of(
                "email", "signup@example.com",
                "password", "password123!",
                "name", "Signup User"
        );

        MvcResult signupResult = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("signup@example.com"))
                .andExpect(jsonPath("$.data.emailVerificationRequired").value(true))
                .andExpect(jsonPath("$.data.message").value("이메일 인증 후 로그인할 수 있습니다."))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.user").doesNotExist())
                .andReturn();

        User saved = userRepository.findByEmail("signup@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("password123!", saved.getPasswordHash())).isTrue();
        assertThat(saved.isEmailVerified()).isFalse();
        assertThat(saved.isPasswordLoginEnabled()).isTrue();
        assertThat(saved.getLocationCode()).isEqualTo("SEOUL");
        assertThat(saved.getPreferredColorsJson()).isEqualTo("[]");
        assertThat(saved.getPreferredMaterialsJson()).isEqualTo("[]");
        assertThat(saved.getStyleTagsJson()).isEqualTo("[]");

        assertThat(clothingItemRepository.countByUserId(saved.getId())).isEqualTo(5);
        assertThat(accountActionTokenRepository.findAll())
                .singleElement()
                .satisfies(token -> {
                    assertThat(token.getUser().getId()).isEqualTo(saved.getId());
                    assertThat(token.getPurpose()).isEqualTo(AccountActionTokenPurpose.EMAIL_VERIFICATION);
                    assertThat(token.getTokenHash()).isNotBlank();
                    assertThat(signupResult.getResponse().getContentAsString()).doesNotContain(token.getTokenHash());
                    assertThat(token.getUsedAt()).isNull();
                });
    }

    @Test
    void oauthProvidersReturnsGoogleDisabledWhenClientConfigIsMissing() throws Exception {
        mockMvc.perform(get("/api/auth/oauth2/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.google.enabled").value(false))
                .andExpect(jsonPath("$.data.google.loginUrl").doesNotExist());
    }

    @Test
    void googleLoginStartFailsWhenProviderIsDisabled() throws Exception {
        mockMvc.perform(get("/api/auth/oauth2/google"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("OAUTH2_PROVIDER_UNAVAILABLE"))
                .andExpect(jsonPath("$.details").isArray());
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
        User user = userRepository.save(
                User.create("login@example.com", passwordEncoder.encode("password123!"), "Login User"));
        Map<String, Object> request = Map.of(
                "email", "login@example.com",
                "password", "password123!"
        );

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value("login@example.com"))
                .andExpect(jsonPath("$.data.user.name").value("Login User"))
                .andExpect(jsonPath("$.data.user.userId").doesNotExist())
                .andReturn();

        assertThat(clothingItemRepository.countByUserId(user.getId())).isEqualTo(5);
        Cookie refreshCookie = loginResult.getResponse().getCookie("smartcloset.refreshToken");
        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie.getValue()).isNotBlank();
        assertThat(loginResult.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .contains("HttpOnly")
                .contains("SameSite=Lax");
        assertThat(loginResult.getResponse().getContentAsString()).doesNotContain(refreshCookie.getValue());
        assertThat(refreshSessionRepository.findAll())
                .hasSize(1)
                .allSatisfy(session -> {
                    assertThat(session.getUser().getId()).isEqualTo(user.getId());
                    assertThat(session.getTokenHash()).isNotBlank();
                    assertThat(session.getTokenHash()).isNotEqualTo(refreshCookie.getValue());
                    assertThat(session.getRevokedAt()).isNull();
                    assertThat(session.getReplacedByTokenHash()).isNull();
                });

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        assertThat(clothingItemRepository.countByUserId(user.getId())).isEqualTo(5);
    }

    @Test
    void loginRejectsUnverifiedPasswordAccount() throws Exception {
        userRepository.save(User.createPasswordSignup(
                "unverified@example.com",
                passwordEncoder.encode("password123!"),
                "Unverified User"
        ));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "unverified@example.com",
                                "password", "password123!"
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_VERIFICATION_REQUIRED"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void emailVerificationRequestAndConfirmUseSingleUseHashedToken() throws Exception {
        User user = userRepository.save(User.createPasswordSignup(
                "verify@example.com",
                passwordEncoder.encode("password123!"),
                "Verify User"
        ));

        mockMvc.perform(post("/api/auth/email-verification/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "missing-verify@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requested").value(true));

        AccountActionTokenService.IssuedAccountActionToken token =
                accountActionTokenService.issue(user, AccountActionTokenPurpose.EMAIL_VERIFICATION);
        assertThat(accountActionTokenRepository.findAll())
                .anySatisfy(savedToken -> {
                    assertThat(savedToken.getTokenHash()).isNotBlank();
                    assertThat(savedToken.getTokenHash()).isNotEqualTo(token.token());
                });

        mockMvc.perform(post("/api/auth/email-verification/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", token.token()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.emailVerified").value(true));

        assertThat(userRepository.findByEmail("verify@example.com").orElseThrow().isEmailVerified()).isTrue();

        mockMvc.perform(post("/api/auth/email-verification/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", token.token()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ACCOUNT_TOKEN_INVALID"));
    }

    @Test
    void passwordResetRequestDoesNotExposeAccountExistenceAndConfirmRevokesRefreshSessions() throws Exception {
        User user = userRepository.save(
                User.create("reset@example.com", passwordEncoder.encode("password123!"), "Reset User"));
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "reset@example.com",
                                "password", "password123!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        Cookie refreshCookie = loginResult.getResponse().getCookie("smartcloset.refreshToken");

        mockMvc.perform(post("/api/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "missing-reset@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requested").value(true));

        AccountActionTokenService.IssuedAccountActionToken token =
                accountActionTokenService.issue(user, AccountActionTokenPurpose.PASSWORD_RESET);

        mockMvc.perform(post("/api/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "token", token.token(),
                                "newPassword", "newPassword123!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.passwordReset").value(true));

        User resetUser = userRepository.findByEmail("reset@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("newPassword123!", resetUser.getPasswordHash())).isTrue();
        assertThat(refreshSessionRepository.findAll())
                .singleElement()
                .satisfies(session -> assertThat(session.getRevokedAt()).isNotNull());

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "reset@example.com",
                                "password", "password123!"
                        ))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "reset@example.com",
                                "password", "newPassword123!"
                        ))))
                .andExpect(status().isOk());
    }

    @Test
    void refreshRotatesRefreshCookieAndReturnsNewAccessTokenWithoutRefreshTokenBody() throws Exception {
        userRepository.save(User.create("refresh@example.com", passwordEncoder.encode("password123!"), "Refresh User"));
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "refresh@example.com",
                                "password", "password123!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        Cookie originalRefreshCookie = loginResult.getResponse().getCookie("smartcloset.refreshToken");
        String originalRefreshToken = originalRefreshCookie.getValue();

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(originalRefreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.data.user.email").value("refresh@example.com"))
                .andReturn();

        Cookie rotatedRefreshCookie = refreshResult.getResponse().getCookie("smartcloset.refreshToken");
        assertThat(rotatedRefreshCookie).isNotNull();
        assertThat(rotatedRefreshCookie.getValue()).isNotBlank();
        assertThat(rotatedRefreshCookie.getValue()).isNotEqualTo(originalRefreshToken);
        assertThat(refreshSessionRepository.findAll())
                .hasSize(2)
                .anySatisfy(session -> {
                    assertThat(session.getRevokedAt()).isNotNull();
                    assertThat(session.getReplacedByTokenHash()).isNotBlank();
                })
                .anySatisfy(session -> {
                    assertThat(session.getRevokedAt()).isNull();
                    assertThat(session.getReplacedByTokenHash()).isNull();
                });

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(originalRefreshCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void refreshRequiresRefreshCookie() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void logoutRevokesRefreshSessionAndExpiresCookieEvenWhenCookieIsMissing() throws Exception {
        userRepository.save(User.create("logout@example.com", passwordEncoder.encode("password123!"), "Logout User"));
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "logout@example.com",
                                "password", "password123!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        Cookie refreshCookie = loginResult.getResponse().getCookie("smartcloset.refreshToken");

        MvcResult logoutResult = mockMvc.perform(post("/api/auth/logout")
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loggedOut").value(true))
                .andReturn();

        assertThat(refreshSessionRepository.findAll())
                .singleElement()
                .satisfies(session -> assertThat(session.getRevokedAt()).isNotNull());
        assertThat(logoutResult.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .contains("smartcloset.refreshToken=")
                .contains("Max-Age=0")
                .contains("HttpOnly");

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loggedOut").value(true));
    }

    @Test
    void loginDoesNotSeedDefaultPresetsWhenUserAlreadyHasOnlyArchivedClothes() throws Exception {
        User user = userRepository.save(
                User.create("existing-clothes@example.com", passwordEncoder.encode("password123!"), "Existing User"));
        ClothingItem archived = ClothingItem.create(
                user,
                "직접 등록한 셔츠",
                ClothingCategory.TOP,
                ClothingColor.WHITE,
                ClothingMaterial.COTTON,
                0,
                25,
                false
        );
        archived.archive();
        clothingItemRepository.save(archived);
        clothingItemRepository.flush();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "existing-clothes@example.com",
                                "password", "password123!"
                        ))))
                .andExpect(status().isOk());

        assertThat(clothingItemRepository.countByUserId(user.getId())).isEqualTo(1);
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

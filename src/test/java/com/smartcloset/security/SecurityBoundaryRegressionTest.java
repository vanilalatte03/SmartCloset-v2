package com.smartcloset.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.http.HttpHeaders;
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
class SecurityBoundaryRegressionTest {

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
    private SecurityErrorResponseWriter securityErrorResponseWriter;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void onlySignupAndLoginArePublicApiEndpoints() throws Exception {
        Map<String, Object> signupRequest = Map.of(
                "email", "public-signup@example.com",
                "password", "password123!",
                "name", "Public Signup"
        );
        userRepository.save(User.create("public-login@example.com", passwordEncoder.encode("password123!"),
                "Public Login"));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.userId").doesNotExist());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "public-login@example.com",
                                "password", "password123!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.userId").doesNotExist());
    }

    @Test
    void protectedApiEndpointsRequireBearerToken() throws Exception {
        Map<String, Object> locationRequest = Map.of("locationCode", "BUSAN");
        Map<String, Object> preferencesRequest = Map.of(
                "preferredColors", java.util.List.of("NAVY"),
                "preferredMaterials", java.util.List.of("COTTON"),
                "styleTags", java.util.List.of("MINIMAL")
        );
        Map<String, Object> clothingRequest = Map.of(
                "name", "그레이 후드",
                "category", "TOP",
                "color", "GRAY",
                "material", "COTTON",
                "minTemperature", 5,
                "maxTemperature", 18,
                "rainSuitable", false
        );

        assertRequiresBearerToken(get("/api/users/me"));
        assertRequiresBearerToken(get("/api/locations"));
        assertRequiresBearerToken(get("/api/users/me/location"));
        assertRequiresBearerToken(put("/api/users/me/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(locationRequest)));
        assertRequiresBearerToken(get("/api/users/me/preferences"));
        assertRequiresBearerToken(put("/api/users/me/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(preferencesRequest)));
        assertRequiresBearerToken(get("/api/clothes"));
        assertRequiresBearerToken(post("/api/clothes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(clothingRequest)));
        assertRequiresBearerToken(get("/api/clothes/{clothingId}", 1L));
        assertRequiresBearerToken(put("/api/clothes/{clothingId}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(clothingRequest)));
        assertRequiresBearerToken(patch("/api/clothes/{clothingId}/archive", 1L));
        assertRequiresBearerToken(post("/api/recommendations"));
        assertRequiresBearerToken(get("/api/recommendations"));
        assertRequiresBearerToken(patch("/api/recommendations/{recommendationId}/worn", 1L));
    }

    @Test
    void unknownApiPathStillRequiresBearerToken() throws Exception {
        mockMvc.perform(get("/api/not-a-public-endpoint"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void currentUserResponseDoesNotExposeUserIdWithAuthenticatedPrincipal() throws Exception {
        User user = userRepository.save(User.createSeedUser("boundary-current-user"));
        String token = jwtTokenProvider.createAccessToken(new CurrentUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getRole()
        ));

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(user.getEmail()))
                .andExpect(jsonPath("$.data.userId").doesNotExist());
    }

    @Test
    void reactDevServerCorsPreflightIsAllowedForProtectedApi() throws Exception {
        mockMvc.perform(options("/api/clothes")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "POST"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "authorization, content-type"));
    }

    @Test
    void securityJsonErrorWriterHasApplicationObjectMapperBean() {
        assertThat(securityErrorResponseWriter).isNotNull();
    }

    @Test
    void providesBcryptPasswordEncoderBean() {
        String encoded = passwordEncoder.encode("password123!");

        assertThat(encoded).isNotEqualTo("password123!");
        assertThat(passwordEncoder.matches("password123!", encoded)).isTrue();
    }

    private void assertRequiresBearerToken(org.springframework.test.web.servlet.RequestBuilder request)
            throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."))
                .andExpect(jsonPath("$.details").isArray());
    }
}

package com.smartcloset.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.domain.ClothingMaterial;
import com.smartcloset.clothing.repository.ClothingItemRepository;
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

        MvcResult signupResult = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value("signup@example.com"))
                .andExpect(jsonPath("$.data.user.name").value("Signup User"))
                .andExpect(jsonPath("$.data.user.role").value("USER"))
                .andExpect(jsonPath("$.data.user.userId").doesNotExist())
                .andReturn();

        User saved = userRepository.findByEmail("signup@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("password123!", saved.getPasswordHash())).isTrue();
        assertThat(saved.getLocationCode()).isEqualTo("SEOUL");
        assertThat(saved.getPreferredColorsJson()).isEqualTo("[]");
        assertThat(saved.getPreferredMaterialsJson()).isEqualTo("[]");
        assertThat(saved.getStyleTagsJson()).isEqualTo("[]");

        assertThat(clothingItemRepository.countByUserId(saved.getId())).isEqualTo(5);

        String accessToken = objectMapper.readTree(signupResult.getResponse().getContentAsString())
                .path("data")
                .path("accessToken")
                .asText();
        MvcResult clothesResult = mockMvc.perform(get("/api/clothes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].userId").doesNotExist())
                .andExpect(jsonPath("$.data[0].name").value("화이트 반팔 티셔츠"))
                .andExpect(jsonPath("$.data[0].category").value("TOP"))
                .andExpect(jsonPath("$.data[0].color").value("WHITE"))
                .andExpect(jsonPath("$.data[0].material").value("COTTON"))
                .andExpect(jsonPath("$.data[0].minTemperature").value(8))
                .andExpect(jsonPath("$.data[0].maxTemperature").value(30))
                .andExpect(jsonPath("$.data[0].rainSuitable").value(false))
                .andExpect(jsonPath("$.data[0].image.url").exists())
                .andExpect(jsonPath("$.data[0].image.contentType").value("image/jpeg"))
                .andReturn();

        JsonNode firstClothing = objectMapper.readTree(clothesResult.getResponse().getContentAsString())
                .path("data")
                .get(0);
        mockMvc.perform(get("/api/clothes/{clothingId}/image", firstClothing.path("id").asLong())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentType()).isEqualTo("image/jpeg"))
                .andExpect(result -> {
                    byte[] bytes = result.getResponse().getContentAsByteArray();
                    assertThat(bytes).hasSizeGreaterThan(3);
                    assertThat(bytes[0] & 0xff).isEqualTo(0xff);
                    assertThat(bytes[1] & 0xff).isEqualTo(0xd8);
                    assertThat(bytes[2] & 0xff).isEqualTo(0xff);
                });
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

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value("login@example.com"))
                .andExpect(jsonPath("$.data.user.name").value("Login User"))
                .andExpect(jsonPath("$.data.user.userId").doesNotExist());

        assertThat(clothingItemRepository.countByUserId(user.getId())).isEqualTo(5);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        assertThat(clothingItemRepository.countByUserId(user.getId())).isEqualTo(5);
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

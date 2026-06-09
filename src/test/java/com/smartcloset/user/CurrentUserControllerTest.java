package com.smartcloset.user;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloset.auth.application.AccountActionTokenService;
import com.smartcloset.auth.application.RefreshTokenService;
import com.smartcloset.auth.domain.AccountActionTokenPurpose;
import com.smartcloset.auth.domain.OAuthProvider;
import com.smartcloset.auth.domain.SocialAccount;
import com.smartcloset.auth.repository.AccountActionTokenRepository;
import com.smartcloset.auth.repository.RefreshSessionRepository;
import com.smartcloset.auth.repository.SocialAccountRepository;
import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.domain.ClothingMaterial;
import com.smartcloset.clothing.infrastructure.file.ClothingImageStorage;
import com.smartcloset.clothing.infrastructure.file.StoredClothingImage;
import com.smartcloset.clothing.repository.ClothingItemRepository;
import com.smartcloset.recommendation.domain.OutfitSlot;
import com.smartcloset.recommendation.domain.RecommendationResult;
import com.smartcloset.recommendation.domain.RecommendationResultItem;
import com.smartcloset.recommendation.domain.RecommendationScore;
import com.smartcloset.recommendation.domain.WearHistory;
import com.smartcloset.recommendation.repository.RecommendationResultItemRepository;
import com.smartcloset.recommendation.repository.RecommendationResultRepository;
import com.smartcloset.recommendation.repository.WearHistoryRepository;
import com.smartcloset.security.CurrentUserPrincipal;
import com.smartcloset.security.JwtTokenProvider;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import com.smartcloset.weather.domain.WeatherCondition;
import com.smartcloset.weather.domain.WeatherType;
import java.time.LocalDateTime;
import java.util.Map;
import org.assertj.core.api.Assertions;
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
class CurrentUserControllerTest {

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
    private RecommendationResultRepository recommendationResultRepository;

    @Autowired
    private RecommendationResultItemRepository recommendationResultItemRepository;

    @Autowired
    private WearHistoryRepository wearHistoryRepository;

    @Autowired
    private RefreshSessionRepository refreshSessionRepository;

    @Autowired
    private AccountActionTokenRepository accountActionTokenRepository;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private AccountActionTokenService accountActionTokenService;

    @Autowired
    private ClothingImageStorage clothingImageStorage;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void returnsCurrentUserForValidBearerTokenWithoutUserIdField() throws Exception {
        User user = userRepository.save(User.createSeedUser("current-user"));
        String token = accessToken(user);

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(user.getEmail()))
                .andExpect(jsonPath("$.data.name").value("current-user"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists())
                .andExpect(jsonPath("$.data.userId").doesNotExist());
    }

    @Test
    void updatesCurrentUserNameForValidBearerToken() throws Exception {
        User user = userRepository.save(User.createSeedUser("before-name"));
        String token = accessToken(user);

        mockMvc.perform(patch("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "  지호  "))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(user.getEmail()))
                .andExpect(jsonPath("$.data.name").value("지호"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.userId").doesNotExist());

        Assertions.assertThat(userRepository.findById(user.getId()).orElseThrow().getName()).isEqualTo("지호");
    }

    @Test
    void updatesGoogleOnlyCurrentUserNameForValidBearerToken() throws Exception {
        User user = userRepository.save(User.createGoogleUser("google-name@example.com", "Google Name"));
        socialAccountRepository.save(SocialAccount.link(
                user,
                OAuthProvider.GOOGLE,
                "google-name-profile",
                user.getEmail(),
                LocalDateTime.now()
        ));

        mockMvc.perform(patch("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Google Jiho"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Google Jiho"))
                .andExpect(jsonPath("$.data.passwordLoginEnabled").value(false))
                .andExpect(jsonPath("$.data.authProviders[0]").value("GOOGLE"));

        Assertions.assertThat(userRepository.findById(user.getId()).orElseThrow().getName()).isEqualTo("Google Jiho");
    }

    @Test
    void updateCurrentUserNameRejectsBlankName() throws Exception {
        User user = userRepository.save(User.createSeedUser("blank-name"));

        mockMvc.perform(patch("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", " "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("METHOD_ARGUMENT_NOT_VALID"));

        Assertions.assertThat(userRepository.findById(user.getId()).orElseThrow().getName()).isEqualTo("blank-name");
    }

    @Test
    void deletesPasswordAccountOwnedDataAndRejectsStaleAccessToken() throws Exception {
        User user = userRepository.save(
                User.create("delete-password@example.com", passwordEncoder.encode("password123!"), "Delete User"));
        User otherUser = userRepository.save(User.createSeedUser("other-user"));
        ClothingItem clothing = clothingItemRepository.save(clothing(user, "delete-shirt"));
        ClothingItem otherClothing = clothingItemRepository.save(clothing(otherUser, "other-shirt"));
        StoredClothingImage storedImage = clothingImageStorage.store(new byte[] {1, 2, 3}, "jpg");
        clothing.updateImageMetadata(storedImage.storedFilename(), "image/jpeg", 3L, LocalDateTime.now());
        RecommendationResult recommendation = recommendationResultRepository.save(recommendation(user));
        RecommendationResult otherRecommendation = recommendationResultRepository.save(recommendation(otherUser));
        recommendationResultItemRepository.save(RecommendationResultItem.of(recommendation, clothing, OutfitSlot.TOP));
        recommendationResultItemRepository.save(
                RecommendationResultItem.of(otherRecommendation, otherClothing, OutfitSlot.TOP));
        wearHistoryRepository.save(WearHistory.record(user, recommendation, LocalDateTime.now()));
        wearHistoryRepository.save(WearHistory.record(otherUser, otherRecommendation, LocalDateTime.now()));
        refreshTokenService.issue(user);
        accountActionTokenService.issue(user, AccountActionTokenPurpose.PASSWORD_RESET);
        socialAccountRepository.save(SocialAccount.link(
                user,
                OAuthProvider.GOOGLE,
                "google-delete-password",
                user.getEmail(),
                LocalDateTime.now()
        ));
        String token = accessToken(user);

        mockMvc.perform(delete("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "confirmation", "DELETE",
                                "password", "password123!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deleted").value(true));

        Assertions.assertThat(userRepository.findById(user.getId())).isEmpty();
        Assertions.assertThat(clothingItemRepository.findById(clothing.getId())).isEmpty();
        Assertions.assertThat(recommendationResultRepository.findById(recommendation.getId())).isEmpty();
        Assertions.assertThat(recommendationResultItemRepository.findAll())
                .extracting(item -> item.getRecommendationResult().getUser().getId())
                .contains(otherUser.getId())
                .doesNotContain(user.getId());
        Assertions.assertThat(wearHistoryRepository.findAll())
                .extracting(history -> history.getUser().getId())
                .contains(otherUser.getId())
                .doesNotContain(user.getId());
        Assertions.assertThat(refreshSessionRepository.findAll())
                .extracting(session -> session.getUser().getId())
                .doesNotContain(user.getId());
        Assertions.assertThat(accountActionTokenRepository.findAll())
                .extracting(tokenEntity -> tokenEntity.getUser().getId())
                .doesNotContain(user.getId());
        Assertions.assertThat(socialAccountRepository.findAll())
                .extracting(account -> account.getUser().getId())
                .doesNotContain(user.getId());
        Assertions.assertThat(userRepository.findById(otherUser.getId())).isPresent();
        Assertions.assertThat(clothingItemRepository.findById(otherClothing.getId())).isPresent();
        Assertions.assertThat(recommendationResultRepository.findById(otherRecommendation.getId())).isPresent();

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));

        mockMvc.perform(get("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void deletePasswordAccountRequiresCurrentPassword() throws Exception {
        User user = userRepository.save(
                User.create("delete-wrong-password@example.com", passwordEncoder.encode("password123!"),
                        "Password User"));

        mockMvc.perform(delete("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "confirmation", "DELETE",
                                "password", "wrong-password"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        Assertions.assertThat(userRepository.findById(user.getId())).isPresent();
    }

    @Test
    void deletesGoogleOnlyAccountWithConfirmationOnly() throws Exception {
        User user = userRepository.save(User.createGoogleUser("google-delete@example.com", "Google Delete"));
        socialAccountRepository.save(SocialAccount.link(
                user,
                OAuthProvider.GOOGLE,
                "google-delete-only",
                user.getEmail(),
                LocalDateTime.now()
        ));

        mockMvc.perform(delete("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("confirmation", "DELETE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deleted").value(true));

        Assertions.assertThat(userRepository.findById(user.getId())).isEmpty();
        Assertions.assertThat(socialAccountRepository.findAll())
                .extracting(account -> account.getUser().getId())
                .doesNotContain(user.getId());
    }

    @Test
    void deleteAccountRequiresExactDeleteConfirmation() throws Exception {
        User user = userRepository.save(User.createGoogleUser("google-confirmation@example.com", "Google Confirm"));

        mockMvc.perform(delete("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("confirmation", "delete"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        Assertions.assertThat(userRepository.findById(user.getId())).isPresent();
    }

    private String accessToken(User user) {
        return jwtTokenProvider.createAccessToken(new CurrentUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getRole()
        ));
    }

    private ClothingItem clothing(User user, String name) {
        return ClothingItem.create(
                user,
                name,
                ClothingCategory.TOP,
                ClothingColor.WHITE,
                ClothingMaterial.COTTON,
                0,
                25,
                false
        );
    }

    private RecommendationResult recommendation(User user) {
        return RecommendationResult.create(
                user,
                WeatherCondition.of(12, WeatherType.CLOUDY, false, false),
                RecommendationScore.of(80, 30, 20, 20, 10, 0),
                "[]"
        );
    }
}

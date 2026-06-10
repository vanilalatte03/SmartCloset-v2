package com.smartcloset.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.domain.ClothingMaterial;
import com.smartcloset.clothing.repository.ClothingItemRepository;
import com.smartcloset.location.domain.LocationOption;
import com.smartcloset.location.domain.LocationSource;
import com.smartcloset.recommendation.domain.OutfitSlot;
import com.smartcloset.recommendation.domain.RecommendationFeedbackSentiment;
import com.smartcloset.recommendation.domain.RecommendationResult;
import com.smartcloset.recommendation.domain.RecommendationResultItem;
import com.smartcloset.recommendation.domain.RecommendationScore;
import com.smartcloset.recommendation.domain.RecommendationSituation;
import com.smartcloset.recommendation.domain.RecommendationThermalFeedback;
import com.smartcloset.recommendation.domain.WearHistory;
import com.smartcloset.recommendation.repository.RecommendationResultItemRepository;
import com.smartcloset.recommendation.repository.RecommendationResultRepository;
import com.smartcloset.recommendation.repository.WearHistoryRepository;
import com.smartcloset.security.CurrentUserPrincipal;
import com.smartcloset.security.JwtTokenProvider;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import com.smartcloset.weather.domain.ForecastPeriod;
import com.smartcloset.weather.domain.WeatherCondition;
import com.smartcloset.weather.domain.WeatherType;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "KMA_SERVICE_KEY=",
        "WEATHER_FALLBACK_ENABLED=true",
        "spring.jpa.properties.hibernate.query.fail_on_pagination_over_collection_fetch=true"
})
@Transactional
class RecommendationControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext webApplicationContext;

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
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void createsRecommendationResultWithItemsAndScoreBreakdown() throws Exception {
        User user = createUserWithP0Closet("recommendation-user");

        MvcResult result = mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.recommendationId").exists())
                .andExpect(jsonPath("$.data.situation").value("CASUAL"))
                .andExpect(jsonPath("$.data.forecastPeriod").value("CURRENT"))
                .andExpect(jsonPath("$.data.weather.temperature").value(12))
                .andExpect(jsonPath("$.data.weather.weatherType").value("CLOUDY"))
                .andExpect(jsonPath("$.data.weather.rainy").value(false))
                .andExpect(jsonPath("$.data.weather.windy").value(false))
                .andExpect(jsonPath("$.data.weather.location.code").value("SEOUL"))
                .andExpect(jsonPath("$.data.weather.location.name").value("서울특별시"))
                .andExpect(jsonPath("$.data.weather.location.fullName").value("서울특별시"))
                .andExpect(jsonPath("$.data.weather.location.nx").value(60))
                .andExpect(jsonPath("$.data.weather.location.ny").value(127))
                .andExpect(jsonPath("$.data.weather.location.source").value("MANUAL_SEARCH"))
                .andExpect(jsonPath("$.data.weather.source.provider").value("STATIC_FALLBACK"))
                .andExpect(jsonPath("$.data.weather.source.kmaUsed").value(false))
                .andExpect(jsonPath("$.data.weather.source.fallbackUsed").value(true))
                .andExpect(jsonPath("$.data.weather.source.baseDate").exists())
                .andExpect(jsonPath("$.data.weather.source.baseTime").exists())
                .andExpect(jsonPath("$.data.weather.source.forecastDate").exists())
                .andExpect(jsonPath("$.data.weather.source.forecastTime").exists())
                .andExpect(jsonPath("$.data.outfit.top.category").value("TOP"))
                .andExpect(jsonPath("$.data.outfit.bottom.category").value("BOTTOM"))
                .andExpect(jsonPath("$.data.outfit.outer.category").value("OUTER"))
                .andExpect(jsonPath("$.data.score.totalScore").exists())
                .andExpect(jsonPath("$.data.score.weatherScore").exists())
                .andExpect(jsonPath("$.data.score.colorScore").exists())
                .andExpect(jsonPath("$.data.score.wearHistoryScore").exists())
                .andExpect(jsonPath("$.data.score.recommendationHistoryScore").exists())
                .andExpect(jsonPath("$.data.score.preferenceScore").value(0))
                .andExpect(jsonPath("$.data.reasons").isArray())
                .andExpect(jsonPath("$.data.worn").value(false))
                .andExpect(jsonPath("$.data.wornAt").doesNotExist())
                .andExpect(jsonPath("$.data.feedback").doesNotExist())
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        long recommendationId = data.get("recommendationId").asLong();
        RecommendationResult saved = recommendationResultRepository.findById(recommendationId).orElseThrow();
        List<String> savedReasons = objectMapper.readValue(
                saved.getReasonsJson(),
                new TypeReference<>() {
                }
        );
        List<RecommendationResultItem> savedItems = recommendationResultItemRepository
                .findByRecommendationResultIdInWithClothingItem(List.of(recommendationId));
        Set<OutfitSlot> slots = savedItems.stream()
                .map(item -> item.getSlot())
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(OutfitSlot.class)));

        assertThat(saved.getUser().getId()).isEqualTo(user.getId());
        assertThat(saved.getSituation().name()).isEqualTo("CASUAL");
        assertThat(saved.getForecastPeriod()).isEqualTo(ForecastPeriod.CURRENT);
        assertThat(saved.getWeatherTemperature()).isEqualTo(12);
        assertThat(saved.getWeatherType()).isEqualTo(WeatherType.CLOUDY);
        assertThat(saved.isRainy()).isFalse();
        assertThat(saved.isWindy()).isFalse();
        assertThat(saved.getWeatherLocationCode()).isEqualTo("SEOUL");
        assertThat(saved.getWeatherLocationSource()).isEqualTo(LocationSource.MANUAL_SEARCH);
        assertThat(saved.getWeatherProvider().name()).isEqualTo("STATIC_FALLBACK");
        assertThat(saved.isWeatherKmaUsed()).isFalse();
        assertThat(saved.isWeatherFallbackUsed()).isTrue();
        assertThat(savedItems).hasSize(3);
        assertThat(slots).containsExactlyInAnyOrder(OutfitSlot.TOP, OutfitSlot.BOTTOM, OutfitSlot.OUTER);
        assertThat(data.get("score").has("diversity" + "Score")).isFalse();
        assertThat(savedReasons).hasSizeBetween(3, 5);
        assertThat(saved.isWorn()).isFalse();
    }

    @Test
    void createsRecommendationWithRequestedForecastPeriodSnapshot() throws Exception {
        User user = createUserWithP0Closet("recommendation-forecast-user");

        MvcResult result = mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"forecastPeriod\":\"AFTERNOON\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.situation").value("CASUAL"))
                .andExpect(jsonPath("$.data.forecastPeriod").value("AFTERNOON"))
                .andExpect(jsonPath("$.data.weather.source.forecastTime").value("1500"))
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        RecommendationResult saved = recommendationResultRepository
                .findById(data.get("recommendationId").asLong())
                .orElseThrow();

        assertThat(saved.getSituation().name()).isEqualTo("CASUAL");
        assertThat(saved.getForecastPeriod()).isEqualTo(ForecastPeriod.AFTERNOON);
        assertThat(saved.getWeatherForecastTime()).isEqualTo("1500");
    }

    @Test
    void createsRecommendationWithRequestedSituationSnapshot() throws Exception {
        User user = createUserWithP0Closet("recommendation-situation-user");

        MvcResult result = mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"situation\":\"WORK\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.situation").value("WORK"))
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        RecommendationResult saved = recommendationResultRepository
                .findById(data.get("recommendationId").asLong())
                .orElseThrow();

        assertThat(saved.getSituation().name()).isEqualTo("WORK");
        assertThat(saved.getForecastPeriod()).isEqualTo(ForecastPeriod.CURRENT);
    }

    @Test
    void defaultsRecommendationSituationWhenRequestBodyOmitsSituation() throws Exception {
        User user = createUserWithP0Closet("recommendation-default-situation-user");

        MvcResult result = mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.situation").value("CASUAL"))
                .andExpect(jsonPath("$.data.forecastPeriod").value("CURRENT"))
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        RecommendationResult saved = recommendationResultRepository
                .findById(data.get("recommendationId").asLong())
                .orElseThrow();

        assertThat(saved.getSituation().name()).isEqualTo("CASUAL");
        assertThat(saved.getForecastPeriod()).isEqualTo(ForecastPeriod.CURRENT);
    }

    @Test
    void rejectsInvalidForecastPeriod() throws Exception {
        User user = createUserWithP0Closet("recommendation-invalid-forecast-user");

        mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"forecastPeriod\":\"LUNCH\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FORMAT"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void rejectsInvalidRecommendationSituation() throws Exception {
        User user = createUserWithP0Closet("recommendation-invalid-situation-user");

        mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"situation\":\"TRAVEL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FORMAT"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void appliesPreferenceScoreAndReasonFromPreferredColorsAndMaterials() throws Exception {
        User user = createUserWithP0Closet(
                "preference-user",
                "[\"NAVY\"]",
                "[\"WOOL\"]",
                "[\"MINIMAL\"]"
        );

        MvcResult result = mockMvc.perform(post("/api/recommendations")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.score.preferenceScore").value(4))
                .andExpect(jsonPath("$.data.reasons").isArray())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        long recommendationId = data.get("recommendationId").asLong();
        RecommendationResult saved = recommendationResultRepository.findById(recommendationId).orElseThrow();
        List<String> reasons = objectMapper.readValue(
                data.get("reasons").toString(),
                new TypeReference<>() {
                }
        );

        assertThat(saved.getPreferenceScore()).isEqualTo(4);
        assertThat(data.get("score").has("diversity" + "Score")).isFalse();
        assertThat(reasons).contains("선호 색상, 소재 또는 스타일 태그를 일부 반영했습니다.");
    }

    @Test
    void changingOnlyStyleTagsChangesRecommendationScoreAndReasons() throws Exception {
        User emptyStyleTagsUser = createUserWithP0Closet("empty-style-tags-user", "[]", "[]", "[]");
        User styleTagsUser = createUserWithP0Closet("style-tags-user", "[]", "[]", "[\"MINIMAL\"]");
        ClothingItem styleTaggedTop = findActiveClothingByCategory(styleTagsUser, ClothingCategory.TOP);
        styleTaggedTop.updateDetails(
                styleTaggedTop.getName(),
                styleTaggedTop.getCategory(),
                styleTaggedTop.getColor(),
                styleTaggedTop.getMaterial(),
                styleTaggedTop.getMinTemperature(),
                styleTaggedTop.getMaxTemperature(),
                styleTaggedTop.isRainSuitable(),
                "[\"MINIMAL\"]"
        );
        clothingItemRepository.flush();

        MvcResult emptyStyleTagsResult = mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(emptyStyleTagsUser)))
                .andExpect(status().isCreated())
                .andReturn();
        MvcResult styleTagsResult = mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(styleTagsUser)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode emptyStyleTagsData = objectMapper.readTree(emptyStyleTagsResult.getResponse().getContentAsString())
                .get("data");
        JsonNode styleTagsData = objectMapper.readTree(styleTagsResult.getResponse().getContentAsString())
                .get("data");

        assertThat(styleTagsData.get("score").get("preferenceScore").asInt())
                .isGreaterThan(emptyStyleTagsData.get("score").get("preferenceScore").asInt());
        assertThat(styleTagsData.get("reasons").toString())
                .contains("선호하는 스타일 태그와 겹치는 옷을 반영했어요.")
                .contains("캐주얼 상황에 맞는 스타일 태그를 반영했어요.");
    }

    @Test
    void includesNullableImageMetadataWithoutChangingRecommendationScoreOrReasons() throws Exception {
        User noImageUser = createUserWithP0Closet("recommendation-no-image-user");
        User imageUser = createUserWithP0Closet("recommendation-image-user");
        ClothingItem imageTop = findActiveClothingByCategory(imageUser, ClothingCategory.TOP);
        imageTop.updateImageMetadata("top-image.jpg", "image/jpeg", 123_456L, LocalDateTime.of(2026, 5, 25, 10, 0));
        clothingItemRepository.flush();

        MvcResult noImageResult = mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(noImageUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.outfit.top.image").doesNotExist())
                .andReturn();
        MvcResult imageResult = mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(imageUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.outfit.top.image.url").value("/api/clothes/" + imageTop.getId() + "/image"))
                .andExpect(jsonPath("$.data.outfit.top.image.contentType").value("image/jpeg"))
                .andExpect(jsonPath("$.data.outfit.top.image.sizeBytes").value(123_456L))
                .andExpect(jsonPath("$.data.outfit.top.image.uploadedAt").exists())
                .andReturn();

        JsonNode noImageData = objectMapper.readTree(noImageResult.getResponse().getContentAsString()).get("data");
        JsonNode imageData = objectMapper.readTree(imageResult.getResponse().getContentAsString()).get("data");
        assertThat(noImageData.get("outfit").get("top").get("image").isNull()).isTrue();
        assertThat(imageData.get("score")).isEqualTo(noImageData.get("score"));
        assertThat(imageData.get("reasons")).isEqualTo(noImageData.get("reasons"));
        assertThat(imageData.has("userId")).isFalse();

        mockMvc.perform(get("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(imageUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].outfit.top.image.url").value("/api/clothes/" + imageTop.getId() + "/image"))
                .andExpect(jsonPath("$.data[0].outfit.bottom.image").doesNotExist())
                .andExpect(jsonPath("$.data[0].userId").doesNotExist());
    }

    @Test
    void includesClothingStyleTagsInRecommendationOutfitItems() throws Exception {
        User user = userRepository.save(User.createSeedUser("recommendation-style-tags-user"));
        clothingItemRepository.save(ClothingItem.create(
                user,
                "미니멀 니트",
                ClothingCategory.TOP,
                ClothingColor.WHITE,
                ClothingMaterial.KNIT,
                0,
                16,
                false,
                "[\"MINIMAL\",\"OFFICE\"]"
        ));
        clothingItemRepository.save(ClothingItem.create(
                user,
                "데일리 데님",
                ClothingCategory.BOTTOM,
                ClothingColor.BLACK,
                ClothingMaterial.DENIM,
                0,
                22,
                false,
                "[\"CASUAL\"]"
        ));
        clothingItemRepository.save(ClothingItem.create(
                user,
                "네이비 코트",
                ClothingCategory.OUTER,
                ClothingColor.NAVY,
                ClothingMaterial.WOOL,
                -10,
                12,
                false,
                "[]"
        ));
        clothingItemRepository.flush();

        mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.outfit.top.styleTags[0]").value("MINIMAL"))
                .andExpect(jsonPath("$.data.outfit.top.styleTags[1]").value("OFFICE"))
                .andExpect(jsonPath("$.data.outfit.bottom.styleTags[0]").value("CASUAL"))
                .andExpect(jsonPath("$.data.outfit.outer.styleTags").isArray())
                .andExpect(jsonPath("$.data.outfit.outer.styleTags").isEmpty());
    }

    @Test
    void marksRecommendationWornIdempotentlyWithoutDuplicatingWearHistory() throws Exception {
        User user = createUserWithP0Closet("worn-user");
        long recommendationId = createRecommendation(user);

        MvcResult firstResult = mockMvc.perform(patch("/api/recommendations/{recommendationId}/worn", recommendationId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendationId").value(recommendationId))
                .andExpect(jsonPath("$.data.worn").value(true))
                .andExpect(jsonPath("$.data.wornAt").exists())
                .andReturn();

        long wearHistoryCount = wearHistoryRepository.count();
        String firstWornAt = objectMapper.readTree(firstResult.getResponse().getContentAsString())
                .get("data")
                .get("wornAt")
                .asText();

        MvcResult secondResult = mockMvc.perform(patch("/api/recommendations/{recommendationId}/worn", recommendationId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendationId").value(recommendationId))
                .andExpect(jsonPath("$.data.worn").value(true))
                .andReturn();

        String secondWornAt = objectMapper.readTree(secondResult.getResponse().getContentAsString())
                .get("data")
                .get("wornAt")
                .asText();
        RecommendationResult saved = recommendationResultRepository.findById(recommendationId).orElseThrow();

        assertThat(wearHistoryRepository.count()).isEqualTo(wearHistoryCount);
        assertThat(secondWornAt).isEqualTo(firstWornAt);
        assertThat(saved.isWorn()).isTrue();
    }

    @Test
    void replacesRecommendationFeedbackSnapshotAndClearsWithEmptyRequest() throws Exception {
        User user = createUserWithP0Closet("feedback-user");
        long recommendationId = createRecommendation(user);

        mockMvc.perform(put("/api/recommendations/{recommendationId}/feedback", recommendationId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sentiment\":\"LIKED\",\"thermal\":\"TOO_COLD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendationId").value(recommendationId))
                .andExpect(jsonPath("$.data.feedback.sentiment").value("LIKED"))
                .andExpect(jsonPath("$.data.feedback.thermal").value("TOO_COLD"))
                .andExpect(jsonPath("$.data.feedback.updatedAt").exists());

        RecommendationResult savedFeedback = recommendationResultRepository.findById(recommendationId).orElseThrow();
        assertThat(savedFeedback.getSentimentFeedback().name()).isEqualTo("LIKED");
        assertThat(savedFeedback.getThermalFeedback().name()).isEqualTo("TOO_COLD");
        assertThat(savedFeedback.getFeedbackUpdatedAt()).isNotNull();

        mockMvc.perform(put("/api/recommendations/{recommendationId}/feedback", recommendationId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sentiment\":\"DISLIKED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feedback.sentiment").value("DISLIKED"))
                .andExpect(jsonPath("$.data.feedback.thermal").doesNotExist())
                .andExpect(jsonPath("$.data.feedback.updatedAt").exists());

        RecommendationResult replacedFeedback = recommendationResultRepository.findById(recommendationId).orElseThrow();
        assertThat(replacedFeedback.getSentimentFeedback().name()).isEqualTo("DISLIKED");
        assertThat(replacedFeedback.getThermalFeedback()).isNull();
        assertThat(replacedFeedback.getFeedbackUpdatedAt()).isNotNull();

        mockMvc.perform(put("/api/recommendations/{recommendationId}/feedback", recommendationId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendationId").value(recommendationId))
                .andExpect(jsonPath("$.data.feedback").doesNotExist());

        RecommendationResult clearedFeedback = recommendationResultRepository.findById(recommendationId).orElseThrow();
        assertThat(clearedFeedback.getSentimentFeedback()).isNull();
        assertThat(clearedFeedback.getThermalFeedback()).isNull();
        assertThat(clearedFeedback.getFeedbackUpdatedAt()).isNull();
    }

    @Test
    void clearsRecommendationFeedbackSnapshotWithExplicitNulls() throws Exception {
        User user = createUserWithP0Closet("feedback-explicit-clear-user");
        long recommendationId = createRecommendation(user);

        mockMvc.perform(put("/api/recommendations/{recommendationId}/feedback", recommendationId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sentiment\":\"LIKED\",\"thermal\":\"TOO_HOT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feedback.sentiment").value("LIKED"))
                .andExpect(jsonPath("$.data.feedback.thermal").value("TOO_HOT"));

        mockMvc.perform(put("/api/recommendations/{recommendationId}/feedback", recommendationId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sentiment\":null,\"thermal\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feedback").doesNotExist());

        RecommendationResult clearedFeedback = recommendationResultRepository.findById(recommendationId).orElseThrow();
        assertThat(clearedFeedback.hasFeedback()).isFalse();
        assertThat(clearedFeedback.getFeedbackUpdatedAt()).isNull();
    }

    @Test
    void returnsRecommendationNotFoundWhenFeedbackTargetDoesNotBelongToUser() throws Exception {
        User owner = createUserWithP0Closet("feedback-owner-user");
        User otherUser = createUserWithP0Closet("feedback-other-user");
        long recommendationId = createRecommendation(owner);

        mockMvc.perform(put("/api/recommendations/{recommendationId}/feedback", recommendationId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(otherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sentiment\":\"LIKED\",\"thermal\":\"TOO_COLD\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECOMMENDATION_NOT_FOUND"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void returnsRecommendationFailureAsUnprocessableEntity() throws Exception {
        User user = userRepository.save(User.createSeedUser("failure-user"));
        clothingItemRepository.save(ClothingItem.create(
                user,
                "더운 날 셔츠",
                ClothingCategory.TOP,
                ClothingColor.WHITE,
                ClothingMaterial.COTTON,
                20,
                30,
                false
        ));
        clothingItemRepository.save(ClothingItem.create(
                user,
                "블랙 팬츠",
                ClothingCategory.BOTTOM,
                ClothingColor.BLACK,
                ClothingMaterial.DENIM,
                0,
                22,
                false
        ));
        clothingItemRepository.save(ClothingItem.create(
                user,
                "네이비 코트",
                ClothingCategory.OUTER,
                ClothingColor.NAVY,
                ClothingMaterial.WOOL,
                -10,
                12,
                false
        ));
        clothingItemRepository.flush();

        mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("NO_TOP_AVAILABLE"))
                .andExpect(jsonPath("$.message").value("현재 날씨에 입을 수 있는 상의가 없습니다."))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void returnsRecommendationNotFoundWhenWornTargetDoesNotBelongToUser() throws Exception {
        User owner = createUserWithP0Closet("owner-user");
        User otherUser = createUserWithP0Closet("other-user");
        long recommendationId = createRecommendation(owner);

        mockMvc.perform(patch("/api/recommendations/{recommendationId}/worn", recommendationId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(otherUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECOMMENDATION_NOT_FOUND"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void recommendationApisRequireBearerToken() throws Exception {
        mockMvc.perform(post("/api/recommendations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/recommendations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(patch("/api/recommendations/{recommendationId}/worn", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(put("/api/recommendations/{recommendationId}/feedback", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void returnsRecommendationHistoryForCurrentUserLatestFirstWithLimitPolicy() throws Exception {
        User targetUser = createUserWithP0Closet("history-target-user");
        User otherUser = createUserWithP0Closet("history-other-user");
        createRecommendation(targetUser);
        long latestTargetRecommendationId = createRecommendation(targetUser);
        long otherRecommendationId = createRecommendation(otherUser);
        mockMvc.perform(patch("/api/recommendations/{recommendationId}/worn", latestTargetRecommendationId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(targetUser)))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/recommendations/{recommendationId}/feedback", latestTargetRecommendationId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(targetUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sentiment\":\"LIKED\",\"thermal\":\"TOO_COLD\"}"))
                .andExpect(status().isOk());

        MvcResult defaultLimitResult = mockMvc.perform(get("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(targetUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].recommendationId").value(latestTargetRecommendationId))
                .andExpect(jsonPath("$.data[0].situation").value("CASUAL"))
                .andExpect(jsonPath("$.data[0].forecastPeriod").value("CURRENT"))
                .andExpect(jsonPath("$.data[0].weather.location.code").value("SEOUL"))
                .andExpect(jsonPath("$.data[0].weather.source.provider").value("STATIC_FALLBACK"))
                .andExpect(jsonPath("$.data[0].score.preferenceScore").exists())
                .andExpect(jsonPath("$.data[0].score.diversityScore").doesNotExist())
                .andExpect(jsonPath("$.data[0].outfit.top").exists())
                .andExpect(jsonPath("$.data[0].worn").value(true))
                .andExpect(jsonPath("$.data[0].wornAt").exists())
                .andExpect(jsonPath("$.data[0].feedback.sentiment").value("LIKED"))
                .andExpect(jsonPath("$.data[0].feedback.thermal").value("TOO_COLD"))
                .andExpect(jsonPath("$.data[0].feedback.updatedAt").exists())
                .andExpect(jsonPath("$.data[0].userId").doesNotExist())
                .andReturn();

        JsonNode defaultData = objectMapper.readTree(defaultLimitResult.getResponse().getContentAsString()).get("data");
        assertThat(defaultData).allSatisfy(history ->
                assertThat(history.get("recommendationId").asLong()).isNotEqualTo(otherRecommendationId));

        mockMvc.perform(get("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(targetUser))
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].recommendationId").value(latestTargetRecommendationId));

        mockMvc.perform(get("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(targetUser))
                        .param("limit", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(targetUser))
                        .param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAGINATION"));

        mockMvc.perform(get("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(targetUser))
                        .param("limit", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAGINATION"));

        mockMvc.perform(get("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(targetUser))
                        .param("limit", "many"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAGINATION"));

        mockMvc.perform(get("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(targetUser))
                        .param("limit", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAGINATION"));
    }

    @Test
    void recommendationCreationUsesBoundedScoringHistoryWindows() throws Exception {
        User user = createUserWithP0Closet("history-window-user", "[\"NAVY\"]", "[\"WOOL\"]", "[]");
        ClothingItem activeTop = findActiveClothingByCategory(user, ClothingCategory.TOP);
        ClothingItem activeBottom = findActiveClothingByCategory(user, ClothingCategory.BOTTOM);
        ClothingItem activeOuter = findActiveClothingByCategory(user, ClothingCategory.OUTER);
        ClothingItem archivedTop = createArchivedClothing(user, "이력 전용 상의", ClothingCategory.TOP);
        ClothingItem archivedBottom = createArchivedClothing(user, "이력 전용 하의", ClothingCategory.BOTTOM);
        ClothingItem archivedOuter = createArchivedClothing(user, "이력 전용 아우터", ClothingCategory.OUTER);
        LocalDateTime baseTime = LocalDateTime.now().minusSeconds(1);

        createHistoricalRecommendation(
                user,
                activeTop,
                activeBottom,
                activeOuter,
                baseTime.minusMinutes(60),
                baseTime.minusMinutes(60),
                RecommendationFeedbackSentiment.DISLIKED,
                RecommendationThermalFeedback.TOO_COLD
        );
        for (int minutesAgo = 50; minutesAgo >= 1; minutesAgo--) {
            createHistoricalRecommendation(
                    user,
                    archivedTop,
                    archivedBottom,
                    archivedOuter,
                    baseTime.minusMinutes(minutesAgo),
                    baseTime.minusMinutes(minutesAgo),
                    RecommendationFeedbackSentiment.LIKED,
                    null
            );
        }
        recommendationResultRepository.flush();
        recommendationResultItemRepository.flush();
        wearHistoryRepository.flush();

        mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.score.wearHistoryScore").value(20))
                .andExpect(jsonPath("$.data.score.recommendationHistoryScore").value(10))
                .andExpect(jsonPath("$.data.score.preferenceScore").value(4));
    }

    @Test
    void recommendationHistoryReturnsPersistedWeatherSnapshotAfterUserLocationChanges() throws Exception {
        User user = createUserWithP0Closet("history-weather-snapshot-user");
        long recommendationId = createRecommendation(user);
        RecommendationResult saved = recommendationResultRepository.findById(recommendationId).orElseThrow();
        assertThat(saved.getWeatherLocationCode()).isEqualTo("SEOUL");

        user.updateLocation(
                new LocationOption(
                        "KMA_4128751000",
                        "일산1동",
                        "경기도 고양시일산서구 일산1동",
                        "경기도",
                        "고양시일산서구",
                        "일산1동",
                        56,
                        129,
                        null,
                        null
                ),
                LocationSource.BROWSER_GEOLOCATION
        );
        userRepository.flush();

        mockMvc.perform(get("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].recommendationId").value(recommendationId))
                .andExpect(jsonPath("$.data[0].forecastPeriod").value("CURRENT"))
                .andExpect(jsonPath("$.data[0].weather.location.code").value("SEOUL"))
                .andExpect(jsonPath("$.data[0].weather.location.name").value("서울특별시"))
                .andExpect(jsonPath("$.data[0].weather.location.fullName").value("서울특별시"))
                .andExpect(jsonPath("$.data[0].weather.location.nx").value(60))
                .andExpect(jsonPath("$.data[0].weather.location.ny").value(127))
                .andExpect(jsonPath("$.data[0].weather.location.source").value("MANUAL_SEARCH"));
    }

    private long createRecommendation(User user) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data")
                .get("recommendationId")
                .asLong();
    }

    private void createHistoricalRecommendation(
            User user,
            ClothingItem top,
            ClothingItem bottom,
            ClothingItem outer,
            LocalDateTime wornAt,
            LocalDateTime feedbackUpdatedAt,
            RecommendationFeedbackSentiment sentimentFeedback,
            RecommendationThermalFeedback thermalFeedback
    ) {
        RecommendationResult recommendationResult = RecommendationResult.create(
                user,
                RecommendationSituation.CASUAL,
                WeatherCondition.of(12, WeatherType.CLOUDY, false, false),
                RecommendationScore.of(80, 30, 20, 20, 10, 0),
                "[]"
        );
        recommendationResult.replaceFeedback(sentimentFeedback, thermalFeedback, feedbackUpdatedAt);
        RecommendationResult saved = recommendationResultRepository.save(recommendationResult);
        recommendationResultItemRepository.saveAll(List.of(
                RecommendationResultItem.of(saved, top, OutfitSlot.TOP),
                RecommendationResultItem.of(saved, bottom, OutfitSlot.BOTTOM),
                RecommendationResultItem.of(saved, outer, OutfitSlot.OUTER)
        ));
        saved.markWorn();
        wearHistoryRepository.save(WearHistory.record(user, saved, wornAt));
    }

    private ClothingItem createArchivedClothing(User user, String name, ClothingCategory category) {
        ClothingItem item = ClothingItem.create(
                user,
                name,
                category,
                ClothingColor.GRAY,
                ClothingMaterial.POLYESTER,
                0,
                20,
                false
        );
        item.archive();
        return clothingItemRepository.save(item);
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtTokenProvider.createAccessToken(new CurrentUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getRole()
        ));
    }

    private User createUserWithP0Closet(String name) {
        return createUserWithP0Closet(name, "[]", "[]", "[]");
    }

    private User createUserWithP0Closet(
            String name,
            String preferredColorsJson,
            String preferredMaterialsJson,
            String styleTagsJson
    ) {
        User user = userRepository.save(User.createSeedUser(name));
        user.updatePreferences(preferredColorsJson, preferredMaterialsJson, styleTagsJson);
        clothingItemRepository.save(ClothingItem.create(
                user,
                "아이보리 니트",
                ClothingCategory.TOP,
                ClothingColor.WHITE,
                ClothingMaterial.KNIT,
                0,
                16,
                false
        ));
        clothingItemRepository.save(ClothingItem.create(
                user,
                "블랙 데님",
                ClothingCategory.BOTTOM,
                ClothingColor.BLACK,
                ClothingMaterial.DENIM,
                0,
                22,
                false
        ));
        clothingItemRepository.save(ClothingItem.create(
                user,
                "네이비 코트",
                ClothingCategory.OUTER,
                ClothingColor.NAVY,
                ClothingMaterial.WOOL,
                -10,
                12,
                false
        ));
        clothingItemRepository.flush();
        return user;
    }

    private ClothingItem findActiveClothingByCategory(User user, ClothingCategory category) {
        return clothingItemRepository.findByUserIdAndArchivedFalseOrderByIdAsc(user.getId())
                .stream()
                .filter(item -> item.getCategory() == category)
                .findFirst()
                .orElseThrow();
    }
}

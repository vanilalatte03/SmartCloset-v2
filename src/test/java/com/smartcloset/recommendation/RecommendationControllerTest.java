package com.smartcloset.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.smartcloset.recommendation.domain.OutfitSlot;
import com.smartcloset.recommendation.domain.RecommendationResult;
import com.smartcloset.recommendation.repository.RecommendationResultRepository;
import com.smartcloset.recommendation.repository.WearHistoryRepository;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import com.smartcloset.weather.domain.WeatherType;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "KMA_SERVICE_KEY=",
        "WEATHER_FALLBACK_ENABLED=true"
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
    private WearHistoryRepository wearHistoryRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void createsRecommendationResultWithItemsAndScoreBreakdown() throws Exception {
        User user = createUserWithP0Closet("recommendation-user");

        MvcResult result = mockMvc.perform(post("/api/recommendations")
                        .param("userId", user.getId().toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.recommendationId").exists())
                .andExpect(jsonPath("$.data.weather.temperature").value(12))
                .andExpect(jsonPath("$.data.weather.weatherType").value("CLOUDY"))
                .andExpect(jsonPath("$.data.weather.rainy").value(false))
                .andExpect(jsonPath("$.data.weather.windy").value(false))
                .andExpect(jsonPath("$.data.outfit.top.category").value("TOP"))
                .andExpect(jsonPath("$.data.outfit.bottom.category").value("BOTTOM"))
                .andExpect(jsonPath("$.data.outfit.outer.category").value("OUTER"))
                .andExpect(jsonPath("$.data.score.totalScore").exists())
                .andExpect(jsonPath("$.data.score.weatherScore").exists())
                .andExpect(jsonPath("$.data.score.colorScore").exists())
                .andExpect(jsonPath("$.data.score.wearHistoryScore").exists())
                .andExpect(jsonPath("$.data.score.recommendationHistoryScore").exists())
                .andExpect(jsonPath("$.data.score.diversityScore").exists())
                .andExpect(jsonPath("$.data.reasons").isArray())
                .andExpect(jsonPath("$.data.worn").value(false))
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
        Set<OutfitSlot> slots = saved.getItems().stream()
                .map(item -> item.getSlot())
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(OutfitSlot.class)));

        assertThat(saved.getUser().getId()).isEqualTo(user.getId());
        assertThat(saved.getWeatherTemperature()).isEqualTo(12);
        assertThat(saved.getWeatherType()).isEqualTo(WeatherType.CLOUDY);
        assertThat(saved.isRainy()).isFalse();
        assertThat(saved.isWindy()).isFalse();
        assertThat(saved.getItems()).hasSize(3);
        assertThat(slots).containsExactlyInAnyOrder(OutfitSlot.TOP, OutfitSlot.BOTTOM, OutfitSlot.OUTER);
        assertThat(savedReasons).hasSizeBetween(3, 5);
        assertThat(saved.isWorn()).isFalse();
    }

    @Test
    void marksRecommendationWornIdempotentlyWithoutDuplicatingWearHistory() throws Exception {
        User user = createUserWithP0Closet("worn-user");
        long recommendationId = createRecommendation(user.getId());

        MvcResult firstResult = mockMvc.perform(patch("/api/recommendations/{recommendationId}/worn", recommendationId)
                        .param("userId", user.getId().toString()))
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
                        .param("userId", user.getId().toString()))
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
                        .param("userId", user.getId().toString()))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("NO_TOP_AVAILABLE"))
                .andExpect(jsonPath("$.message").value("현재 날씨에 입을 수 있는 상의가 없습니다."))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void returnsRecommendationNotFoundWhenWornTargetDoesNotBelongToUser() throws Exception {
        User owner = createUserWithP0Closet("owner-user");
        User otherUser = createUserWithP0Closet("other-user");
        long recommendationId = createRecommendation(owner.getId());

        mockMvc.perform(patch("/api/recommendations/{recommendationId}/worn", recommendationId)
                        .param("userId", otherUser.getId().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECOMMENDATION_NOT_FOUND"))
                .andExpect(jsonPath("$.details").isArray());
    }

    private long createRecommendation(Long userId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/recommendations")
                        .param("userId", userId.toString()))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data")
                .get("recommendationId")
                .asLong();
    }

    private User createUserWithP0Closet(String name) {
        User user = userRepository.save(User.createSeedUser(name));
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
}

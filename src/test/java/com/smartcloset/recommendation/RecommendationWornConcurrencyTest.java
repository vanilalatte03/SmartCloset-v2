package com.smartcloset.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.domain.ClothingMaterial;
import com.smartcloset.clothing.repository.ClothingItemRepository;
import com.smartcloset.recommendation.application.RecommendationService;
import com.smartcloset.recommendation.domain.RecommendationResult;
import com.smartcloset.recommendation.repository.RecommendationResultItemRepository;
import com.smartcloset.recommendation.repository.RecommendationResultRepository;
import com.smartcloset.recommendation.repository.WearHistoryRepository;
import com.smartcloset.security.CurrentUserPrincipal;
import com.smartcloset.security.JwtTokenProvider;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "KMA_SERVICE_KEY=",
        "WEATHER_FALLBACK_ENABLED=true",
        "spring.jpa.properties.hibernate.query.fail_on_pagination_over_collection_fetch=true"
})
class RecommendationWornConcurrencyTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private RecommendationService recommendationService;

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

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final List<Long> createdUserIds = new ArrayList<>();
    private TransactionTemplate transactionTemplate;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    void cleanup() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        transactionTemplate.executeWithoutResult(status -> {
            for (Long userId : createdUserIds) {
                wearHistoryRepository.deleteByUserId(userId);
                recommendationResultItemRepository.deleteByRecommendationResultUserId(userId);
                recommendationResultRepository.deleteByUserId(userId);
                clothingItemRepository.deleteByUserId(userId);
                userRepository.findById(userId).ifPresent(userRepository::delete);
            }
        });
    }

    @Test
    void concurrentWornRequestsReuseSingleWearHistory() throws Exception {
        User user = createUserWithP0Closet();
        Long recommendationId = recommendationService.createRecommendation(user.getId()).recommendationId();
        String bearerToken = bearerToken(user);
        executorService = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<WornAttempt> first = executorService.submit(wornAttempt(recommendationId, bearerToken, ready, start));
        Future<WornAttempt> second = executorService.submit(wornAttempt(recommendationId, bearerToken, ready, start));

        assertThat(ready.await(3, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        List<WornAttempt> attempts = List.of(first.get(3, TimeUnit.SECONDS), second.get(3, TimeUnit.SECONDS));

        assertThat(attempts).allSatisfy(attempt -> {
            assertThat(attempt.status()).isEqualTo(200);
            assertThat(attempt.recommendationId()).isEqualTo(recommendationId);
            assertThat(attempt.worn()).isTrue();
            assertThat(attempt.wornAt()).isNotBlank();
        });
        String savedWornAt = attempts.get(0).wornAt();
        assertThat(attempts)
                .extracting(WornAttempt::wornAt)
                .containsOnly(savedWornAt);
        assertThat(wearHistoryRepository.findByRecommendationResultIdIn(List.of(recommendationId)))
                .singleElement()
                .satisfies(history -> assertThat(history.getWornAt().toString()).isEqualTo(savedWornAt));
        RecommendationResult saved = recommendationResultRepository.findById(recommendationId).orElseThrow();
        assertThat(saved.isWorn()).isTrue();
    }

    private Callable<WornAttempt> wornAttempt(
            Long recommendationId,
            String bearerToken,
            CountDownLatch ready,
            CountDownLatch start
    ) {
        return () -> {
            ready.countDown();
            if (!start.await(3, TimeUnit.SECONDS)) {
                throw new IllegalStateException("concurrent worn start signal timed out");
            }
            MvcResult result = mockMvc.perform(patch("/api/recommendations/{recommendationId}/worn", recommendationId)
                            .with(csrf())
                            .header(HttpHeaders.AUTHORIZATION, bearerToken))
                    .andReturn();
            String body = result.getResponse().getContentAsString();
            if (result.getResponse().getStatus() != 200) {
                return WornAttempt.failure(result.getResponse().getStatus(), body);
            }
            var data = objectMapper.readTree(body).get("data");
            return new WornAttempt(
                    result.getResponse().getStatus(),
                    data.get("recommendationId").asLong(),
                    data.get("worn").asBoolean(),
                    data.get("wornAt").asText(),
                    body
            );
        };
    }

    private User createUserWithP0Closet() {
        return transactionTemplate.execute(status -> {
            String name = "worn-concurrent-" + System.nanoTime();
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
            createdUserIds.add(user.getId());
            return user;
        });
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtTokenProvider.createAccessToken(new CurrentUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getRole()
        ));
    }

    private record WornAttempt(
            int status,
            Long recommendationId,
            boolean worn,
            String wornAt,
            String body
    ) {

        static WornAttempt failure(int status, String body) {
            return new WornAttempt(status, null, false, null, body);
        }
    }
}

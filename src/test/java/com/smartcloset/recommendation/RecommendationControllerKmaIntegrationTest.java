package com.smartcloset.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
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
import com.smartcloset.location.domain.LocationOption;
import com.smartcloset.recommendation.domain.RecommendationResult;
import com.smartcloset.recommendation.repository.RecommendationResultRepository;
import com.smartcloset.security.CurrentUserPrincipal;
import com.smartcloset.security.JwtTokenProvider;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import com.smartcloset.weather.domain.WeatherType;
import com.smartcloset.weather.infrastructure.kma.KmaForecastBaseTime;
import com.smartcloset.weather.infrastructure.kma.KmaForecastBaseTimeCalculator;
import com.smartcloset.weather.infrastructure.kma.KmaForecastClient;
import com.smartcloset.weather.infrastructure.kma.KmaForecastClientException;
import com.smartcloset.weather.infrastructure.kma.KmaForecastItem;
import com.smartcloset.weather.infrastructure.kma.KmaGrid;
import com.smartcloset.weather.infrastructure.kma.KmaWeatherProperties;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "KMA_SERVICE_KEY=test-service-key",
        "WEATHER_FALLBACK_ENABLED=true"
})
@Import(RecommendationControllerKmaIntegrationTest.FakeKmaForecastClientConfig.class)
@Transactional
class RecommendationControllerKmaIntegrationTest {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmm");

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
    private KmaWeatherProperties weatherProperties;

    @Autowired
    private ControllableKmaForecastClient kmaForecastClient;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        weatherProperties.getKma().setServiceKey("test-service-key");
        weatherProperties.setFallbackEnabled(true);
        kmaForecastClient.reset();
    }

    @Test
    void createsRecommendationWithKmaWeatherSnapshotAndPersistsIt() throws Exception {
        User user = createUserWithKmaSuitableCloset("kma-success-user");
        kmaForecastClient.returning(completeFutureForecastGroup("18", "4", "1", "1.0mm", "4.2"));

        MvcResult result = mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.weather.temperature").value(18))
                .andExpect(jsonPath("$.data.weather.weatherType").value("RAINY"))
                .andExpect(jsonPath("$.data.weather.rainy").value(true))
                .andExpect(jsonPath("$.data.weather.windy").value(true))
                .andExpect(jsonPath("$.data.score.totalScore").exists())
                .andReturn();

        long recommendationId = recommendationIdFrom(result);
        RecommendationResult saved = recommendationResultRepository.findById(recommendationId).orElseThrow();

        assertThat(kmaForecastClient.called()).isTrue();
        assertThat(kmaForecastClient.requestedGrid()).isEqualTo(new KmaGrid(60, 127));
        assertThat(saved.getWeatherTemperature()).isEqualTo(18);
        assertThat(saved.getWeatherType()).isEqualTo(WeatherType.RAINY);
        assertThat(saved.isRainy()).isTrue();
        assertThat(saved.isWindy()).isTrue();
    }

    @Test
    void usesSelectedUserLocationGridWhenCallingKma() throws Exception {
        User user = createUserWithKmaSuitableCloset("kma-busan-user");
        user.updateLocation(new LocationOption("BUSAN", "부산광역시", 98, 76));
        userRepository.flush();
        kmaForecastClient.returning(completeFutureForecastGroup("18", "3", "0", "-", "2.0"));

        mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isCreated());

        assertThat(kmaForecastClient.requestedGrid()).isEqualTo(new KmaGrid(98, 76));
    }

    @Test
    void backfillsDefaultLocationAndUsesSeoulGridWhenUserLocationIsMissing() throws Exception {
        User user = createUserWithKmaSuitableCloset("kma-backfill-user", false);
        kmaForecastClient.returning(completeFutureForecastGroup("18", "3", "0", "-", "2.0"));

        mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isCreated());

        User saved = userRepository.findById(user.getId()).orElseThrow();
        assertThat(kmaForecastClient.requestedGrid()).isEqualTo(new KmaGrid(60, 127));
        assertThat(saved.getLocationCode()).isEqualTo("SEOUL");
        assertThat(saved.getLocationName()).isEqualTo("서울특별시");
        assertThat(saved.getLocationNx()).isEqualTo(60);
        assertThat(saved.getLocationNy()).isEqualTo(127);
    }

    @Test
    void fallsBackAndPersistsFallbackWeatherWhenKmaFailsAndFallbackEnabled() throws Exception {
        User user = createUserWithFallbackSuitableCloset("kma-fallback-user");
        kmaForecastClient.failing(new KmaForecastClientException("NODATA_ERROR"));

        MvcResult result = mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.weather.temperature").value(12))
                .andExpect(jsonPath("$.data.weather.weatherType").value("CLOUDY"))
                .andExpect(jsonPath("$.data.weather.rainy").value(false))
                .andExpect(jsonPath("$.data.weather.windy").value(false))
                .andReturn();

        long recommendationId = recommendationIdFrom(result);
        RecommendationResult saved = recommendationResultRepository.findById(recommendationId).orElseThrow();

        assertThat(kmaForecastClient.called()).isTrue();
        assertThat(saved.getWeatherTemperature()).isEqualTo(12);
        assertThat(saved.getWeatherType()).isEqualTo(WeatherType.CLOUDY);
        assertThat(saved.isRainy()).isFalse();
        assertThat(saved.isWindy()).isFalse();
    }

    @Test
    void returnsInternalServerErrorAndDoesNotPersistRecommendationWhenStrictKmaFails() throws Exception {
        User user = createUserWithFallbackSuitableCloset("kma-strict-user");
        weatherProperties.setFallbackEnabled(false);
        kmaForecastClient.failing(new KmaForecastClientException("SERVICE_ERROR"));
        long beforeCount = recommendationResultRepository.count();

        mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.details").isArray());

        assertThat(kmaForecastClient.called()).isTrue();
        assertThat(recommendationResultRepository.count()).isEqualTo(beforeCount);
    }

    private long recommendationIdFrom(MvcResult result) throws Exception {
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        return data.get("recommendationId").asLong();
    }

    private User createUserWithKmaSuitableCloset(String name) {
        return createUserWithKmaSuitableCloset(name, true);
    }

    private User createUserWithKmaSuitableCloset(String name, boolean seedLocation) {
        User user = userRepository.save(seedLocation ? User.createSeedUser(name) : User.create(name));
        clothingItemRepository.save(ClothingItem.create(
                user,
                "White cotton tee",
                ClothingCategory.TOP,
                ClothingColor.WHITE,
                ClothingMaterial.COTTON,
                0,
                30,
                true
        ));
        clothingItemRepository.save(ClothingItem.create(
                user,
                "Black denim",
                ClothingCategory.BOTTOM,
                ClothingColor.BLACK,
                ClothingMaterial.DENIM,
                0,
                30,
                true
        ));
        clothingItemRepository.save(ClothingItem.create(
                user,
                "Black nylon jacket",
                ClothingCategory.OUTER,
                ClothingColor.BLACK,
                ClothingMaterial.NYLON,
                0,
                30,
                true
        ));
        clothingItemRepository.flush();
        return user;
    }

    private User createUserWithFallbackSuitableCloset(String name) {
        User user = userRepository.save(User.createSeedUser(name));
        clothingItemRepository.save(ClothingItem.create(
                user,
                "Ivory knit",
                ClothingCategory.TOP,
                ClothingColor.WHITE,
                ClothingMaterial.KNIT,
                0,
                16,
                false
        ));
        clothingItemRepository.save(ClothingItem.create(
                user,
                "Black denim",
                ClothingCategory.BOTTOM,
                ClothingColor.BLACK,
                ClothingMaterial.DENIM,
                0,
                22,
                false
        ));
        clothingItemRepository.save(ClothingItem.create(
                user,
                "Navy coat",
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

    private List<KmaForecastItem> completeFutureForecastGroup(
            String tmp,
            String sky,
            String pty,
            String pcp,
            String wsd
    ) {
        ZonedDateTime forecastAt = ZonedDateTime.now(KmaForecastBaseTimeCalculator.KST_ZONE)
                .plusHours(25)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        String fcstDate = forecastAt.format(DATE_FORMATTER);
        String fcstTime = forecastAt.format(TIME_FORMATTER);
        return List.of(
                item(fcstDate, fcstTime, "TMP", tmp),
                item(fcstDate, fcstTime, "SKY", sky),
                item(fcstDate, fcstTime, "PTY", pty),
                item(fcstDate, fcstTime, "PCP", pcp),
                item(fcstDate, fcstTime, "WSD", wsd)
        );
    }

    private KmaForecastItem item(String fcstDate, String fcstTime, String category, String fcstValue) {
        return new KmaForecastItem(fcstDate, fcstTime, category, fcstValue);
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtTokenProvider.createAccessToken(new CurrentUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getRole()
        ));
    }

    @TestConfiguration
    static class FakeKmaForecastClientConfig {

        @Bean
        @Primary
        ControllableKmaForecastClient controllableKmaForecastClient() {
            return new ControllableKmaForecastClient();
        }
    }

    static final class ControllableKmaForecastClient implements KmaForecastClient {

        private List<KmaForecastItem> items = List.of();
        private KmaForecastClientException failure;
        private boolean called;
        private KmaGrid requestedGrid;

        @Override
        public List<KmaForecastItem> getVilageForecast(KmaForecastBaseTime baseTime, KmaGrid grid) {
            called = true;
            requestedGrid = grid;
            if (failure != null) {
                throw failure;
            }
            return items;
        }

        void returning(List<KmaForecastItem> items) {
            this.items = List.copyOf(items);
            this.failure = null;
        }

        void failing(KmaForecastClientException failure) {
            this.items = List.of();
            this.failure = failure;
        }

        void reset() {
            this.items = List.of();
            this.failure = null;
            this.called = false;
            this.requestedGrid = null;
        }

        boolean called() {
            return called;
        }

        KmaGrid requestedGrid() {
            return requestedGrid;
        }
    }
}

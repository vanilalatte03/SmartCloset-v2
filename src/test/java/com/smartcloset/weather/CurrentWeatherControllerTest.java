package com.smartcloset.weather;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smartcloset.recommendation.repository.RecommendationResultRepository;
import com.smartcloset.recommendation.repository.WearHistoryRepository;
import com.smartcloset.security.CurrentUserPrincipal;
import com.smartcloset.security.JwtTokenProvider;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import com.smartcloset.weather.infrastructure.kma.KmaWeatherProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "KMA_SERVICE_KEY=",
        "WEATHER_FALLBACK_ENABLED=true"
})
@Transactional
class CurrentWeatherControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecommendationResultRepository recommendationResultRepository;

    @Autowired
    private WearHistoryRepository wearHistoryRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private KmaWeatherProperties weatherProperties;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        weatherProperties.getKma().setServiceKey("");
        weatherProperties.setFallbackEnabled(true);
    }

    @AfterEach
    void tearDown() {
        weatherProperties.getKma().setServiceKey("");
        weatherProperties.setFallbackEnabled(true);
    }

    @Test
    void returnsFallbackCurrentWeatherWithoutUserIdOrRecommendationSideEffects() throws Exception {
        User user = userRepository.save(User.createSeedUser("current-weather-user"));
        long recommendationCount = recommendationResultRepository.count();
        long wearHistoryCount = wearHistoryRepository.count();

        mockMvc.perform(get("/api/weather/current")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.temperature").value(12))
                .andExpect(jsonPath("$.data.weatherType").value("CLOUDY"))
                .andExpect(jsonPath("$.data.rainy").value(false))
                .andExpect(jsonPath("$.data.windy").value(false))
                .andExpect(jsonPath("$.data.userId").doesNotExist());

        assertThat(recommendationResultRepository.count()).isEqualTo(recommendationCount);
        assertThat(wearHistoryRepository.count()).isEqualTo(wearHistoryCount);
    }

    @Test
    void returnsInternalServerErrorInStrictModeWithoutCreatingRecommendationOrWearHistory() throws Exception {
        User user = userRepository.save(User.createSeedUser("current-weather-strict-user"));
        weatherProperties.setFallbackEnabled(false);
        long recommendationCount = recommendationResultRepository.count();
        long wearHistoryCount = wearHistoryRepository.count();

        mockMvc.perform(get("/api/weather/current")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.details").isArray());

        assertThat(recommendationResultRepository.count()).isEqualTo(recommendationCount);
        assertThat(wearHistoryRepository.count()).isEqualTo(wearHistoryCount);
    }

    @Test
    void currentWeatherRejectsMissingAndMalformedBearerToken() throws Exception {
        mockMvc.perform(get("/api/weather/current"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.details").isArray());

        mockMvc.perform(get("/api/weather/current")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
                .andExpect(jsonPath("$.details").isArray());
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtTokenProvider.createAccessToken(new CurrentUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getRole()
        ));
    }
}

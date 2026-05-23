package com.smartcloset.weather;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smartcloset.location.domain.LocationOption;
import com.smartcloset.recommendation.repository.RecommendationResultRepository;
import com.smartcloset.recommendation.repository.WearHistoryRepository;
import com.smartcloset.security.CurrentUserPrincipal;
import com.smartcloset.security.JwtTokenProvider;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
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
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "KMA_SERVICE_KEY=test-service-key",
        "WEATHER_FALLBACK_ENABLED=true"
})
@Import(CurrentWeatherControllerKmaIntegrationTest.FakeKmaForecastClientConfig.class)
@Transactional
class CurrentWeatherControllerKmaIntegrationTest {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmm");

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

    @AfterEach
    void tearDown() {
        weatherProperties.getKma().setServiceKey("test-service-key");
        weatherProperties.setFallbackEnabled(true);
        kmaForecastClient.reset();
    }

    @Test
    void returnsKmaCurrentWeatherForSelectedUserLocationWithoutPersistenceSideEffects() throws Exception {
        User user = userRepository.save(User.createSeedUser("current-weather-kma-user"));
        user.updateLocation(new LocationOption("BUSAN", "부산광역시", 98, 76));
        userRepository.flush();
        kmaForecastClient.returning(completeFutureForecastGroup("18", "3", "0", "-", "2.0"));
        long recommendationCount = recommendationResultRepository.count();
        long wearHistoryCount = wearHistoryRepository.count();

        mockMvc.perform(get("/api/weather/current")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.temperature").value(18))
                .andExpect(jsonPath("$.data.weatherType").value("CLOUDY"))
                .andExpect(jsonPath("$.data.rainy").value(false))
                .andExpect(jsonPath("$.data.windy").value(false))
                .andExpect(jsonPath("$.data.userId").doesNotExist());

        assertThat(kmaForecastClient.called()).isTrue();
        assertThat(kmaForecastClient.requestedGrid()).isEqualTo(new KmaGrid(98, 76));
        assertThat(recommendationResultRepository.count()).isEqualTo(recommendationCount);
        assertThat(wearHistoryRepository.count()).isEqualTo(wearHistoryCount);
    }

    @Test
    void returnsInternalServerErrorWhenStrictKmaModeProviderFails() throws Exception {
        User user = userRepository.save(User.createSeedUser("current-weather-kma-strict-user"));
        weatherProperties.setFallbackEnabled(false);
        kmaForecastClient.failing(new KmaForecastClientException("SERVICE_ERROR"));
        long recommendationCount = recommendationResultRepository.count();
        long wearHistoryCount = wearHistoryRepository.count();

        mockMvc.perform(get("/api/weather/current")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.details").isArray());

        assertThat(kmaForecastClient.called()).isTrue();
        assertThat(recommendationResultRepository.count()).isEqualTo(recommendationCount);
        assertThat(wearHistoryRepository.count()).isEqualTo(wearHistoryCount);
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtTokenProvider.createAccessToken(new CurrentUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getRole()
        ));
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
                new KmaForecastItem(fcstDate, fcstTime, "TMP", tmp),
                new KmaForecastItem(fcstDate, fcstTime, "SKY", sky),
                new KmaForecastItem(fcstDate, fcstTime, "PTY", pty),
                new KmaForecastItem(fcstDate, fcstTime, "PCP", pcp),
                new KmaForecastItem(fcstDate, fcstTime, "WSD", wsd)
        );
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

package com.smartcloset.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.domain.ClothingMaterial;
import com.smartcloset.clothing.repository.ClothingItemRepository;
import com.smartcloset.security.CurrentUserPrincipal;
import com.smartcloset.security.JwtTokenProvider;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import com.smartcloset.weather.infrastructure.kma.KmaForecastBaseTime;
import com.smartcloset.weather.infrastructure.kma.KmaForecastBaseTimeCalculator;
import com.smartcloset.weather.infrastructure.kma.KmaForecastClient;
import com.smartcloset.weather.infrastructure.kma.KmaForecastItem;
import com.smartcloset.weather.infrastructure.kma.KmaGrid;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "KMA_SERVICE_KEY=test-service-key",
        "WEATHER_FALLBACK_ENABLED=true"
})
@Import(RecommendationTransactionBoundaryTest.RecordingKmaForecastClientConfig.class)
class RecommendationTransactionBoundaryTest {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmm");

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClothingItemRepository clothingItemRepository;

    @Autowired
    private RecordingKmaForecastClient kmaForecastClient;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        kmaForecastClient.reset();
        kmaForecastClient.returning(completeFutureForecastGroup());
    }

    @Test
    void callsKmaClientOutsideActiveSpringTransaction() throws Exception {
        User user = createUserWithKmaSuitableCloset();

        mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isCreated());

        assertThat(kmaForecastClient.called()).isTrue();
        assertThat(kmaForecastClient.transactionActiveDuringCall()).isFalse();
    }

    private User createUserWithKmaSuitableCloset() {
        User user = userRepository.save(User.createSeedUser("kma-transaction-boundary-user"));
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
        return user;
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtTokenProvider.createAccessToken(new CurrentUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getRole()
        ));
    }

    private List<KmaForecastItem> completeFutureForecastGroup() {
        ZonedDateTime forecastAt = ZonedDateTime.now(KmaForecastBaseTimeCalculator.KST_ZONE)
                .plusHours(25)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        String fcstDate = forecastAt.format(DATE_FORMATTER);
        String fcstTime = forecastAt.format(TIME_FORMATTER);
        return List.of(
                new KmaForecastItem(fcstDate, fcstTime, "TMP", "18"),
                new KmaForecastItem(fcstDate, fcstTime, "SKY", "3"),
                new KmaForecastItem(fcstDate, fcstTime, "PTY", "0"),
                new KmaForecastItem(fcstDate, fcstTime, "PCP", "-"),
                new KmaForecastItem(fcstDate, fcstTime, "WSD", "2.0")
        );
    }

    @TestConfiguration
    static class RecordingKmaForecastClientConfig {

        @Bean
        @Primary
        RecordingKmaForecastClient recordingKmaForecastClient() {
            return new RecordingKmaForecastClient();
        }
    }

    static final class RecordingKmaForecastClient implements KmaForecastClient {

        private List<KmaForecastItem> items = List.of();
        private boolean called;
        private boolean transactionActiveDuringCall;

        @Override
        public List<KmaForecastItem> getVilageForecast(KmaForecastBaseTime baseTime, KmaGrid grid) {
            called = true;
            transactionActiveDuringCall = TransactionSynchronizationManager.isActualTransactionActive();
            return items;
        }

        void returning(List<KmaForecastItem> items) {
            this.items = List.copyOf(items);
        }

        void reset() {
            this.items = List.of();
            this.called = false;
            this.transactionActiveDuringCall = false;
        }

        boolean called() {
            return called;
        }

        boolean transactionActiveDuringCall() {
            return transactionActiveDuringCall;
        }
    }
}

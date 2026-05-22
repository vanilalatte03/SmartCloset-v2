package com.smartcloset.weather.infrastructure.kma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import com.smartcloset.user.application.UserLocationReader;
import com.smartcloset.user.application.UserLocationSnapshot;
import com.smartcloset.weather.domain.WeatherCondition;
import com.smartcloset.weather.domain.WeatherType;
import com.smartcloset.weather.infrastructure.StaticWeatherProvider;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class KmaVilageForecastWeatherProviderTest {

    private final Clock clock = Clock.fixed(
            ZonedDateTime.parse("2026-05-21T14:15:00+09:00").toInstant(),
            KmaForecastBaseTimeCalculator.KST_ZONE
    );

    @Test
    void returnsFallbackWeatherWhenServiceKeyIsMissingAndFallbackEnabled() {
        FakeKmaForecastClient client = FakeKmaForecastClient.returning(completeGroup());
        KmaVilageForecastWeatherProvider provider = newProvider(properties("", true), client);

        WeatherCondition weather = provider.getCurrentWeather(1L);

        assertFallbackWeather(weather);
        assertThat(client.called()).isFalse();
    }

    @Test
    void returnsKmaMappedWeatherWhenServiceKeyIsConfiguredAndKmaResponseIsValid() {
        FakeKmaForecastClient client = FakeKmaForecastClient.returning(completeGroup());
        KmaVilageForecastWeatherProvider provider = newProvider(properties("test-service-key", true), client);

        WeatherCondition weather = provider.getCurrentWeather(1L);

        assertThat(client.requestedBaseTime()).isEqualTo(new KmaForecastBaseTime("20260521", "1400"));
        assertThat(client.requestedGrid()).isEqualTo(new KmaGrid(98, 76));
        assertThat(weather.temperature()).isEqualTo(13);
        assertThat(weather.weatherType()).isEqualTo(WeatherType.SUNNY);
        assertThat(weather.rainy()).isFalse();
        assertThat(weather.windy()).isFalse();
    }

    @Test
    void returnsFallbackWeatherWhenKmaClientFailsAndFallbackEnabled() {
        FakeKmaForecastClient client = FakeKmaForecastClient.failing(new KmaForecastClientException("NODATA_ERROR"));
        KmaVilageForecastWeatherProvider provider = newProvider(properties("test-service-key", true), client);

        WeatherCondition weather = provider.getCurrentWeather(1L);

        assertFallbackWeather(weather);
        assertThat(client.called()).isTrue();
    }

    @Test
    void returnsFallbackWeatherWhenKmaMappingFailsAndFallbackEnabled() {
        FakeKmaForecastClient client = FakeKmaForecastClient.returning(List.of(
                item("20260521", "1500", "TMP", "13"),
                item("20260521", "1500", "SKY", "1"),
                item("20260521", "1500", "PTY", "0"),
                item("20260521", "1500", "PCP", "-")
        ));
        KmaVilageForecastWeatherProvider provider = newProvider(properties("test-service-key", true), client);

        WeatherCondition weather = provider.getCurrentWeather(1L);

        assertFallbackWeather(weather);
    }

    @Test
    void throwsInternalServerErrorWhenServiceKeyIsMissingAndFallbackDisabled() {
        KmaVilageForecastWeatherProvider provider = newProvider(
                properties("", false),
                FakeKmaForecastClient.returning(completeGroup())
        );

        assertInternalServerError(provider);
    }

    @Test
    void throwsInternalServerErrorWhenKmaClientFailsAndFallbackDisabled() {
        KmaVilageForecastWeatherProvider provider = newProvider(
                properties("test-service-key", false),
                FakeKmaForecastClient.failing(new KmaForecastClientException("SERVICE_ERROR"))
        );

        assertInternalServerError(provider);
    }

    private KmaVilageForecastWeatherProvider newProvider(
            KmaWeatherProperties properties,
            KmaForecastClient client
    ) {
        return new KmaVilageForecastWeatherProvider(
                properties,
                client,
                new KmaForecastBaseTimeCalculator(),
                new KmaWeatherConditionMapper(),
                new StaticWeatherProvider(),
                new FakeUserLocationReader(),
                clock
        );
    }

    private KmaWeatherProperties properties(String serviceKey, boolean fallbackEnabled) {
        KmaWeatherProperties properties = new KmaWeatherProperties();
        properties.getKma().setServiceKey(serviceKey);
        properties.setFallbackEnabled(fallbackEnabled);
        return properties;
    }

    private List<KmaForecastItem> completeGroup() {
        return List.of(
                item("20260521", "1500", "TMP", "12.6"),
                item("20260521", "1500", "SKY", "1"),
                item("20260521", "1500", "PTY", "0"),
                item("20260521", "1500", "PCP", "-"),
                item("20260521", "1500", "WSD", "3.9")
        );
    }

    private static KmaForecastItem item(String fcstDate, String fcstTime, String category, String fcstValue) {
        return new KmaForecastItem(fcstDate, fcstTime, category, fcstValue);
    }

    private void assertFallbackWeather(WeatherCondition weather) {
        assertThat(weather.temperature()).isEqualTo(12);
        assertThat(weather.weatherType()).isEqualTo(WeatherType.CLOUDY);
        assertThat(weather.rainy()).isFalse();
        assertThat(weather.windy()).isFalse();
    }

    private void assertInternalServerError(KmaVilageForecastWeatherProvider provider) {
        assertThatThrownBy(() -> provider.getCurrentWeather(1L))
                .isInstanceOf(SmartClosetException.class)
                .satisfies(exception -> assertThat(((SmartClosetException) exception).errorCode())
                        .isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    private static final class FakeKmaForecastClient implements KmaForecastClient {

        private final List<KmaForecastItem> items;
        private final KmaForecastClientException failure;
        private boolean called;
        private KmaForecastBaseTime requestedBaseTime;
        private KmaGrid requestedGrid;

        private FakeKmaForecastClient(List<KmaForecastItem> items, KmaForecastClientException failure) {
            this.items = items;
            this.failure = failure;
        }

        static FakeKmaForecastClient returning(List<KmaForecastItem> items) {
            return new FakeKmaForecastClient(items, null);
        }

        static FakeKmaForecastClient failing(KmaForecastClientException failure) {
            return new FakeKmaForecastClient(List.of(), failure);
        }

        @Override
        public List<KmaForecastItem> getVilageForecast(KmaForecastBaseTime baseTime, KmaGrid grid) {
            called = true;
            requestedBaseTime = baseTime;
            requestedGrid = grid;
            if (failure != null) {
                throw failure;
            }
            return items;
        }

        boolean called() {
            return called;
        }

        KmaForecastBaseTime requestedBaseTime() {
            return requestedBaseTime;
        }

        KmaGrid requestedGrid() {
            return requestedGrid;
        }
    }

    private static final class FakeUserLocationReader implements UserLocationReader {

        @Override
        public UserLocationSnapshot getRequiredLocationSnapshot(Long userId) {
            return new UserLocationSnapshot(userId, "BUSAN", "부산광역시", 98, 76, LocalDateTime.now());
        }
    }
}

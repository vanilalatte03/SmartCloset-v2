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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
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
        assertThat(client.callCount()).isEqualTo(1);
        assertThat(weather.temperature()).isEqualTo(13);
        assertThat(weather.weatherType()).isEqualTo(WeatherType.SUNNY);
        assertThat(weather.rainy()).isFalse();
        assertThat(weather.windy()).isFalse();
    }

    @Test
    void reusesCachedKmaWeatherWithinTtlForSameKey() {
        FakeKmaForecastClient client = FakeKmaForecastClient.returning(completeGroup());
        KmaVilageForecastWeatherProvider provider = newProvider(properties("test-service-key", true), client);

        WeatherCondition firstWeather = provider.getCurrentWeather(1L);
        WeatherCondition secondWeather = provider.getCurrentWeather(1L);

        assertThat(secondWeather).isEqualTo(firstWeather);
        assertThat(client.callCount()).isEqualTo(1);
    }

    @Test
    void reusesCachedFallbackWeatherWithinTtlForSameKey() {
        FakeKmaForecastClient client = FakeKmaForecastClient.returning(completeGroup());
        CountingStaticWeatherProvider fallbackProvider = new CountingStaticWeatherProvider();
        KmaVilageForecastWeatherProvider provider = newProvider(
                properties("", true),
                client,
                fallbackProvider
        );

        WeatherCondition firstWeather = provider.getCurrentWeather(1L);
        WeatherCondition secondWeather = provider.getCurrentWeather(1L);

        assertThat(secondWeather).isEqualTo(firstWeather);
        assertFallbackWeather(secondWeather);
        assertThat(client.called()).isFalse();
        assertThat(fallbackProvider.callCount()).isEqualTo(1);
    }

    @Test
    void refreshesCachedWeatherAfterTtlExpires() {
        MutableClock mutableClock = MutableClock.fixed("2026-05-21T14:15:00+09:00");
        FakeKmaForecastClient client = FakeKmaForecastClient.returning(completeGroup());
        KmaVilageForecastWeatherProvider provider = newProvider(
                properties("test-service-key", true),
                client,
                mutableClock
        );

        provider.getCurrentWeather(1L);
        mutableClock.advance(Duration.ofMinutes(2));
        provider.getCurrentWeather(1L);

        assertThat(client.callCount()).isEqualTo(2);
    }

    @Test
    void doesNotReuseCachedWeatherWhenLocationChanges() {
        FakeKmaForecastClient client = FakeKmaForecastClient.returning(completeGroup());
        MutableUserLocationReader locationReader = new MutableUserLocationReader(busanLocation());
        KmaVilageForecastWeatherProvider provider = newProvider(
                properties("test-service-key", true),
                client,
                new StaticWeatherProvider(),
                locationReader,
                clock
        );

        provider.getCurrentWeather(1L);
        locationReader.setLocation(new UserLocationSnapshot(
                1L,
                "SEOUL",
                "서울특별시",
                60,
                127,
                LocalDateTime.now()
        ));
        provider.getCurrentWeather(1L);

        assertThat(client.callCount()).isEqualTo(2);
        assertThat(client.requestedGrids()).containsExactly(new KmaGrid(98, 76), new KmaGrid(60, 127));
    }

    @Test
    void doesNotReuseCachedWeatherWhenBaseTimeChangesWithinTtl() {
        FakeKmaForecastClient client = FakeKmaForecastClient.returning(completeGroup());
        MutableBaseTimeCalculator baseTimeCalculator = new MutableBaseTimeCalculator(
                new KmaForecastBaseTime("20260521", "1400")
        );
        KmaVilageForecastWeatherProvider provider = new KmaVilageForecastWeatherProvider(
                properties("test-service-key", true),
                client,
                baseTimeCalculator,
                new KmaWeatherConditionMapper(),
                new StaticWeatherProvider(),
                new FakeUserLocationReader(),
                clock
        );

        provider.getCurrentWeather(1L);
        baseTimeCalculator.setBaseTime(new KmaForecastBaseTime("20260521", "1700"));
        provider.getCurrentWeather(1L);

        assertThat(client.callCount()).isEqualTo(2);
        assertThat(client.requestedBaseTimes()).containsExactly(
                new KmaForecastBaseTime("20260521", "1400"),
                new KmaForecastBaseTime("20260521", "1700")
        );
    }

    @Test
    void cachedFallbackDoesNotHideStrictModeFailureWhenFallbackSettingChanges() {
        KmaWeatherProperties properties = properties("", true);
        FakeKmaForecastClient client = FakeKmaForecastClient.returning(completeGroup());
        KmaVilageForecastWeatherProvider provider = newProvider(properties, client);

        WeatherCondition fallbackWeather = provider.getCurrentWeather(1L);
        properties.setFallbackEnabled(false);

        assertFallbackWeather(fallbackWeather);
        assertInternalServerError(provider);
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
        return newProvider(properties, client, clock);
    }

    private KmaVilageForecastWeatherProvider newProvider(
            KmaWeatherProperties properties,
            KmaForecastClient client,
            Clock clock
    ) {
        return newProvider(properties, client, new StaticWeatherProvider(), new FakeUserLocationReader(), clock);
    }

    private KmaVilageForecastWeatherProvider newProvider(
            KmaWeatherProperties properties,
            KmaForecastClient client,
            StaticWeatherProvider fallbackProvider
    ) {
        return newProvider(properties, client, fallbackProvider, new FakeUserLocationReader(), clock);
    }

    private KmaVilageForecastWeatherProvider newProvider(
            KmaWeatherProperties properties,
            KmaForecastClient client,
            StaticWeatherProvider fallbackProvider,
            UserLocationReader userLocationReader,
            Clock clock
    ) {
        return new KmaVilageForecastWeatherProvider(
                properties,
                client,
                new KmaForecastBaseTimeCalculator(),
                new KmaWeatherConditionMapper(),
                fallbackProvider,
                userLocationReader,
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

    private UserLocationSnapshot busanLocation() {
        return new UserLocationSnapshot(1L, "BUSAN", "부산광역시", 98, 76, LocalDateTime.now());
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
        private int callCount;
        private KmaForecastBaseTime requestedBaseTime;
        private KmaGrid requestedGrid;
        private final List<KmaForecastBaseTime> requestedBaseTimes = new ArrayList<>();
        private final List<KmaGrid> requestedGrids = new ArrayList<>();

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
            callCount++;
            requestedBaseTime = baseTime;
            requestedGrid = grid;
            requestedBaseTimes.add(baseTime);
            requestedGrids.add(grid);
            if (failure != null) {
                throw failure;
            }
            return items;
        }

        boolean called() {
            return callCount > 0;
        }

        int callCount() {
            return callCount;
        }

        KmaForecastBaseTime requestedBaseTime() {
            return requestedBaseTime;
        }

        KmaGrid requestedGrid() {
            return requestedGrid;
        }

        List<KmaForecastBaseTime> requestedBaseTimes() {
            return List.copyOf(requestedBaseTimes);
        }

        List<KmaGrid> requestedGrids() {
            return List.copyOf(requestedGrids);
        }
    }

    private static final class FakeUserLocationReader implements UserLocationReader {

        @Override
        public UserLocationSnapshot getRequiredLocationSnapshot(Long userId) {
            return new UserLocationSnapshot(userId, "BUSAN", "부산광역시", 98, 76, LocalDateTime.now());
        }
    }

    private static final class MutableUserLocationReader implements UserLocationReader {

        private UserLocationSnapshot location;

        private MutableUserLocationReader(UserLocationSnapshot location) {
            this.location = location;
        }

        private void setLocation(UserLocationSnapshot location) {
            this.location = location;
        }

        @Override
        public UserLocationSnapshot getRequiredLocationSnapshot(Long userId) {
            return location;
        }
    }

    private static final class MutableBaseTimeCalculator extends KmaForecastBaseTimeCalculator {

        private KmaForecastBaseTime baseTime;

        private MutableBaseTimeCalculator(KmaForecastBaseTime baseTime) {
            this.baseTime = baseTime;
        }

        private void setBaseTime(KmaForecastBaseTime baseTime) {
            this.baseTime = baseTime;
        }

        @Override
        public KmaForecastBaseTime calculate(Clock clock) {
            return baseTime;
        }
    }

    private static final class CountingStaticWeatherProvider extends StaticWeatherProvider {

        private int callCount;

        @Override
        public WeatherCondition getCurrentWeather(Long userId) {
            callCount++;
            return super.getCurrentWeather(userId);
        }

        private int callCount() {
            return callCount;
        }
    }

    private static final class MutableClock extends Clock {

        private final ZoneId zone;
        private Instant instant;

        private MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        private static MutableClock fixed(String value) {
            ZonedDateTime dateTime = ZonedDateTime.parse(value);
            return new MutableClock(dateTime.toInstant(), dateTime.getZone());
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}

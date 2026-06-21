package com.smartcloset.weather.infrastructure.kma;

import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import com.smartcloset.common.observability.SmartClosetMetrics;
import com.smartcloset.user.application.UserLocationReader;
import com.smartcloset.user.application.UserLocationSnapshot;
import com.smartcloset.weather.application.WeatherProvider;
import com.smartcloset.weather.domain.ForecastPeriod;
import com.smartcloset.weather.domain.WeatherCondition;
import com.smartcloset.weather.domain.WeatherLocationSnapshot;
import com.smartcloset.weather.domain.WeatherSnapshot;
import com.smartcloset.weather.domain.WeatherSource;
import com.smartcloset.weather.infrastructure.StaticWeatherProvider;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * KMA 단기예보를 SmartCloset의 {@link WeatherProvider} 계약으로 변환하는 primary provider다.
 *
 * <p>서비스 키가 없거나 KMA 호출/매핑이 실패하면 설정에 따라 {@link StaticWeatherProvider}로
 * fallback하며, 추천 이력에는 KMA 사용 여부와 fallback 여부만 snapshot으로 남긴다.</p>
 */
@Component
@Primary
public class KmaVilageForecastWeatherProvider implements WeatherProvider {

    private static final DateTimeFormatter FORECAST_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter FORECAST_TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmm");

    private final KmaWeatherProperties properties;
    private final KmaForecastClient client;
    private final KmaForecastBaseTimeCalculator baseTimeCalculator;
    private final KmaWeatherConditionMapper mapper;
    private final StaticWeatherProvider fallbackProvider;
    private final UserLocationReader userLocationReader;
    private final SmartClosetMetrics metrics;
    private final Clock clock;
    private final Map<WeatherCacheKey, WeatherCacheEntry> weatherCache = new ConcurrentHashMap<>();

    @Autowired
    public KmaVilageForecastWeatherProvider(
            KmaWeatherProperties properties,
            KmaForecastClient client,
            StaticWeatherProvider fallbackProvider,
            UserLocationReader userLocationReader,
            SmartClosetMetrics metrics
    ) {
        this(
                properties,
                client,
                new KmaForecastBaseTimeCalculator(),
                new KmaWeatherConditionMapper(),
                fallbackProvider,
                userLocationReader,
                metrics,
                Clock.system(KmaForecastBaseTimeCalculator.KST_ZONE)
        );
    }

    KmaVilageForecastWeatherProvider(
            KmaWeatherProperties properties,
            KmaForecastClient client,
            KmaForecastBaseTimeCalculator baseTimeCalculator,
            KmaWeatherConditionMapper mapper,
            StaticWeatherProvider fallbackProvider,
            UserLocationReader userLocationReader,
            Clock clock
    ) {
        this(
                properties,
                client,
                baseTimeCalculator,
                mapper,
                fallbackProvider,
                userLocationReader,
                SmartClosetMetrics.noop(),
                clock
        );
    }

    KmaVilageForecastWeatherProvider(
            KmaWeatherProperties properties,
            KmaForecastClient client,
            KmaForecastBaseTimeCalculator baseTimeCalculator,
            KmaWeatherConditionMapper mapper,
            StaticWeatherProvider fallbackProvider,
            UserLocationReader userLocationReader,
            SmartClosetMetrics metrics,
            Clock clock
    ) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.baseTimeCalculator = Objects.requireNonNull(baseTimeCalculator, "baseTimeCalculator must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.fallbackProvider = Objects.requireNonNull(fallbackProvider, "fallbackProvider must not be null");
        this.userLocationReader = Objects.requireNonNull(userLocationReader, "userLocationReader must not be null");
        this.metrics = Objects.requireNonNull(metrics, "metrics must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        if (properties.cacheTtl().isZero() || properties.cacheTtl().isNegative()) {
            throw new IllegalArgumentException("KMA weather cache ttl must be positive");
        }
        if (properties.cacheMaxSize() <= 0) {
            throw new IllegalArgumentException("KMA weather cache max size must be positive");
        }
    }

    @Override
    public WeatherSnapshot getWeather(Long userId, ForecastPeriod forecastPeriod) {
        Timer.Sample sample = metrics.startTimer();
        UserLocationSnapshot location = userLocationReader.getRequiredLocationSnapshot(userId);
        KmaForecastBaseTime baseTime = baseTimeCalculator.calculate(clock);
        ForecastPeriod resolvedForecastPeriod = forecastPeriod == null ? ForecastPeriod.CURRENT : forecastPeriod;
        WeatherCacheKey cacheKey = WeatherCacheKey.from(
                location,
                baseTime,
                resolvedForecastPeriod,
                !properties.serviceKey().isBlank(),
                properties.fallbackEnabled()
        );
        Instant now = clock.instant();
        removeExpiredEntries(now);
        WeatherCacheEntry cachedWeather = weatherCache.get(cacheKey);
        if (cachedWeather != null) {
            if (now.isBefore(cachedWeather.expiresAt())) {
                metrics.recordWeatherProvider(sample, resolvedForecastPeriod, "cache_hit");
                return weatherSnapshot(cachedWeather.weather(), location);
            }
            weatherCache.remove(cacheKey, cachedWeather);
        }

        try {
            CachedWeather weather = fetchWeather(userId, location, baseTime, resolvedForecastPeriod);
            weatherCache.put(cacheKey, new WeatherCacheEntry(weather, now.plus(properties.cacheTtl()), now));
            trimCache(cacheKey);
            metrics.recordWeatherProvider(
                    sample,
                    resolvedForecastPeriod,
                    weather.source().fallbackUsed() ? "fallback" : "success"
            );
            return weatherSnapshot(weather, location);
        } catch (RuntimeException exception) {
            metrics.recordWeatherProvider(sample, resolvedForecastPeriod, "failure");
            throw exception;
        }
    }

    private CachedWeather fetchWeather(
            Long userId,
            UserLocationSnapshot location,
            KmaForecastBaseTime baseTime,
            ForecastPeriod forecastPeriod
    ) {
        KmaGrid grid = new KmaGrid(location.nx(), location.ny());

        if (properties.serviceKey().isBlank()) {
            return fallbackOrThrow(userId, location, baseTime, forecastPeriod);
        }

        try {
            List<KmaForecastItem> items = client.getVilageForecast(baseTime, grid);
            KmaMappedWeather mappedWeather = mapper.map(items, ZonedDateTime.now(clock), forecastPeriod);
            return new CachedWeather(
                    mappedWeather.condition(),
                    WeatherSource.kma(
                            baseTime.baseDate(),
                            baseTime.baseTime(),
                            mappedWeather.forecastDate(),
                            mappedWeather.forecastTime()
                    )
            );
        } catch (KmaForecastClientException | KmaWeatherMappingException exception) {
            return fallbackOrThrow(userId, location, baseTime, forecastPeriod);
        }
    }

    /**
     * fallback도 실제 사용자 위치 snapshot을 유지한다. 바뀌는 것은 날씨 값과 source metadata뿐이다.
     */
    private CachedWeather fallbackOrThrow(
            Long userId,
            UserLocationSnapshot location,
            KmaForecastBaseTime baseTime,
            ForecastPeriod forecastPeriod
    ) {
        if (properties.fallbackEnabled()) {
            LocalDateTime forecastDateTime = fallbackForecastDateTime(forecastPeriod);
            return new CachedWeather(
                    fallbackProvider.getCurrentWeather(userId).condition(),
                    WeatherSource.fallback(
                            baseTime.baseDate(),
                            baseTime.baseTime(),
                            forecastDateTime.format(FORECAST_DATE_FORMATTER),
                            forecastDateTime.format(FORECAST_TIME_FORMATTER)
                    )
            );
        }
        throw new SmartClosetException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private WeatherSnapshot weatherSnapshot(CachedWeather weather, UserLocationSnapshot location) {
        return new WeatherSnapshot(
                weather.condition(),
                WeatherLocationSnapshot.from(location),
                weather.source()
        );
    }

    private void removeExpiredEntries(Instant now) {
        weatherCache.forEach((key, entry) -> {
            if (!now.isBefore(entry.expiresAt())) {
                weatherCache.remove(key, entry);
            }
        });
    }

    private void trimCache(WeatherCacheKey protectedKey) {
        int maxSize = properties.cacheMaxSize();
        int excess = weatherCache.size() - maxSize;
        if (excess <= 0) {
            return;
        }

        weatherCache.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(protectedKey))
                .sorted(Comparator
                        .comparing((Map.Entry<WeatherCacheKey, WeatherCacheEntry> entry) -> entry.getValue().expiresAt())
                        .thenComparing(entry -> entry.getValue().cachedAt()))
                .limit(excess)
                .map(Map.Entry::getKey)
                .toList()
                .forEach(weatherCache::remove);
    }

    int cacheEntryCount() {
        return weatherCache.size();
    }

    /**
     * KMA 응답이 없을 때도 forecastPeriod별 표시 시간이 일관되도록 source forecast time을 만든다.
     */
    private LocalDateTime fallbackForecastDateTime(ForecastPeriod forecastPeriod) {
        ZonedDateTime nowKst = ZonedDateTime.now(clock)
                .withZoneSameInstant(KmaForecastBaseTimeCalculator.KST_ZONE);
        LocalDate today = nowKst.toLocalDate();
        return switch (forecastPeriod) {
            case CURRENT -> roundUpToForecastHour(nowKst.toLocalDateTime());
            case MORNING -> LocalDateTime.of(today, LocalTime.of(9, 0));
            case AFTERNOON -> LocalDateTime.of(today, LocalTime.of(15, 0));
            case EVENING -> LocalDateTime.of(today, LocalTime.of(21, 0));
        };
    }

    private LocalDateTime roundUpToForecastHour(LocalDateTime dateTime) {
        LocalDateTime truncatedToHour = dateTime.withMinute(0).withSecond(0).withNano(0);
        if (dateTime.equals(truncatedToHour)) {
            return truncatedToHour;
        }
        return truncatedToHour.plusHours(1);
    }

    /**
     * 캐시는 KMA grid, 발표 시각, 예보 시간대, provider 설정이 모두 같을 때만 재사용한다.
     *
     * <p>사용자별 위치 snapshot은 캐시 값이 아니라 반환 시점에 합성해 같은 grid의 날씨 값을 공유한다.</p>
     */
    private record WeatherCacheKey(
            int nx,
            int ny,
            String baseDate,
            String baseTime,
            ForecastPeriod forecastPeriod,
            boolean serviceKeyConfigured,
            boolean fallbackEnabled
    ) {

        private WeatherCacheKey {
            Objects.requireNonNull(baseDate, "baseDate must not be null");
            Objects.requireNonNull(baseTime, "baseTime must not be null");
            Objects.requireNonNull(forecastPeriod, "forecastPeriod must not be null");
        }

        private static WeatherCacheKey from(
                UserLocationSnapshot location,
                KmaForecastBaseTime baseTime,
                ForecastPeriod forecastPeriod,
                boolean serviceKeyConfigured,
                boolean fallbackEnabled
        ) {
            return new WeatherCacheKey(
                    location.nx(),
                    location.ny(),
                    baseTime.baseDate(),
                    baseTime.baseTime(),
                    forecastPeriod,
                    serviceKeyConfigured,
                    fallbackEnabled
            );
        }
    }

    private record CachedWeather(WeatherCondition condition, WeatherSource source) {

        private CachedWeather {
            Objects.requireNonNull(condition, "condition must not be null");
            Objects.requireNonNull(source, "source must not be null");
        }
    }

    private record WeatherCacheEntry(CachedWeather weather, Instant expiresAt, Instant cachedAt) {

        private WeatherCacheEntry {
            Objects.requireNonNull(weather, "weather must not be null");
            Objects.requireNonNull(expiresAt, "expiresAt must not be null");
            Objects.requireNonNull(cachedAt, "cachedAt must not be null");
        }
    }
}

package com.smartcloset.weather.infrastructure.kma;

import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import com.smartcloset.user.application.UserLocationReader;
import com.smartcloset.user.application.UserLocationSnapshot;
import com.smartcloset.weather.application.WeatherProvider;
import com.smartcloset.weather.domain.WeatherCondition;
import com.smartcloset.weather.infrastructure.StaticWeatherProvider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class KmaVilageForecastWeatherProvider implements WeatherProvider {

    private static final Duration WEATHER_CACHE_TTL = Duration.ofMinutes(2);

    private final KmaWeatherProperties properties;
    private final KmaForecastClient client;
    private final KmaForecastBaseTimeCalculator baseTimeCalculator;
    private final KmaWeatherConditionMapper mapper;
    private final StaticWeatherProvider fallbackProvider;
    private final UserLocationReader userLocationReader;
    private final Clock clock;
    private final Map<WeatherCacheKey, WeatherCacheEntry> weatherCache = new ConcurrentHashMap<>();

    @Autowired
    public KmaVilageForecastWeatherProvider(
            KmaWeatherProperties properties,
            KmaForecastClient client,
            StaticWeatherProvider fallbackProvider,
            UserLocationReader userLocationReader
    ) {
        this(
                properties,
                client,
                new KmaForecastBaseTimeCalculator(),
                new KmaWeatherConditionMapper(),
                fallbackProvider,
                userLocationReader,
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
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.baseTimeCalculator = Objects.requireNonNull(baseTimeCalculator, "baseTimeCalculator must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.fallbackProvider = Objects.requireNonNull(fallbackProvider, "fallbackProvider must not be null");
        this.userLocationReader = Objects.requireNonNull(userLocationReader, "userLocationReader must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public WeatherCondition getCurrentWeather(Long userId) {
        UserLocationSnapshot location = userLocationReader.getRequiredLocationSnapshot(userId);
        KmaForecastBaseTime baseTime = baseTimeCalculator.calculate(clock);
        WeatherCacheKey cacheKey = WeatherCacheKey.from(
                userId,
                location,
                baseTime,
                !properties.serviceKey().isBlank(),
                properties.fallbackEnabled()
        );
        Instant now = clock.instant();
        WeatherCacheEntry cachedWeather = weatherCache.get(cacheKey);
        if (cachedWeather != null) {
            if (now.isBefore(cachedWeather.expiresAt())) {
                return cachedWeather.weather();
            }
            weatherCache.remove(cacheKey, cachedWeather);
        }

        WeatherCondition weather = fetchCurrentWeather(userId, location, baseTime);
        weatherCache.put(cacheKey, new WeatherCacheEntry(weather, now.plus(WEATHER_CACHE_TTL)));
        return weather;
    }

    private WeatherCondition fetchCurrentWeather(
            Long userId,
            UserLocationSnapshot location,
            KmaForecastBaseTime baseTime
    ) {
        KmaGrid grid = new KmaGrid(location.nx(), location.ny());

        if (properties.serviceKey().isBlank()) {
            return fallbackOrThrow(userId);
        }

        try {
            List<KmaForecastItem> items = client.getVilageForecast(baseTime, grid);
            return mapper.map(items, ZonedDateTime.now(clock));
        } catch (KmaForecastClientException | KmaWeatherMappingException exception) {
            return fallbackOrThrow(userId);
        }
    }

    private WeatherCondition fallbackOrThrow(Long userId) {
        if (properties.fallbackEnabled()) {
            return fallbackProvider.getCurrentWeather(userId);
        }
        throw new SmartClosetException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private record WeatherCacheKey(
            Long userId,
            String locationCode,
            int nx,
            int ny,
            String baseDate,
            String baseTime,
            boolean serviceKeyConfigured,
            boolean fallbackEnabled
    ) {

        private WeatherCacheKey {
            Objects.requireNonNull(userId, "userId must not be null");
            Objects.requireNonNull(locationCode, "locationCode must not be null");
            Objects.requireNonNull(baseDate, "baseDate must not be null");
            Objects.requireNonNull(baseTime, "baseTime must not be null");
        }

        private static WeatherCacheKey from(
                Long userId,
                UserLocationSnapshot location,
                KmaForecastBaseTime baseTime,
                boolean serviceKeyConfigured,
                boolean fallbackEnabled
        ) {
            return new WeatherCacheKey(
                    userId,
                    location.code(),
                    location.nx(),
                    location.ny(),
                    baseTime.baseDate(),
                    baseTime.baseTime(),
                    serviceKeyConfigured,
                    fallbackEnabled
            );
        }
    }

    private record WeatherCacheEntry(WeatherCondition weather, Instant expiresAt) {

        private WeatherCacheEntry {
            Objects.requireNonNull(weather, "weather must not be null");
            Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        }
    }
}

package com.smartcloset.weather.infrastructure.kma;

import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import com.smartcloset.user.application.UserLocationReader;
import com.smartcloset.user.application.UserLocationSnapshot;
import com.smartcloset.weather.application.WeatherProvider;
import com.smartcloset.weather.domain.ForecastPeriod;
import com.smartcloset.weather.domain.WeatherLocationSnapshot;
import com.smartcloset.weather.domain.WeatherSnapshot;
import com.smartcloset.weather.domain.WeatherSource;
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
    public WeatherSnapshot getWeather(Long userId, ForecastPeriod forecastPeriod) {
        UserLocationSnapshot location = userLocationReader.getRequiredLocationSnapshot(userId);
        KmaForecastBaseTime baseTime = baseTimeCalculator.calculate(clock);
        ForecastPeriod resolvedForecastPeriod = forecastPeriod == null ? ForecastPeriod.CURRENT : forecastPeriod;
        WeatherCacheKey cacheKey = WeatherCacheKey.from(
                userId,
                location,
                baseTime,
                resolvedForecastPeriod,
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

        WeatherSnapshot weather = fetchWeather(userId, location, baseTime, resolvedForecastPeriod);
        weatherCache.put(cacheKey, new WeatherCacheEntry(weather, now.plus(WEATHER_CACHE_TTL)));
        return weather;
    }

    private WeatherSnapshot fetchWeather(
            Long userId,
            UserLocationSnapshot location,
            KmaForecastBaseTime baseTime,
            ForecastPeriod forecastPeriod
    ) {
        KmaGrid grid = new KmaGrid(location.nx(), location.ny());

        if (properties.serviceKey().isBlank()) {
            return fallbackOrThrow(userId, location, baseTime);
        }

        try {
            List<KmaForecastItem> items = client.getVilageForecast(baseTime, grid);
            KmaMappedWeather mappedWeather = mapper.map(items, ZonedDateTime.now(clock), forecastPeriod);
            return new WeatherSnapshot(
                    mappedWeather.condition(),
                    WeatherLocationSnapshot.from(location),
                    WeatherSource.kma(
                            baseTime.baseDate(),
                            baseTime.baseTime(),
                            mappedWeather.forecastDate(),
                            mappedWeather.forecastTime()
                    )
            );
        } catch (KmaForecastClientException | KmaWeatherMappingException exception) {
            return fallbackOrThrow(userId, location, baseTime);
        }
    }

    private WeatherSnapshot fallbackOrThrow(Long userId, UserLocationSnapshot location, KmaForecastBaseTime baseTime) {
        if (properties.fallbackEnabled()) {
            return new WeatherSnapshot(
                    fallbackProvider.getCurrentWeather(userId).condition(),
                    WeatherLocationSnapshot.from(location),
                    WeatherSource.fallback(baseTime.baseDate(), baseTime.baseTime())
            );
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
            ForecastPeriod forecastPeriod,
            boolean serviceKeyConfigured,
            boolean fallbackEnabled
    ) {

        private WeatherCacheKey {
            Objects.requireNonNull(userId, "userId must not be null");
            Objects.requireNonNull(locationCode, "locationCode must not be null");
            Objects.requireNonNull(baseDate, "baseDate must not be null");
            Objects.requireNonNull(baseTime, "baseTime must not be null");
            Objects.requireNonNull(forecastPeriod, "forecastPeriod must not be null");
        }

        private static WeatherCacheKey from(
                Long userId,
                UserLocationSnapshot location,
                KmaForecastBaseTime baseTime,
                ForecastPeriod forecastPeriod,
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
                    forecastPeriod,
                    serviceKeyConfigured,
                    fallbackEnabled
            );
        }
    }

    private record WeatherCacheEntry(WeatherSnapshot weather, Instant expiresAt) {

        private WeatherCacheEntry {
            Objects.requireNonNull(weather, "weather must not be null");
            Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        }
    }
}

package com.smartcloset.weather.infrastructure.kma;

import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import com.smartcloset.weather.application.WeatherProvider;
import com.smartcloset.weather.domain.WeatherCondition;
import com.smartcloset.weather.infrastructure.StaticWeatherProvider;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class KmaVilageForecastWeatherProvider implements WeatherProvider {

    private final KmaWeatherProperties properties;
    private final KmaForecastClient client;
    private final KmaForecastBaseTimeCalculator baseTimeCalculator;
    private final KmaWeatherConditionMapper mapper;
    private final StaticWeatherProvider fallbackProvider;
    private final Clock clock;

    @Autowired
    public KmaVilageForecastWeatherProvider(
            KmaWeatherProperties properties,
            KmaForecastClient client,
            StaticWeatherProvider fallbackProvider
    ) {
        this(
                properties,
                client,
                new KmaForecastBaseTimeCalculator(),
                new KmaWeatherConditionMapper(),
                fallbackProvider,
                Clock.system(KmaForecastBaseTimeCalculator.KST_ZONE)
        );
    }

    KmaVilageForecastWeatherProvider(
            KmaWeatherProperties properties,
            KmaForecastClient client,
            KmaForecastBaseTimeCalculator baseTimeCalculator,
            KmaWeatherConditionMapper mapper,
            StaticWeatherProvider fallbackProvider,
            Clock clock
    ) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.baseTimeCalculator = Objects.requireNonNull(baseTimeCalculator, "baseTimeCalculator must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.fallbackProvider = Objects.requireNonNull(fallbackProvider, "fallbackProvider must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public WeatherCondition getCurrentWeather(Long userId) {
        if (properties.serviceKey().isBlank()) {
            return fallbackOrThrow(userId);
        }

        try {
            KmaForecastBaseTime baseTime = baseTimeCalculator.calculate(clock);
            List<KmaForecastItem> items = client.getVilageForecast(baseTime);
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
}

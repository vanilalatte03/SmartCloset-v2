package com.smartcloset.common.observability;

import com.smartcloset.recommendation.domain.RecommendationSituation;
import com.smartcloset.weather.domain.ForecastPeriod;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * SmartCloset 운영 metric 이름과 low-cardinality tag를 한 곳에서 관리한다.
 */
@Component
public class SmartClosetMetrics {

    private static final String UNKNOWN = "unknown";

    private final MeterRegistry meterRegistry;

    public SmartClosetMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
    }

    public static SmartClosetMetrics noop() {
        return new SmartClosetMetrics(new SimpleMeterRegistry());
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordRecommendation(
            Timer.Sample sample,
            RecommendationSituation situation,
            ForecastPeriod forecastPeriod,
            String outcome
    ) {
        Tags tags = Tags.of(
                "situation", enumTag(situation),
                "forecast_period", enumTag(forecastPeriod),
                "outcome", normalize(outcome)
        );
        counter("smartcloset.recommendation.requests", tags).increment();
        stop(sample, "smartcloset.recommendation.duration", tags);
    }

    public void recordWeatherProvider(Timer.Sample sample, ForecastPeriod forecastPeriod, String outcome) {
        Tags tags = Tags.of(
                "provider", "kma_vilage_forecast",
                "forecast_period", enumTag(forecastPeriod),
                "outcome", normalize(outcome)
        );
        counter("smartcloset.weather.provider.requests", tags).increment();
        stop(sample, "smartcloset.weather.provider.duration", tags);
    }

    public void recordClothingAnalysis(Timer.Sample sample, String outcome) {
        Tags tags = Tags.of(
                "provider", "openai",
                "outcome", normalize(outcome)
        );
        counter("smartcloset.clothing.analysis.requests", tags).increment();
        stop(sample, "smartcloset.clothing.analysis.duration", tags);
    }

    private Counter counter(String name, Tags tags) {
        return Counter.builder(name)
                .tags(tags)
                .register(meterRegistry);
    }

    private void stop(Timer.Sample sample, String name, Tags tags) {
        if (sample == null) {
            return;
        }
        sample.stop(Timer.builder(name)
                .tags(tags)
                .register(meterRegistry));
    }

    private String enumTag(Enum<?> value) {
        return value == null ? UNKNOWN : value.name().toLowerCase(Locale.ROOT);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}

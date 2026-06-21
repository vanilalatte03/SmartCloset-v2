package com.smartcloset.common.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.smartcloset.recommendation.domain.RecommendationSituation;
import com.smartcloset.weather.domain.ForecastPeriod;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class SmartClosetMetricsTest {

    @Test
    void recordsRecommendationWeatherAndAnalysisMetricsWithStableTags() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        SmartClosetMetrics metrics = new SmartClosetMetrics(meterRegistry);

        metrics.recordRecommendation(
                metrics.startTimer(),
                RecommendationSituation.CASUAL,
                ForecastPeriod.MORNING,
                "success"
        );
        metrics.recordWeatherProvider(metrics.startTimer(), ForecastPeriod.CURRENT, "fallback");
        metrics.recordClothingAnalysis(metrics.startTimer(), "unavailable");

        assertThat(meterRegistry.get("smartcloset.recommendation.requests")
                .tag("situation", "casual")
                .tag("forecast_period", "morning")
                .tag("outcome", "success")
                .counter()
                .count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("smartcloset.recommendation.duration")
                .tag("situation", "casual")
                .tag("forecast_period", "morning")
                .tag("outcome", "success")
                .timer()
                .count()).isEqualTo(1);

        assertThat(meterRegistry.get("smartcloset.weather.provider.requests")
                .tag("provider", "kma_vilage_forecast")
                .tag("forecast_period", "current")
                .tag("outcome", "fallback")
                .counter()
                .count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("smartcloset.weather.provider.duration")
                .tag("provider", "kma_vilage_forecast")
                .tag("forecast_period", "current")
                .tag("outcome", "fallback")
                .timer()
                .count()).isEqualTo(1);

        assertThat(meterRegistry.get("smartcloset.clothing.analysis.requests")
                .tag("provider", "openai")
                .tag("outcome", "unavailable")
                .counter()
                .count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("smartcloset.clothing.analysis.duration")
                .tag("provider", "openai")
                .tag("outcome", "unavailable")
                .timer()
                .count()).isEqualTo(1);
    }
}

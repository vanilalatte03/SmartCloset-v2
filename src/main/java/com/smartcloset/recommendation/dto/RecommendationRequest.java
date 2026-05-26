package com.smartcloset.recommendation.dto;

import com.smartcloset.recommendation.domain.RecommendationSituation;
import com.smartcloset.weather.domain.ForecastPeriod;

public record RecommendationRequest(
        RecommendationSituation situation,
        ForecastPeriod forecastPeriod
) {

    public RecommendationSituation situationOrDefault() {
        return situation == null ? RecommendationSituation.CASUAL : situation;
    }

    public ForecastPeriod forecastPeriodOrDefault() {
        return forecastPeriod == null ? ForecastPeriod.CURRENT : forecastPeriod;
    }
}

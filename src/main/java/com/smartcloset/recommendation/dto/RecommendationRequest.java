package com.smartcloset.recommendation.dto;

import com.smartcloset.recommendation.domain.RecommendationSituation;

public record RecommendationRequest(
        RecommendationSituation situation
) {

    public RecommendationSituation situationOrDefault() {
        return situation == null ? RecommendationSituation.CASUAL : situation;
    }
}

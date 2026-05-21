package com.smartcloset.recommendation.dto;

import java.time.LocalDateTime;

public record RecommendationWornResponse(
        Long recommendationId,
        boolean worn,
        LocalDateTime wornAt
) {

    public static RecommendationWornResponse of(Long recommendationId, LocalDateTime wornAt) {
        return new RecommendationWornResponse(recommendationId, true, wornAt);
    }
}

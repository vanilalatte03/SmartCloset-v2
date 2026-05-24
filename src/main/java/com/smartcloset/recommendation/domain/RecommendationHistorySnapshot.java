package com.smartcloset.recommendation.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

public record RecommendationHistorySnapshot(
        Long recommendationResultId,
        LocalDateTime createdAt,
        Set<Long> clothingItemIds
) {

    public RecommendationHistorySnapshot {
        Objects.requireNonNull(recommendationResultId, "recommendationResultId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        clothingItemIds = Set.copyOf(Objects.requireNonNull(clothingItemIds, "clothingItemIds must not be null"));
    }
}

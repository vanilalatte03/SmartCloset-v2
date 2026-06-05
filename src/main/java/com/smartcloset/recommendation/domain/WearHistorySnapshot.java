package com.smartcloset.recommendation.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

/**
 * 최근 착용 페널티 계산에 필요한 착용 이력 요약이다.
 */
public record WearHistorySnapshot(
        Long recommendationResultId,
        LocalDateTime wornAt,
        Set<Long> clothingItemIds
) {

    public WearHistorySnapshot {
        Objects.requireNonNull(recommendationResultId, "recommendationResultId must not be null");
        Objects.requireNonNull(wornAt, "wornAt must not be null");
        clothingItemIds = Set.copyOf(Objects.requireNonNull(clothingItemIds, "clothingItemIds must not be null"));
    }
}

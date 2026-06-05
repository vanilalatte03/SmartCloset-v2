package com.smartcloset.recommendation.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

/**
 * 추천 반복 페널티와 피드백 보정에 필요한 과거 추천 요약이다.
 *
 * <p>추천 계산 중에는 entity 전체가 필요하지 않으므로, 옷 id set과 feedback metadata만 들고 다닌다.</p>
 */
public record RecommendationHistorySnapshot(
        Long recommendationResultId,
        LocalDateTime createdAt,
        Set<Long> clothingItemIds,
        int weatherTemperature,
        RecommendationFeedbackSentiment sentimentFeedback,
        RecommendationThermalFeedback thermalFeedback,
        LocalDateTime feedbackUpdatedAt
) {

    public RecommendationHistorySnapshot {
        Objects.requireNonNull(recommendationResultId, "recommendationResultId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        clothingItemIds = Set.copyOf(Objects.requireNonNull(clothingItemIds, "clothingItemIds must not be null"));
    }

    public RecommendationHistorySnapshot(
            Long recommendationResultId,
            LocalDateTime createdAt,
            Set<Long> clothingItemIds
    ) {
        this(recommendationResultId, createdAt, clothingItemIds, 0, null, null, null);
    }

    /**
     * 최근 피드백 보정에 사용할 수 있는 feedback 값과 갱신 시각이 모두 있는지 확인한다.
     */
    public boolean hasFeedback() {
        return feedbackUpdatedAt != null && (sentimentFeedback != null || thermalFeedback != null);
    }
}

package com.smartcloset.recommendation.dto;

import com.smartcloset.recommendation.domain.OutfitCandidate;
import com.smartcloset.recommendation.domain.RecommendationResult;
import java.time.LocalDateTime;
import java.util.List;

public record RecommendationResponse(
        Long recommendationId,
        WeatherResponse weather,
        RecommendationOutfitResponse outfit,
        RecommendationScoreResponse score,
        List<String> reasons,
        boolean worn,
        LocalDateTime createdAt
) {

    public static RecommendationResponse from(
            RecommendationResult recommendationResult,
            OutfitCandidate candidate,
            List<String> reasons
    ) {
        return new RecommendationResponse(
                recommendationResult.getId(),
                WeatherResponse.from(recommendationResult),
                RecommendationOutfitResponse.from(candidate),
                RecommendationScoreResponse.from(recommendationResult),
                List.copyOf(reasons),
                recommendationResult.isWorn(),
                recommendationResult.getCreatedAt()
        );
    }
}

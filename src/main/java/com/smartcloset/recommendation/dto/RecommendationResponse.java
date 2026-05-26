package com.smartcloset.recommendation.dto;

import com.smartcloset.clothing.application.ClothingStyleTagMapper;
import com.smartcloset.recommendation.domain.OutfitCandidate;
import com.smartcloset.recommendation.domain.RecommendationResult;
import com.smartcloset.recommendation.domain.RecommendationResultItem;
import com.smartcloset.recommendation.domain.RecommendationSituation;
import java.time.LocalDateTime;
import java.util.List;

public record RecommendationResponse(
        Long recommendationId,
        RecommendationSituation situation,
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
            List<String> reasons,
            ClothingStyleTagMapper styleTagMapper
    ) {
        return new RecommendationResponse(
                recommendationResult.getId(),
                recommendationResult.getSituation(),
                WeatherResponse.from(recommendationResult),
                RecommendationOutfitResponse.from(candidate, styleTagMapper),
                RecommendationScoreResponse.from(recommendationResult),
                List.copyOf(reasons),
                recommendationResult.isWorn(),
                recommendationResult.getCreatedAt()
        );
    }

    public static RecommendationResponse from(
            RecommendationResult recommendationResult,
            List<RecommendationResultItem> items,
            List<String> reasons,
            ClothingStyleTagMapper styleTagMapper
    ) {
        return new RecommendationResponse(
                recommendationResult.getId(),
                recommendationResult.getSituation(),
                WeatherResponse.from(recommendationResult),
                RecommendationOutfitResponse.from(items, styleTagMapper),
                RecommendationScoreResponse.from(recommendationResult),
                List.copyOf(reasons),
                recommendationResult.isWorn(),
                recommendationResult.getCreatedAt()
        );
    }
}

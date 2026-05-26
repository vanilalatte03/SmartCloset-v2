package com.smartcloset.recommendation.dto;

import com.smartcloset.clothing.application.ClothingStyleTagMapper;
import com.smartcloset.recommendation.domain.OutfitCandidate;
import com.smartcloset.recommendation.domain.RecommendationResult;
import com.smartcloset.recommendation.domain.RecommendationResultItem;
import com.smartcloset.recommendation.domain.RecommendationSituation;
import com.smartcloset.weather.domain.ForecastPeriod;
import java.time.LocalDateTime;
import java.util.List;

public record RecommendationResponse(
        Long recommendationId,
        RecommendationSituation situation,
        ForecastPeriod forecastPeriod,
        WeatherResponse weather,
        RecommendationOutfitResponse outfit,
        RecommendationScoreResponse score,
        List<String> reasons,
        boolean worn,
        LocalDateTime wornAt,
        RecommendationFeedbackStateResponse feedback,
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
                recommendationResult.getForecastPeriod(),
                WeatherResponse.from(recommendationResult),
                RecommendationOutfitResponse.from(candidate, styleTagMapper),
                RecommendationScoreResponse.from(recommendationResult),
                List.copyOf(reasons),
                recommendationResult.isWorn(),
                null,
                RecommendationFeedbackStateResponse.from(recommendationResult),
                recommendationResult.getCreatedAt()
        );
    }

    public static RecommendationResponse from(
            RecommendationResult recommendationResult,
            List<RecommendationResultItem> items,
            List<String> reasons,
            LocalDateTime wornAt,
            ClothingStyleTagMapper styleTagMapper
    ) {
        return new RecommendationResponse(
                recommendationResult.getId(),
                recommendationResult.getSituation(),
                recommendationResult.getForecastPeriod(),
                WeatherResponse.from(recommendationResult),
                RecommendationOutfitResponse.from(items, styleTagMapper),
                RecommendationScoreResponse.from(recommendationResult),
                List.copyOf(reasons),
                recommendationResult.isWorn(),
                wornAt,
                RecommendationFeedbackStateResponse.from(recommendationResult),
                recommendationResult.getCreatedAt()
        );
    }
}

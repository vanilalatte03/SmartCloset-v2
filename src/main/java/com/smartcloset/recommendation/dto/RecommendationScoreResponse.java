package com.smartcloset.recommendation.dto;

import com.smartcloset.recommendation.domain.RecommendationResult;
import com.smartcloset.recommendation.domain.RecommendationScore;

public record RecommendationScoreResponse(
        int totalScore,
        int weatherScore,
        int colorScore,
        int wearHistoryScore,
        int recommendationHistoryScore,
        int preferenceScore
) {

    public static RecommendationScoreResponse from(RecommendationScore score) {
        return new RecommendationScoreResponse(
                score.totalScore(),
                score.weatherScore(),
                score.colorScore(),
                score.wearHistoryScore(),
                score.recommendationHistoryScore(),
                score.preferenceScore()
        );
    }

    public static RecommendationScoreResponse from(RecommendationResult recommendationResult) {
        return new RecommendationScoreResponse(
                recommendationResult.getTotalScore(),
                recommendationResult.getWeatherScore(),
                recommendationResult.getColorScore(),
                recommendationResult.getWearHistoryScore(),
                recommendationResult.getRecommendationHistoryScore(),
                recommendationResult.getPreferenceScore()
        );
    }
}

package com.smartcloset.recommendation.dto;

import com.smartcloset.recommendation.domain.RecommendationFeedbackSentiment;
import com.smartcloset.recommendation.domain.RecommendationResult;
import com.smartcloset.recommendation.domain.RecommendationThermalFeedback;
import java.time.LocalDateTime;

public record RecommendationFeedbackStateResponse(
        RecommendationFeedbackSentiment sentiment,
        RecommendationThermalFeedback thermal,
        LocalDateTime updatedAt
) {

    public static RecommendationFeedbackStateResponse from(RecommendationResult recommendationResult) {
        if (!recommendationResult.hasFeedback()) {
            return null;
        }
        return new RecommendationFeedbackStateResponse(
                recommendationResult.getSentimentFeedback(),
                recommendationResult.getThermalFeedback(),
                recommendationResult.getFeedbackUpdatedAt()
        );
    }
}

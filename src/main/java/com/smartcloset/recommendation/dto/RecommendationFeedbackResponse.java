package com.smartcloset.recommendation.dto;

public record RecommendationFeedbackResponse(
        Long recommendationId,
        RecommendationFeedbackStateResponse feedback
) {
}

package com.smartcloset.recommendation.dto;

import com.smartcloset.recommendation.domain.RecommendationFeedbackSentiment;
import com.smartcloset.recommendation.domain.RecommendationThermalFeedback;

public record RecommendationFeedbackRequest(
        RecommendationFeedbackSentiment sentiment,
        RecommendationThermalFeedback thermal
) {
}

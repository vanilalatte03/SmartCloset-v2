package com.smartcloset.recommendation.domain;

public record RecommendationScore(
        int totalScore,
        int weatherScore,
        int colorScore,
        int wearHistoryScore,
        int recommendationHistoryScore,
        int preferenceScore
) {

    public RecommendationScore {
        requireNonNegative(totalScore, "totalScore");
        requireNonNegative(weatherScore, "weatherScore");
        requireNonNegative(colorScore, "colorScore");
        requireNonNegative(wearHistoryScore, "wearHistoryScore");
        requireNonNegative(recommendationHistoryScore, "recommendationHistoryScore");
        requireNonNegative(preferenceScore, "preferenceScore");
    }

    public static RecommendationScore of(
            int totalScore,
            int weatherScore,
            int colorScore,
            int wearHistoryScore,
            int recommendationHistoryScore,
            int preferenceScore
    ) {
        return new RecommendationScore(
                totalScore,
                weatherScore,
                colorScore,
                wearHistoryScore,
                recommendationHistoryScore,
                preferenceScore
        );
    }

    private static void requireNonNegative(int score, String fieldName) {
        if (score < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
    }
}

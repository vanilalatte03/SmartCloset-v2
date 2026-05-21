package com.smartcloset.recommendation.domain;

public record RecommendationScore(
        int totalScore,
        int weatherScore,
        int colorScore,
        int wearHistoryScore,
        int recommendationHistoryScore,
        int diversityScore
) {

    public RecommendationScore {
        requireNonNegative(totalScore, "totalScore");
        requireNonNegative(weatherScore, "weatherScore");
        requireNonNegative(colorScore, "colorScore");
        requireNonNegative(wearHistoryScore, "wearHistoryScore");
        requireNonNegative(recommendationHistoryScore, "recommendationHistoryScore");
        requireNonNegative(diversityScore, "diversityScore");
    }

    public static RecommendationScore of(
            int totalScore,
            int weatherScore,
            int colorScore,
            int wearHistoryScore,
            int recommendationHistoryScore,
            int diversityScore
    ) {
        return new RecommendationScore(
                totalScore,
                weatherScore,
                colorScore,
                wearHistoryScore,
                recommendationHistoryScore,
                diversityScore
        );
    }

    private static void requireNonNegative(int score, String fieldName) {
        if (score < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
    }
}

package com.smartcloset.recommendation.domain;

/**
 * 추천 후보의 총점과 세부 점수 breakdown이다.
 *
 * <p>세부 점수 최대값은 {@link RecommendationScorer}가 관리하고, DTO는 이 값을 그대로 노출해
 * 사용자가 추천 이유를 확인할 수 있게 한다.</p>
 */
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

    /**
     * 총점과 세부 점수를 음수 없이 검증하는 RecommendationScore를 생성한다.
     */
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

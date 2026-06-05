package com.smartcloset.recommendation.domain;

import java.util.Objects;

/**
 * 후보와 계산된 점수를 함께 들고 다니는 계산 모델이다.
 */
public record ScoredOutfitCandidate(OutfitCandidate candidate, RecommendationScore score) {

    public ScoredOutfitCandidate {
        Objects.requireNonNull(candidate, "candidate must not be null");
        Objects.requireNonNull(score, "score must not be null");
    }
}

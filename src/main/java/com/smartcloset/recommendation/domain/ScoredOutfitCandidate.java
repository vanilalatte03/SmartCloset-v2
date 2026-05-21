package com.smartcloset.recommendation.domain;

import java.util.Objects;

public record ScoredOutfitCandidate(OutfitCandidate candidate, RecommendationScore score) {

    public ScoredOutfitCandidate {
        Objects.requireNonNull(candidate, "candidate must not be null");
        Objects.requireNonNull(score, "score must not be null");
    }
}

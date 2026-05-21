package com.smartcloset.recommendation.dto;

import com.smartcloset.recommendation.domain.OutfitCandidate;

public record RecommendationOutfitResponse(
        OutfitItemResponse top,
        OutfitItemResponse bottom,
        OutfitItemResponse outer
) {

    public static RecommendationOutfitResponse from(OutfitCandidate candidate) {
        return new RecommendationOutfitResponse(
                OutfitItemResponse.from(candidate.top()),
                OutfitItemResponse.from(candidate.bottom()),
                candidate.hasOuter() ? OutfitItemResponse.from(candidate.outer()) : null
        );
    }
}

package com.smartcloset.recommendation.dto;

import com.smartcloset.recommendation.domain.OutfitCandidate;
import com.smartcloset.recommendation.domain.OutfitSlot;
import com.smartcloset.recommendation.domain.RecommendationResult;
import com.smartcloset.recommendation.domain.RecommendationResultItem;
import java.util.Map;
import java.util.stream.Collectors;

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

    public static RecommendationOutfitResponse from(RecommendationResult recommendationResult) {
        Map<OutfitSlot, RecommendationResultItem> itemsBySlot = recommendationResult.getItems().stream()
                .collect(Collectors.toMap(RecommendationResultItem::getSlot, item -> item));
        return new RecommendationOutfitResponse(
                OutfitItemResponse.from(itemsBySlot.get(OutfitSlot.TOP).getClothingItem()),
                OutfitItemResponse.from(itemsBySlot.get(OutfitSlot.BOTTOM).getClothingItem()),
                itemsBySlot.containsKey(OutfitSlot.OUTER)
                        ? OutfitItemResponse.from(itemsBySlot.get(OutfitSlot.OUTER).getClothingItem())
                        : null
        );
    }
}

package com.smartcloset.recommendation.dto;

import com.smartcloset.clothing.application.ClothingStyleTagMapper;
import com.smartcloset.recommendation.domain.OutfitCandidate;
import com.smartcloset.recommendation.domain.OutfitSlot;
import com.smartcloset.recommendation.domain.RecommendationResultItem;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record RecommendationOutfitResponse(
        OutfitItemResponse top,
        OutfitItemResponse bottom,
        OutfitItemResponse outer
) {

    public static RecommendationOutfitResponse from(
            OutfitCandidate candidate,
            ClothingStyleTagMapper styleTagMapper
    ) {
        return new RecommendationOutfitResponse(
                OutfitItemResponse.from(candidate.top(), styleTagMapper),
                OutfitItemResponse.from(candidate.bottom(), styleTagMapper),
                candidate.hasOuter() ? OutfitItemResponse.from(candidate.outer(), styleTagMapper) : null
        );
    }

    public static RecommendationOutfitResponse from(
            List<RecommendationResultItem> resultItems,
            ClothingStyleTagMapper styleTagMapper
    ) {
        Map<OutfitSlot, OutfitItemResponse> items = new EnumMap<>(OutfitSlot.class);
        for (RecommendationResultItem item : resultItems) {
            items.put(item.getSlot(), OutfitItemResponse.from(item.getClothingItem(), styleTagMapper));
        }
        return new RecommendationOutfitResponse(
                items.get(OutfitSlot.TOP),
                items.get(OutfitSlot.BOTTOM),
                items.get(OutfitSlot.OUTER)
        );
    }
}

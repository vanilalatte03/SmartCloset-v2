package com.smartcloset.recommendation.dto;

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

    public static RecommendationOutfitResponse from(OutfitCandidate candidate) {
        return new RecommendationOutfitResponse(
                OutfitItemResponse.from(candidate.top()),
                OutfitItemResponse.from(candidate.bottom()),
                candidate.hasOuter() ? OutfitItemResponse.from(candidate.outer()) : null
        );
    }

    public static RecommendationOutfitResponse from(List<RecommendationResultItem> resultItems) {
        Map<OutfitSlot, OutfitItemResponse> items = new EnumMap<>(OutfitSlot.class);
        for (RecommendationResultItem item : resultItems) {
            items.put(item.getSlot(), OutfitItemResponse.from(item.getClothingItem()));
        }
        return new RecommendationOutfitResponse(
                items.get(OutfitSlot.TOP),
                items.get(OutfitSlot.BOTTOM),
                items.get(OutfitSlot.OUTER)
        );
    }
}

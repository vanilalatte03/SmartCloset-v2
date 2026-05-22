package com.smartcloset.clothing.dto;

import com.smartcloset.clothing.domain.ClothingItem;
import java.time.LocalDateTime;

public record ClothingArchiveResponse(
        Long id,
        boolean archived,
        LocalDateTime updatedAt
) {

    public static ClothingArchiveResponse from(ClothingItem clothingItem) {
        return new ClothingArchiveResponse(
                clothingItem.getId(),
                clothingItem.isArchived(),
                clothingItem.getUpdatedAt()
        );
    }
}

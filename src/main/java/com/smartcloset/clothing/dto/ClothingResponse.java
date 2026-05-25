package com.smartcloset.clothing.dto;

import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.domain.ClothingMaterial;
import java.time.LocalDateTime;

public record ClothingResponse(
        Long id,
        String name,
        ClothingCategory category,
        ClothingColor color,
        ClothingMaterial material,
        int minTemperature,
        int maxTemperature,
        boolean rainSuitable,
        boolean archived,
        ClothingImageResponse image,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ClothingResponse from(ClothingItem clothingItem) {
        return new ClothingResponse(
                clothingItem.getId(),
                clothingItem.getName(),
                clothingItem.getCategory(),
                clothingItem.getColor(),
                clothingItem.getMaterial(),
                clothingItem.getMinTemperature(),
                clothingItem.getMaxTemperature(),
                clothingItem.isRainSuitable(),
                clothingItem.isArchived(),
                ClothingImageResponse.from(clothingItem),
                clothingItem.getCreatedAt(),
                clothingItem.getUpdatedAt()
        );
    }
}

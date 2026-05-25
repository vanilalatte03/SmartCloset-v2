package com.smartcloset.recommendation.dto;

import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.domain.ClothingMaterial;
import com.smartcloset.clothing.dto.ClothingImageResponse;

public record OutfitItemResponse(
        Long id,
        String name,
        ClothingCategory category,
        ClothingColor color,
        ClothingMaterial material,
        ClothingImageResponse image
) {

    public static OutfitItemResponse from(ClothingItem clothingItem) {
        return new OutfitItemResponse(
                clothingItem.getId(),
                clothingItem.getName(),
                clothingItem.getCategory(),
                clothingItem.getColor(),
                clothingItem.getMaterial(),
                ClothingImageResponse.from(clothingItem)
        );
    }
}

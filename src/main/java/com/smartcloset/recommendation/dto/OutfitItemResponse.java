package com.smartcloset.recommendation.dto;

import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.domain.ClothingMaterial;

public record OutfitItemResponse(
        Long id,
        String name,
        ClothingCategory category,
        ClothingColor color,
        ClothingMaterial material
) {

    public static OutfitItemResponse from(ClothingItem clothingItem) {
        return new OutfitItemResponse(
                clothingItem.getId(),
                clothingItem.getName(),
                clothingItem.getCategory(),
                clothingItem.getColor(),
                clothingItem.getMaterial()
        );
    }
}

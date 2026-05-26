package com.smartcloset.recommendation.dto;

import com.smartcloset.clothing.application.ClothingStyleTagMapper;
import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.domain.ClothingMaterial;
import com.smartcloset.clothing.dto.ClothingImageResponse;
import java.util.List;

public record OutfitItemResponse(
        Long id,
        String name,
        ClothingCategory category,
        ClothingColor color,
        ClothingMaterial material,
        List<String> styleTags,
        ClothingImageResponse image
) {

    public static OutfitItemResponse from(ClothingItem clothingItem, ClothingStyleTagMapper styleTagMapper) {
        return new OutfitItemResponse(
                clothingItem.getId(),
                clothingItem.getName(),
                clothingItem.getCategory(),
                clothingItem.getColor(),
                clothingItem.getMaterial(),
                styleTagMapper.fromJson(clothingItem.getStyleTagsJson()),
                ClothingImageResponse.from(clothingItem)
        );
    }
}

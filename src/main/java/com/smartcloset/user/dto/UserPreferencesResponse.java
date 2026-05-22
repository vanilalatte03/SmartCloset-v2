package com.smartcloset.user.dto;

import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingMaterial;
import java.util.List;

public record UserPreferencesResponse(
        List<ClothingColor> preferredColors,
        List<ClothingMaterial> preferredMaterials,
        List<String> styleTags
) {
}

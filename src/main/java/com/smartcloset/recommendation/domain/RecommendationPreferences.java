package com.smartcloset.recommendation.domain;

import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingMaterial;
import java.util.Collection;
import java.util.Set;

public record RecommendationPreferences(
        Set<ClothingColor> preferredColors,
        Set<ClothingMaterial> preferredMaterials
) {

    public RecommendationPreferences {
        preferredColors = preferredColors == null ? Set.of() : Set.copyOf(preferredColors);
        preferredMaterials = preferredMaterials == null ? Set.of() : Set.copyOf(preferredMaterials);
    }

    public static RecommendationPreferences empty() {
        return new RecommendationPreferences(Set.of(), Set.of());
    }

    public static RecommendationPreferences of(
            Collection<ClothingColor> preferredColors,
            Collection<ClothingMaterial> preferredMaterials
    ) {
        return new RecommendationPreferences(
                preferredColors == null ? Set.of() : Set.copyOf(preferredColors),
                preferredMaterials == null ? Set.of() : Set.copyOf(preferredMaterials)
        );
    }
}

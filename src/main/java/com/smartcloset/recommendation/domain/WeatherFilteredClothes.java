package com.smartcloset.recommendation.domain;

import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingItem;
import java.util.List;
import java.util.Objects;

public record WeatherFilteredClothes(
        List<ClothingItem> tops,
        List<ClothingItem> bottoms,
        List<ClothingItem> outers
) {

    public WeatherFilteredClothes {
        tops = List.copyOf(Objects.requireNonNull(tops, "tops must not be null"));
        bottoms = List.copyOf(Objects.requireNonNull(bottoms, "bottoms must not be null"));
        outers = List.copyOf(Objects.requireNonNull(outers, "outers must not be null"));
    }

    public List<ClothingItem> allItems() {
        return java.util.stream.Stream.of(tops, bottoms, outers)
                .flatMap(List::stream)
                .toList();
    }

    public boolean hasCategory(ClothingCategory category) {
        return switch (category) {
            case TOP -> !tops.isEmpty();
            case BOTTOM -> !bottoms.isEmpty();
            case OUTER -> !outers.isEmpty();
        };
    }
}

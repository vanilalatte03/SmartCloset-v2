package com.smartcloset.recommendation.domain;

import com.smartcloset.clothing.domain.ClothingItem;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record OutfitCandidate(
        ClothingItem top,
        ClothingItem bottom,
        ClothingItem outer,
        int generationOrder
) {

    public OutfitCandidate {
        Objects.requireNonNull(top, "top must not be null");
        Objects.requireNonNull(bottom, "bottom must not be null");
        if (generationOrder < 0) {
            throw new IllegalArgumentException("generationOrder must not be negative");
        }
    }

    public static OutfitCandidate withoutOuter(ClothingItem top, ClothingItem bottom, int generationOrder) {
        return new OutfitCandidate(top, bottom, null, generationOrder);
    }

    public static OutfitCandidate withOuter(
            ClothingItem top,
            ClothingItem bottom,
            ClothingItem outer,
            int generationOrder
    ) {
        return new OutfitCandidate(top, bottom, Objects.requireNonNull(outer, "outer must not be null"), generationOrder);
    }

    public boolean hasOuter() {
        return outer != null;
    }

    public List<ClothingItem> items() {
        if (outer == null) {
            return List.of(top, bottom);
        }
        return List.of(top, bottom, outer);
    }

    public Set<Long> itemIds() {
        return items().stream()
                .map(ClothingItem::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean hasSameItemSet(Set<Long> otherItemIds) {
        return itemIds().equals(otherItemIds);
    }

    public boolean intersects(Set<Long> otherItemIds) {
        return itemIds().stream().anyMatch(otherItemIds::contains);
    }

    public Stream<ClothingItem> stream() {
        return items().stream();
    }
}

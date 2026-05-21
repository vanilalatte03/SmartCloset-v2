package com.smartcloset.recommendation.domain;

import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.weather.domain.WeatherCondition;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class WeatherSuitabilityFilter {

    private static final Comparator<ClothingItem> BY_ID_ASC =
            Comparator.comparing(ClothingItem::getId, Comparator.nullsLast(Long::compareTo));

    public WeatherFilteredClothes filter(List<ClothingItem> clothes, WeatherCondition weather) {
        Objects.requireNonNull(clothes, "clothes must not be null");
        Objects.requireNonNull(weather, "weather must not be null");

        List<ClothingItem> activeClothes = clothes.stream()
                .filter(item -> !item.isArchived())
                .sorted(BY_ID_ASC)
                .toList();

        if (!hasActiveCategory(activeClothes, ClothingCategory.TOP)
                || !hasActiveCategory(activeClothes, ClothingCategory.BOTTOM)) {
            throw new RecommendationFailureException(RecommendationFailureCode.INSUFFICIENT_CLOSET_ITEMS);
        }

        List<ClothingItem> weatherSuitable = activeClothes.stream()
                .filter(item -> isTemperatureSuitable(item, weather))
                .toList();

        if (weatherSuitable.isEmpty()) {
            throw new RecommendationFailureException(RecommendationFailureCode.NO_WEATHER_SUITABLE_ITEM);
        }

        WeatherFilteredClothes filtered = new WeatherFilteredClothes(
                byCategory(weatherSuitable, ClothingCategory.TOP),
                byCategory(weatherSuitable, ClothingCategory.BOTTOM),
                byCategory(weatherSuitable, ClothingCategory.OUTER)
        );

        if (filtered.tops().isEmpty()) {
            throw new RecommendationFailureException(RecommendationFailureCode.NO_TOP_AVAILABLE);
        }
        if (filtered.bottoms().isEmpty()) {
            throw new RecommendationFailureException(RecommendationFailureCode.NO_BOTTOM_AVAILABLE);
        }
        if (isOuterRequired(weather) && filtered.outers().isEmpty()) {
            throw new RecommendationFailureException(RecommendationFailureCode.OUTER_REQUIRED_BUT_NOT_AVAILABLE);
        }

        return filtered;
    }

    static boolean isOuterRequired(WeatherCondition weather) {
        return weather.temperature() <= 12;
    }

    private boolean hasActiveCategory(List<ClothingItem> activeClothes, ClothingCategory category) {
        return activeClothes.stream().anyMatch(item -> item.getCategory() == category);
    }

    private boolean isTemperatureSuitable(ClothingItem item, WeatherCondition weather) {
        return weather.temperature() >= item.getMinTemperature()
                && weather.temperature() <= item.getMaxTemperature();
    }

    private List<ClothingItem> byCategory(List<ClothingItem> clothes, ClothingCategory category) {
        return clothes.stream()
                .filter(item -> item.getCategory() == category)
                .sorted(BY_ID_ASC)
                .toList();
    }
}

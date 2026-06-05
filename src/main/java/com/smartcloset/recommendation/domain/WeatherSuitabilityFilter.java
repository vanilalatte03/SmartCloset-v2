package com.smartcloset.recommendation.domain;

import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.weather.domain.WeatherCondition;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 추천 후보를 만들기 전에 옷장과 현재 날씨가 최소 조건을 만족하는지 검사한다.
 *
 * <p>필터 실패는 사용자에게 안내해야 하는 business failure이므로 구체적인
 * {@link RecommendationFailureCode}로 변환한다.</p>
 */
public class WeatherSuitabilityFilter {

    private static final Comparator<ClothingItem> BY_ID_ASC =
            Comparator.comparing(ClothingItem::getId, Comparator.nullsLast(Long::compareTo));

    /**
     * 활성 옷 중 현재 날씨 범위를 만족하는 옷만 category별 후보 bucket으로 남긴다.
     */
    public WeatherFilteredClothes filter(List<ClothingItem> clothes, WeatherCondition weather) {
        Objects.requireNonNull(clothes, "clothes must not be null");
        Objects.requireNonNull(weather, "weather must not be null");

        // 보관된 옷은 옷장에는 남아 있지만 추천 후보에서는 제외한다.
        List<ClothingItem> activeClothes = clothes.stream()
                .filter(item -> !item.isArchived())
                .sorted(BY_ID_ASC)
                .toList();

        if (!hasActiveCategory(activeClothes, ClothingCategory.TOP)
                || !hasActiveCategory(activeClothes, ClothingCategory.BOTTOM)) {
            throw new RecommendationFailureException(RecommendationFailureCode.INSUFFICIENT_CLOSET_ITEMS);
        }

        // 각 옷이 가진 min/max temperature 범위를 먼저 통과해야 후보 조합에 참여할 수 있다.
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

    /**
     * 12도 이하는 아우터가 필수라는 추천 정책을 중앙에서 제공한다.
     */
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

package com.smartcloset.recommendation.domain;

import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.domain.ClothingMaterial;
import com.smartcloset.weather.domain.WeatherCondition;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 대형 옷장에서 추천 후보 조합 수가 폭증하지 않도록 category별 계산 후보 pool을 제한한다.
 */
public class RecommendationCandidateBudgeter {

    public static final int MAX_ITEMS_PER_CATEGORY = 32;

    private static final Comparator<Long> ID_ASC = Comparator.nullsLast(Long::compareTo);

    public WeatherFilteredClothes apply(
            WeatherFilteredClothes clothes,
            WeatherCondition weather,
            List<WearHistorySnapshot> wearHistories,
            List<RecommendationHistorySnapshot> recommendationHistories,
            LocalDateTime requestedAt,
            List<ClothingColor> preferredColors,
            List<ClothingMaterial> preferredMaterials,
            List<String> preferredStyleTags,
            RecommendationSituation situation
    ) {
        Objects.requireNonNull(clothes, "clothes must not be null");
        Objects.requireNonNull(weather, "weather must not be null");
        Objects.requireNonNull(wearHistories, "wearHistories must not be null");
        Objects.requireNonNull(recommendationHistories, "recommendationHistories must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        Objects.requireNonNull(preferredColors, "preferredColors must not be null");
        Objects.requireNonNull(preferredMaterials, "preferredMaterials must not be null");
        Objects.requireNonNull(preferredStyleTags, "preferredStyleTags must not be null");

        BudgetContext context = new BudgetContext(
                weather,
                wearHistories,
                recommendationHistories,
                requestedAt,
                Set.copyOf(preferredColors),
                Set.copyOf(preferredMaterials),
                styleTagKeys(preferredStyleTags),
                situationStyleTagKeys(situation)
        );
        return new WeatherFilteredClothes(
                limitPool(clothes.tops(), context),
                limitPool(clothes.bottoms(), context),
                limitPool(clothes.outers(), context)
        );
    }

    private List<ClothingItem> limitPool(List<ClothingItem> items, BudgetContext context) {
        if (items.size() <= MAX_ITEMS_PER_CATEGORY) {
            return items;
        }

        return items.stream()
                .sorted(Comparator
                        .comparingInt((ClothingItem item) -> budgetScore(item, context))
                        .reversed()
                        .thenComparing(ClothingItem::getId, ID_ASC))
                .limit(MAX_ITEMS_PER_CATEGORY)
                .sorted(Comparator.comparing(ClothingItem::getId, ID_ASC))
                .toList();
    }

    private int budgetScore(ClothingItem item, BudgetContext context) {
        return weatherFitScore(item, context.weather())
                + preferenceScore(item, context)
                + historyDiversityScore(item, context);
    }

    private int weatherFitScore(ClothingItem item, WeatherCondition weather) {
        int temperature = weather.temperature();
        int midpointTwice = item.getMinTemperature() + item.getMaxTemperature();
        int centerDistanceTwice = Math.abs(midpointTwice - temperature * 2);
        int edgeMargin = Math.min(
                temperature - item.getMinTemperature(),
                item.getMaxTemperature() - temperature
        );
        int score = Math.max(0, 30 - centerDistanceTwice) + Math.min(Math.max(edgeMargin, 0), 10);

        if (weather.rainy()) {
            score += item.isRainSuitable() ? 8 : -4;
        }
        if (weather.temperature() <= 12 && isWarmMaterial(item.getMaterial())) {
            score += 6;
        }
        if (weather.temperature() >= 25) {
            if (isWarmMaterial(item.getMaterial())) {
                score -= 6;
            } else if (item.getMaterial() == ClothingMaterial.COTTON || item.getMaterial() == ClothingMaterial.DENIM) {
                score += 4;
            }
        }
        if (weather.rainy() && item.getMaterial() == ClothingMaterial.NYLON) {
            score += 3;
        }
        if (weather.rainy() && item.getMaterial() == ClothingMaterial.WOOL) {
            score -= 3;
        }
        return score;
    }

    private int preferenceScore(ClothingItem item, BudgetContext context) {
        int score = 0;
        if (context.preferredColors().contains(item.getColor())) {
            score += 8;
        }
        if (context.preferredMaterials().contains(item.getMaterial())) {
            score += 8;
        }
        Set<String> itemTags = styleTagKeys(RecommendationStyleTags.fromJson(item.getStyleTagsJson()));
        if (intersects(itemTags, context.preferredStyleTags())) {
            score += 6;
        }
        if (intersects(itemTags, context.situationStyleTags())) {
            score += 3;
        }
        return score;
    }

    private int historyDiversityScore(ClothingItem item, BudgetContext context) {
        Long itemId = item.getId();
        if (itemId == null) {
            return 0;
        }
        return wearDiversityScore(itemId, context) + recommendationDiversityScore(itemId, context);
    }

    private int wearDiversityScore(Long itemId, BudgetContext context) {
        return context.wearHistories().stream()
                .filter(history -> history.clothingItemIds().contains(itemId))
                .map(WearHistorySnapshot::wornAt)
                .max(LocalDateTime::compareTo)
                .map(wornAt -> {
                    if (!wornAt.isBefore(context.requestedAt().minusDays(1))) {
                        return 0;
                    }
                    if (!wornAt.isBefore(context.requestedAt().minusDays(3))) {
                        return 4;
                    }
                    if (!wornAt.isBefore(context.requestedAt().minusDays(7))) {
                        return 8;
                    }
                    return 12;
                })
                .orElse(12);
    }

    private int recommendationDiversityScore(Long itemId, BudgetContext context) {
        return context.recommendationHistories().stream()
                .filter(history -> history.clothingItemIds().contains(itemId))
                .map(RecommendationHistorySnapshot::createdAt)
                .max(LocalDateTime::compareTo)
                .map(recommendedAt -> {
                    if (!recommendedAt.isBefore(context.requestedAt().minusDays(3))) {
                        return 0;
                    }
                    if (!recommendedAt.isBefore(context.requestedAt().minusDays(7))) {
                        return 5;
                    }
                    return 10;
                })
                .orElse(10);
    }

    private boolean isWarmMaterial(ClothingMaterial material) {
        return material == ClothingMaterial.KNIT || material == ClothingMaterial.WOOL;
    }

    private boolean intersects(Set<String> left, Set<String> right) {
        return left.stream().anyMatch(right::contains);
    }

    private Set<String> styleTagKeys(List<String> styleTags) {
        return styleTags.stream()
                .map(this::styleTagKey)
                .filter(key -> !key.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    private Set<String> situationStyleTagKeys(RecommendationSituation situation) {
        RecommendationSituation resolvedSituation = situation == null ? RecommendationSituation.CASUAL : situation;
        return switch (resolvedSituation) {
            case WORK -> styleTagKeys(List.of("WORK", "OFFICE", "MINIMAL", "SMART", "출근", "오피스", "미니멀", "단정"));
            case CASUAL -> styleTagKeys(List.of("CASUAL", "DAILY", "COMFORT", "MINIMAL", "캐주얼", "데일리", "편안함", "미니멀"));
            case WORKOUT -> styleTagKeys(List.of("WORKOUT", "SPORTY", "ACTIVE", "COMFORT", "운동", "스포티", "활동적", "편안함"));
            case DATE -> styleTagKeys(List.of("DATE", "NEAT", "POINT", "MINIMAL", "데이트", "깔끔", "포인트", "미니멀"));
            case FORMAL -> styleTagKeys(List.of("FORMAL", "OFFICIAL", "SMART", "MINIMAL", "격식", "포멀", "단정", "미니멀"));
        };
    }

    private String styleTagKey(String styleTag) {
        if (styleTag == null) {
            return "";
        }
        return styleTag.trim().toLowerCase(Locale.ROOT);
    }

    private record BudgetContext(
            WeatherCondition weather,
            List<WearHistorySnapshot> wearHistories,
            List<RecommendationHistorySnapshot> recommendationHistories,
            LocalDateTime requestedAt,
            Set<ClothingColor> preferredColors,
            Set<ClothingMaterial> preferredMaterials,
            Set<String> preferredStyleTags,
            Set<String> situationStyleTags
    ) {
    }
}

package com.smartcloset.recommendation.domain;

import static com.smartcloset.recommendation.domain.RecommendationDomainTestFixtures.clothing;
import static com.smartcloset.recommendation.domain.RecommendationDomainTestFixtures.recommendationHistory;
import static com.smartcloset.recommendation.domain.RecommendationDomainTestFixtures.user;
import static com.smartcloset.recommendation.domain.RecommendationDomainTestFixtures.wearHistory;
import static org.assertj.core.api.Assertions.assertThat;

import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.domain.ClothingMaterial;
import com.smartcloset.user.domain.User;
import com.smartcloset.weather.domain.WeatherCondition;
import com.smartcloset.weather.domain.WeatherType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class RecommendationCandidateBudgeterTest {

    private final RecommendationCandidateBudgeter budgeter = new RecommendationCandidateBudgeter();
    private final LocalDateTime requestedAt = LocalDateTime.of(2026, 6, 10, 12, 0);

    @Test
    void limitsEachCategoryPoolWithDeterministicSelectionAndIdOrderedGenerationPool() {
        User user = user(1);
        int overBudgetId = RecommendationCandidateBudgeter.MAX_ITEMS_PER_CATEGORY + 3;
        List<ClothingItem> tops = candidates(user, ClothingCategory.TOP, 1, overBudgetId, ClothingColor.BLACK, ClothingMaterial.COTTON);
        List<ClothingItem> bottoms = candidates(user, ClothingCategory.BOTTOM, 101, overBudgetId, ClothingColor.BLACK, ClothingMaterial.COTTON);
        List<ClothingItem> outers = candidates(user, ClothingCategory.OUTER, 201, overBudgetId, ClothingColor.BLACK, ClothingMaterial.COTTON);
        ClothingItem preferredTop = clothing(overBudgetId, user, ClothingCategory.TOP, ClothingColor.RED, ClothingMaterial.COTTON, 0, 30, false);
        ClothingItem preferredBottom = clothing(100 + overBudgetId, user, ClothingCategory.BOTTOM, ClothingColor.BLACK, ClothingMaterial.NYLON, 0, 30, false);
        ClothingItem preferredOuter = clothing(
                200 + overBudgetId,
                user,
                ClothingCategory.OUTER,
                ClothingColor.BLACK,
                ClothingMaterial.COTTON,
                0,
                30,
                false,
                List.of("favorite")
        );

        WeatherFilteredClothes budgeted = budgeter.apply(
                new WeatherFilteredClothes(replaceLast(tops, preferredTop), replaceLast(bottoms, preferredBottom), replaceLast(outers, preferredOuter)),
                WeatherCondition.of(18, WeatherType.CLOUDY, false, false),
                List.of(),
                List.of(),
                requestedAt,
                List.of(ClothingColor.RED),
                List.of(ClothingMaterial.NYLON),
                List.of("favorite"),
                RecommendationSituation.CASUAL
        );

        assertThat(budgeted.tops()).hasSize(RecommendationCandidateBudgeter.MAX_ITEMS_PER_CATEGORY);
        assertThat(budgeted.bottoms()).hasSize(RecommendationCandidateBudgeter.MAX_ITEMS_PER_CATEGORY);
        assertThat(budgeted.outers()).hasSize(RecommendationCandidateBudgeter.MAX_ITEMS_PER_CATEGORY);
        assertThat(ids(budgeted.tops())).contains((long) overBudgetId);
        assertThat(ids(budgeted.bottoms())).contains((long) (100 + overBudgetId));
        assertThat(ids(budgeted.outers())).contains((long) (200 + overBudgetId));
        assertThat(ids(budgeted.tops())).isSorted();
        assertThat(ids(budgeted.bottoms())).isSorted();
        assertThat(ids(budgeted.outers())).isSorted();
    }

    @Test
    void keepsPoolsUnchangedWhenTheyAreAtOrBelowBudget() {
        User user = user(1);
        ClothingItem top2 = clothing(2, user, ClothingCategory.TOP, ClothingColor.WHITE, ClothingMaterial.COTTON, 0, 30, false);
        ClothingItem top1 = clothing(1, user, ClothingCategory.TOP, ClothingColor.BLACK, ClothingMaterial.COTTON, 0, 30, false);
        ClothingItem bottom = clothing(3, user, ClothingCategory.BOTTOM, ClothingColor.BLACK, ClothingMaterial.DENIM, 0, 30, false);
        WeatherFilteredClothes filtered = new WeatherFilteredClothes(List.of(top2, top1), List.of(bottom), List.of());

        WeatherFilteredClothes budgeted = budgeter.apply(
                filtered,
                WeatherCondition.of(20, WeatherType.CLOUDY, false, false),
                List.of(),
                List.of(),
                requestedAt,
                List.of(ClothingColor.BLACK),
                List.of(),
                List.of(),
                RecommendationSituation.CASUAL
        );

        assertThat(budgeted.tops()).containsExactly(top2, top1);
        assertThat(budgeted.bottoms()).containsExactly(bottom);
        assertThat(budgeted.outers()).isEmpty();
    }

    @Test
    void ignoresImageMetadataWhenSelectingBudgetPool() {
        User user = user(1);
        List<ClothingItem> tops = candidates(
                user,
                ClothingCategory.TOP,
                1,
                RecommendationCandidateBudgeter.MAX_ITEMS_PER_CATEGORY + 1,
                ClothingColor.BLACK,
                ClothingMaterial.COTTON
        );
        ClothingItem bottom = clothing(100, user, ClothingCategory.BOTTOM, ClothingColor.BLACK, ClothingMaterial.DENIM, 0, 30, false);
        WeatherFilteredClothes filtered = new WeatherFilteredClothes(tops, List.of(bottom), List.of());
        WeatherCondition weather = WeatherCondition.of(20, WeatherType.CLOUDY, false, false);

        List<Long> before = ids(apply(filtered, weather).tops());
        tops.get(tops.size() - 1).updateImageMetadata("image.webp", "image/webp", 1234, requestedAt);
        List<Long> after = ids(apply(filtered, weather).tops());

        assertThat(after).isEqualTo(before);
    }

    @Test
    void boundsGeneratedCandidateCountAfterBudgetingLargeCloset() {
        User user = user(1);
        int itemCount = RecommendationCandidateBudgeter.MAX_ITEMS_PER_CATEGORY + 8;
        WeatherCondition weather = WeatherCondition.of(18, WeatherType.CLOUDY, false, false);
        WeatherFilteredClothes budgeted = apply(
                new WeatherFilteredClothes(
                        candidates(user, ClothingCategory.TOP, 1, itemCount, ClothingColor.BLACK, ClothingMaterial.COTTON),
                        candidates(user, ClothingCategory.BOTTOM, 101, itemCount, ClothingColor.BLACK, ClothingMaterial.DENIM),
                        candidates(user, ClothingCategory.OUTER, 201, itemCount, ClothingColor.BLACK, ClothingMaterial.COTTON)
                ),
                weather
        );
        int[] generatedCount = new int[1];

        new OutfitCandidateGenerator().forEach(budgeted, weather, candidate -> generatedCount[0]++);

        int max = RecommendationCandidateBudgeter.MAX_ITEMS_PER_CATEGORY;
        assertThat(generatedCount[0]).isEqualTo(max * max * (1 + max));
    }

    @Test
    void penalizesRecentlyWornAndRecommendedItemsWhenBudgetIsExceeded() {
        User user = user(1);
        int overBudgetId = RecommendationCandidateBudgeter.MAX_ITEMS_PER_CATEGORY + 1;
        ClothingItem recentTop = clothing(1, user, ClothingCategory.TOP, ClothingColor.BLACK, ClothingMaterial.COTTON, 0, 30, false);
        ClothingItem freshTop = clothing(overBudgetId, user, ClothingCategory.TOP, ClothingColor.BLACK, ClothingMaterial.COTTON, 0, 30, false);
        List<ClothingItem> tops = replaceLast(
                candidates(user, ClothingCategory.TOP, 1, overBudgetId, ClothingColor.BLACK, ClothingMaterial.COTTON),
                freshTop
        );

        WeatherFilteredClothes budgeted = budgeter.apply(
                new WeatherFilteredClothes(tops, List.of(clothing(100, user, ClothingCategory.BOTTOM, ClothingColor.BLACK, ClothingMaterial.DENIM, 0, 30, false)), List.of()),
                WeatherCondition.of(20, WeatherType.CLOUDY, false, false),
                List.of(wearHistory(1, requestedAt.minusHours(2), recentTop)),
                List.of(recommendationHistory(2, requestedAt.minusHours(1), recentTop)),
                requestedAt,
                List.of(),
                List.of(),
                List.of(),
                RecommendationSituation.CASUAL
        );

        assertThat(ids(budgeted.tops())).contains((long) overBudgetId);
        assertThat(ids(budgeted.tops())).doesNotContain(recentTop.getId());
    }

    private WeatherFilteredClothes apply(WeatherFilteredClothes filtered, WeatherCondition weather) {
        return budgeter.apply(
                filtered,
                weather,
                List.of(),
                List.of(),
                requestedAt,
                List.of(),
                List.of(),
                List.of(),
                RecommendationSituation.CASUAL
        );
    }

    private List<ClothingItem> candidates(
            User user,
            ClothingCategory category,
            int startId,
            int count,
            ClothingColor color,
            ClothingMaterial material
    ) {
        return IntStream.range(0, count)
                .mapToObj(offset -> clothing(startId + offset, user, category, color, material, 0, 30, false))
                .toList();
    }

    private List<ClothingItem> replaceLast(List<ClothingItem> items, ClothingItem replacement) {
        return IntStream.range(0, items.size())
                .mapToObj(index -> index == items.size() - 1 ? replacement : items.get(index))
                .toList();
    }

    private List<Long> ids(List<ClothingItem> items) {
        return items.stream().map(ClothingItem::getId).toList();
    }
}

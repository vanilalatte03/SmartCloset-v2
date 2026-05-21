package com.smartcloset.recommendation.domain;

import static com.smartcloset.recommendation.domain.RecommendationDomainTestFixtures.clothing;
import static com.smartcloset.recommendation.domain.RecommendationDomainTestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.domain.ClothingMaterial;
import com.smartcloset.user.domain.User;
import com.smartcloset.weather.domain.WeatherCondition;
import com.smartcloset.weather.domain.WeatherType;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecommendationWeatherSuitabilityFilterTest {

    private final WeatherSuitabilityFilter filter = new WeatherSuitabilityFilter();
    private final WeatherCondition coldWeather = WeatherCondition.of(12, WeatherType.CLOUDY, false, false);

    @Test
    void excludesArchivedAndTemperatureUnsuitableItems() {
        User user = user(1);
        ClothingItem top = clothing(1, user, ClothingCategory.TOP, ClothingColor.WHITE, ClothingMaterial.KNIT, 0, 16, false);
        ClothingItem archivedTop = clothing(2, user, ClothingCategory.TOP, ClothingColor.GRAY, ClothingMaterial.KNIT, 0, 16, false);
        archivedTop.archive();
        ClothingItem bottom = clothing(3, user, ClothingCategory.BOTTOM, ClothingColor.BLACK, ClothingMaterial.DENIM, 0, 20, false);
        ClothingItem tooWarmBottom = clothing(4, user, ClothingCategory.BOTTOM, ClothingColor.BLUE, ClothingMaterial.DENIM, 13, 30, false);
        ClothingItem outer = clothing(5, user, ClothingCategory.OUTER, ClothingColor.NAVY, ClothingMaterial.WOOL, -10, 12, false);

        WeatherFilteredClothes filtered = filter.filter(
                List.of(top, archivedTop, bottom, tooWarmBottom, outer),
                coldWeather
        );

        assertThat(filtered.tops()).containsExactly(top);
        assertThat(filtered.bottoms()).containsExactly(bottom);
        assertThat(filtered.outers()).containsExactly(outer);
        assertThat(filtered.allItems()).doesNotContain(archivedTop, tooWarmBottom);
    }

    @Test
    void failsWithInsufficientClosetItemsWhenActiveTopOrBottomCompositionIsImpossible() {
        User user = user(1);
        ClothingItem top = clothing(1, user, ClothingCategory.TOP, ClothingColor.WHITE, ClothingMaterial.COTTON, 0, 30, false);

        assertThatThrownBy(() -> filter.filter(List.of(top), WeatherCondition.of(20, WeatherType.SUNNY, false, false)))
                .isInstanceOfSatisfying(RecommendationFailureException.class, exception ->
                        assertThat(exception.failureCode()).isEqualTo(RecommendationFailureCode.INSUFFICIENT_CLOSET_ITEMS));
    }

    @Test
    void failsWithNoWeatherSuitableItemWhenAllActiveClothesMissTemperatureRange() {
        User user = user(1);
        ClothingItem top = clothing(1, user, ClothingCategory.TOP, ClothingColor.WHITE, ClothingMaterial.COTTON, 0, 5, false);
        ClothingItem bottom = clothing(2, user, ClothingCategory.BOTTOM, ClothingColor.BLACK, ClothingMaterial.DENIM, 20, 30, false);

        assertThatThrownBy(() -> filter.filter(List.of(top, bottom), coldWeather))
                .isInstanceOfSatisfying(RecommendationFailureException.class, exception ->
                        assertThat(exception.failureCode()).isEqualTo(RecommendationFailureCode.NO_WEATHER_SUITABLE_ITEM));
    }

    @Test
    void failsWithNoTopAvailableWhenHardFilterRemovesAllTops() {
        User user = user(1);
        ClothingItem top = clothing(1, user, ClothingCategory.TOP, ClothingColor.WHITE, ClothingMaterial.COTTON, 0, 5, false);
        ClothingItem bottom = clothing(2, user, ClothingCategory.BOTTOM, ClothingColor.BLACK, ClothingMaterial.DENIM, 0, 20, false);
        ClothingItem outer = clothing(3, user, ClothingCategory.OUTER, ClothingColor.NAVY, ClothingMaterial.WOOL, -10, 12, false);

        assertThatThrownBy(() -> filter.filter(List.of(top, bottom, outer), coldWeather))
                .isInstanceOfSatisfying(RecommendationFailureException.class, exception ->
                        assertThat(exception.failureCode()).isEqualTo(RecommendationFailureCode.NO_TOP_AVAILABLE));
    }

    @Test
    void failsWithNoBottomAvailableWhenHardFilterRemovesAllBottoms() {
        User user = user(1);
        ClothingItem top = clothing(1, user, ClothingCategory.TOP, ClothingColor.WHITE, ClothingMaterial.COTTON, 0, 20, false);
        ClothingItem bottom = clothing(2, user, ClothingCategory.BOTTOM, ClothingColor.BLACK, ClothingMaterial.DENIM, 20, 30, false);
        ClothingItem outer = clothing(3, user, ClothingCategory.OUTER, ClothingColor.NAVY, ClothingMaterial.WOOL, -10, 12, false);

        assertThatThrownBy(() -> filter.filter(List.of(top, bottom, outer), coldWeather))
                .isInstanceOfSatisfying(RecommendationFailureException.class, exception ->
                        assertThat(exception.failureCode()).isEqualTo(RecommendationFailureCode.NO_BOTTOM_AVAILABLE));
    }

    @Test
    void failsWithOuterRequiredWhenColdWeatherHasNoSuitableOuter() {
        User user = user(1);
        ClothingItem top = clothing(1, user, ClothingCategory.TOP, ClothingColor.WHITE, ClothingMaterial.COTTON, 0, 20, false);
        ClothingItem bottom = clothing(2, user, ClothingCategory.BOTTOM, ClothingColor.BLACK, ClothingMaterial.DENIM, 0, 20, false);
        ClothingItem outer = clothing(3, user, ClothingCategory.OUTER, ClothingColor.NAVY, ClothingMaterial.WOOL, -10, 8, false);

        assertThatThrownBy(() -> filter.filter(List.of(top, bottom, outer), coldWeather))
                .isInstanceOfSatisfying(RecommendationFailureException.class, exception ->
                        assertThat(exception.failureCode()).isEqualTo(RecommendationFailureCode.OUTER_REQUIRED_BUT_NOT_AVAILABLE));
    }
}

package com.smartcloset.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.domain.ClothingMaterial;
import com.smartcloset.recommendation.domain.OutfitSlot;
import com.smartcloset.recommendation.domain.RecommendationResult;
import com.smartcloset.recommendation.domain.RecommendationScore;
import com.smartcloset.user.domain.User;
import com.smartcloset.weather.domain.WeatherCondition;
import com.smartcloset.weather.domain.WeatherType;
import org.junit.jupiter.api.Test;

class EntityBehaviorTest {

    @Test
    void clothingItemArchiveIsIdempotentAndUpdateDoesNotChangeArchived() {
        User user = User.createSeedUser("demo-user");
        ClothingItem item = ClothingItem.create(
                user,
                "Gray Knit",
                ClothingCategory.TOP,
                ClothingColor.GRAY,
                ClothingMaterial.KNIT,
                5,
                18,
                false
        );

        item.archive();
        item.archive();
        item.updateDetails(
                "Warm Gray Knit",
                ClothingCategory.TOP,
                ClothingColor.GRAY,
                ClothingMaterial.KNIT,
                3,
                16,
                false
        );

        assertThat(item.isArchived()).isTrue();
        assertThat(item.getName()).isEqualTo("Warm Gray Knit");
        assertThat(item.getMinTemperature()).isEqualTo(3);
        assertThat(item.getMaxTemperature()).isEqualTo(16);
    }

    @Test
    void recommendationResultStoresItemsAndMarkWornIsIdempotent() {
        User user = User.createSeedUser("demo-user");
        ClothingItem top = ClothingItem.create(
                user,
                "아이보리 니트",
                ClothingCategory.TOP,
                ClothingColor.WHITE,
                ClothingMaterial.KNIT,
                0,
                16,
                false
        );
        RecommendationResult result = RecommendationResult.create(
                user,
                WeatherCondition.of(12, WeatherType.CLOUDY, false, false),
                RecommendationScore.of(88, 35, 25, 20, 8, 0),
                "[\"현재 기온이 낮아 아우터를 포함한 조합을 추천했습니다.\"]"
        );

        result.addItem(top, OutfitSlot.TOP);
        result.markWorn();
        result.markWorn();

        assertThat(result.isWorn()).isTrue();
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getClothingItem()).isSameAs(top);
        assertThat(result.getItems().get(0).getSlot()).isEqualTo(OutfitSlot.TOP);
    }
}

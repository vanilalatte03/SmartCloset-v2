package com.smartcloset.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.domain.ClothingMaterial;
import com.smartcloset.recommendation.domain.OutfitSlot;
import com.smartcloset.recommendation.domain.RecommendationResult;
import com.smartcloset.recommendation.domain.RecommendationResultItem;
import com.smartcloset.recommendation.domain.RecommendationScore;
import com.smartcloset.user.domain.User;
import com.smartcloset.weather.domain.WeatherCondition;
import com.smartcloset.weather.domain.WeatherType;
import java.time.LocalDateTime;
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
    void clothingImageMetadataCanBeUpdatedAndClearedWithoutChangingDetails() {
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
        LocalDateTime uploadedAt = LocalDateTime.of(2026, 5, 25, 10, 0);

        item.updateImageMetadata("image-uuid.jpg", "image/jpeg", 123_456L, uploadedAt);

        assertThat(item.getImageStoredFilename()).isEqualTo("image-uuid.jpg");
        assertThat(item.getImageContentType()).isEqualTo("image/jpeg");
        assertThat(item.getImageSizeBytes()).isEqualTo(123_456L);
        assertThat(item.getImageUploadedAt()).isEqualTo(uploadedAt);
        assertThat(item.getName()).isEqualTo("Gray Knit");
        assertThat(item.isArchived()).isFalse();

        item.clearImageMetadata();

        assertThat(item.getImageStoredFilename()).isNull();
        assertThat(item.getImageContentType()).isNull();
        assertThat(item.getImageSizeBytes()).isNull();
        assertThat(item.getImageUploadedAt()).isNull();
    }

    @Test
    void recommendationResultItemLinksResultAndClothingItemAndMarkWornIsIdempotent() {
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

        RecommendationResultItem resultItem = RecommendationResultItem.of(result, top, OutfitSlot.TOP);
        result.markWorn();
        result.markWorn();

        assertThat(result.isWorn()).isTrue();
        assertThat(resultItem.getRecommendationResult()).isSameAs(result);
        assertThat(resultItem.getClothingItem()).isSameAs(top);
        assertThat(resultItem.getSlot()).isEqualTo(OutfitSlot.TOP);
    }
}

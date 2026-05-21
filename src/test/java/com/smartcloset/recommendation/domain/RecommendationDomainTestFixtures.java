package com.smartcloset.recommendation.domain;

import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.domain.ClothingMaterial;
import com.smartcloset.user.domain.User;
import com.smartcloset.weather.domain.WeatherCondition;
import com.smartcloset.weather.domain.WeatherType;
import java.time.LocalDateTime;
import org.springframework.test.util.ReflectionTestUtils;

final class RecommendationDomainTestFixtures {

    private RecommendationDomainTestFixtures() {
    }

    static User user(long id) {
        User user = User.createSeedUser("demo-user-" + id);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    static ClothingItem clothing(
            long id,
            User user,
            ClothingCategory category,
            ClothingColor color,
            ClothingMaterial material,
            int minTemperature,
            int maxTemperature,
            boolean rainSuitable
    ) {
        ClothingItem item = ClothingItem.create(
                user,
                category.name() + "-" + id,
                category,
                color,
                material,
                minTemperature,
                maxTemperature,
                rainSuitable
        );
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    static OutfitCandidate candidate(ClothingItem top, ClothingItem bottom) {
        return OutfitCandidate.withoutOuter(top, bottom, 0);
    }

    static OutfitCandidate candidate(ClothingItem top, ClothingItem bottom, ClothingItem outer) {
        return OutfitCandidate.withOuter(top, bottom, outer, 0);
    }

    static RecommendationResult recommendation(
            long id,
            User user,
            LocalDateTime createdAt,
            ClothingItem... items
    ) {
        RecommendationResult result = RecommendationResult.create(
                user,
                WeatherCondition.of(12, WeatherType.CLOUDY, false, false),
                RecommendationScore.of(90, 35, 25, 20, 10, 0),
                "[\"test\"]"
        );
        ReflectionTestUtils.setField(result, "id", id);
        ReflectionTestUtils.setField(result, "createdAt", createdAt);
        for (ClothingItem item : items) {
            result.addItem(item, OutfitSlot.valueOf(item.getCategory().name()));
        }
        return result;
    }

    static WearHistory wearHistory(
            long id,
            User user,
            RecommendationResult recommendationResult,
            LocalDateTime wornAt
    ) {
        WearHistory wearHistory = WearHistory.record(user, recommendationResult, wornAt);
        ReflectionTestUtils.setField(wearHistory, "id", id);
        return wearHistory;
    }
}

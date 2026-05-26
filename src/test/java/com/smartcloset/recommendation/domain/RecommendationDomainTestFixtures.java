package com.smartcloset.recommendation.domain;

import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.domain.ClothingMaterial;
import com.smartcloset.user.domain.User;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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

    static ClothingItem clothing(
            long id,
            User user,
            ClothingCategory category,
            ClothingColor color,
            ClothingMaterial material,
            int minTemperature,
            int maxTemperature,
            boolean rainSuitable,
            List<String> styleTags
    ) {
        ClothingItem item = ClothingItem.create(
                user,
                category.name() + "-" + id,
                category,
                color,
                material,
                minTemperature,
                maxTemperature,
                rainSuitable,
                toJson(styleTags)
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

    static RecommendationHistorySnapshot recommendationHistory(
            long id,
            LocalDateTime createdAt,
            ClothingItem... items
    ) {
        return new RecommendationHistorySnapshot(id, createdAt, itemIds(items));
    }

    static RecommendationHistorySnapshot feedbackHistory(
            long id,
            LocalDateTime createdAt,
            int weatherTemperature,
            RecommendationFeedbackSentiment sentiment,
            RecommendationThermalFeedback thermal,
            LocalDateTime feedbackUpdatedAt,
            ClothingItem... items
    ) {
        return new RecommendationHistorySnapshot(
                id,
                createdAt,
                itemIds(items),
                weatherTemperature,
                sentiment,
                thermal,
                feedbackUpdatedAt
        );
    }

    static WearHistorySnapshot wearHistory(
            long id,
            LocalDateTime wornAt,
            ClothingItem... items
    ) {
        return new WearHistorySnapshot(id, wornAt, itemIds(items));
    }

    private static Set<Long> itemIds(ClothingItem... items) {
        return Arrays.stream(items)
                .map(ClothingItem::getId)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String toJson(List<String> values) {
        return "[\"" + String.join("\",\"", values) + "\"]";
    }
}

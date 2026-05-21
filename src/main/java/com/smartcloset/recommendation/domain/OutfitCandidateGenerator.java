package com.smartcloset.recommendation.domain;

import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.weather.domain.WeatherCondition;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OutfitCandidateGenerator {

    public List<OutfitCandidate> generate(WeatherFilteredClothes clothes, WeatherCondition weather) {
        Objects.requireNonNull(clothes, "clothes must not be null");
        Objects.requireNonNull(weather, "weather must not be null");

        List<OutfitCandidate> candidates = new ArrayList<>();
        int generationOrder = 0;

        for (ClothingItem top : clothes.tops()) {
            for (ClothingItem bottom : clothes.bottoms()) {
                if (!WeatherSuitabilityFilter.isOuterRequired(weather)) {
                    candidates.add(OutfitCandidate.withoutOuter(top, bottom, generationOrder++));
                }
                for (ClothingItem outer : clothes.outers()) {
                    candidates.add(OutfitCandidate.withOuter(top, bottom, outer, generationOrder++));
                }
            }
        }

        if (candidates.isEmpty()) {
            throw new RecommendationFailureException(RecommendationFailureCode.INSUFFICIENT_CLOSET_ITEMS);
        }

        return List.copyOf(candidates);
    }
}

package com.smartcloset.recommendation.domain;

import static com.smartcloset.recommendation.domain.RecommendationDomainTestFixtures.clothing;
import static com.smartcloset.recommendation.domain.RecommendationDomainTestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;

import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.domain.ClothingMaterial;
import com.smartcloset.user.domain.User;
import com.smartcloset.weather.domain.WeatherCondition;
import com.smartcloset.weather.domain.WeatherType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecommendationOutfitCandidateGeneratorTest {

    private final OutfitCandidateGenerator generator = new OutfitCandidateGenerator();

    @Test
    void generatesOnlyOuterCandidatesWhenTemperatureIsTwelve() {
        User user = user(1);
        ClothingItem top = clothing(1, user, ClothingCategory.TOP, ClothingColor.WHITE, ClothingMaterial.KNIT, 0, 16, false);
        ClothingItem bottom = clothing(2, user, ClothingCategory.BOTTOM, ClothingColor.BLACK, ClothingMaterial.DENIM, 0, 20, false);
        ClothingItem outer1 = clothing(3, user, ClothingCategory.OUTER, ClothingColor.NAVY, ClothingMaterial.WOOL, -10, 12, false);
        ClothingItem outer2 = clothing(4, user, ClothingCategory.OUTER, ClothingColor.BLACK, ClothingMaterial.NYLON, 5, 18, true);

        List<OutfitCandidate> candidates = generator.generate(
                new WeatherFilteredClothes(List.of(top), List.of(bottom), List.of(outer1, outer2)),
                WeatherCondition.of(12, WeatherType.CLOUDY, false, false)
        );

        assertThat(candidates).hasSize(2);
        assertThat(candidates).allMatch(OutfitCandidate::hasOuter);
        assertThat(candidates).extracting(OutfitCandidate::outer).containsExactly(outer1, outer2);
        assertThat(candidates).extracting(OutfitCandidate::generationOrder).containsExactly(0, 1);
    }

    @Test
    void generatesNoOuterCandidateBeforeOuterCandidatesWhenOuterIsOptional() {
        User user = user(1);
        ClothingItem top = clothing(1, user, ClothingCategory.TOP, ClothingColor.WHITE, ClothingMaterial.COTTON, 0, 25, false);
        ClothingItem bottom = clothing(2, user, ClothingCategory.BOTTOM, ClothingColor.BLACK, ClothingMaterial.DENIM, 0, 25, false);
        ClothingItem outer = clothing(3, user, ClothingCategory.OUTER, ClothingColor.NAVY, ClothingMaterial.NYLON, 5, 18, true);

        List<OutfitCandidate> candidates = generator.generate(
                new WeatherFilteredClothes(List.of(top), List.of(bottom), List.of(outer)),
                WeatherCondition.of(15, WeatherType.CLOUDY, false, false)
        );

        assertThat(candidates).hasSize(2);
        assertThat(candidates.get(0).hasOuter()).isFalse();
        assertThat(candidates.get(1).hasOuter()).isTrue();
        assertThat(candidates).extracting(OutfitCandidate::generationOrder).containsExactly(0, 1);
    }

    @Test
    void visitsCandidatesInGeneratedOrderWithoutRequiringCallerToBuildFullList() {
        User user = user(1);
        ClothingItem top = clothing(1, user, ClothingCategory.TOP, ClothingColor.WHITE, ClothingMaterial.COTTON, 0, 25, false);
        ClothingItem bottom = clothing(2, user, ClothingCategory.BOTTOM, ClothingColor.BLACK, ClothingMaterial.DENIM, 0, 25, false);
        ClothingItem outer = clothing(3, user, ClothingCategory.OUTER, ClothingColor.NAVY, ClothingMaterial.NYLON, 5, 18, true);
        WeatherFilteredClothes clothes = new WeatherFilteredClothes(List.of(top), List.of(bottom), List.of(outer));
        WeatherCondition weather = WeatherCondition.of(15, WeatherType.CLOUDY, false, false);
        List<OutfitCandidate> visited = new ArrayList<>();

        generator.forEach(clothes, weather, visited::add);

        assertThat(visited).containsExactlyElementsOf(generator.generate(clothes, weather));
    }
}

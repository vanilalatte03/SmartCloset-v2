package com.smartcloset.recommendation.domain;

import static com.smartcloset.recommendation.domain.RecommendationDomainTestFixtures.candidate;
import static com.smartcloset.recommendation.domain.RecommendationDomainTestFixtures.clothing;
import static com.smartcloset.recommendation.domain.RecommendationDomainTestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;

import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingMaterial;
import com.smartcloset.user.domain.User;
import com.smartcloset.weather.domain.WeatherCondition;
import com.smartcloset.weather.domain.WeatherType;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecommendationReasonGeneratorTest {

    private final RecommendationScorer scorer = new RecommendationScorer();
    private final RecommendationReasonGenerator generator = new RecommendationReasonGenerator();
    private final LocalDateTime requestedAt = LocalDateTime.of(2026, 5, 21, 12, 0);

    @Test
    void generatesThreeToFiveReasonsByPriority() {
        User user = user(1);
        OutfitCandidate candidate = candidate(
                clothing(1, user, ClothingCategory.TOP, ClothingColor.WHITE, ClothingMaterial.KNIT, 0, 16, false),
                clothing(2, user, ClothingCategory.BOTTOM, ClothingColor.BLACK, ClothingMaterial.DENIM, 0, 20, false),
                clothing(3, user, ClothingCategory.OUTER, ClothingColor.NAVY, ClothingMaterial.WOOL, -10, 12, false)
        );
        WeatherCondition weather = WeatherCondition.of(12, WeatherType.CLOUDY, false, false);
        RecommendationScore score = scorer.score(candidate, weather, List.of(), List.of(), requestedAt);

        List<String> reasons = generator.generate(candidate, score, weather, List.of(), List.of(), requestedAt);

        assertThat(reasons).hasSizeBetween(3, 5);
        assertThat(reasons.get(0)).isEqualTo("현재 기온이 낮아 아우터를 포함한 조합을 추천했습니다.");
        assertThat(reasons.get(1)).isEqualTo("상의와 하의 색상이 무채색 중심이라 안정적인 조합입니다.");
        assertThat(reasons.get(2)).isEqualTo("최근 착용 이력이 적어 반복 착용 부담이 낮습니다.");
        assertThat(reasons).contains("니트 또는 울 소재가 현재 기온에 적합해 보온성을 보완합니다.");
    }

    @Test
    void generatesRainyWoolMaterialReasonWhenWeatherScoreIsPenalized() {
        User user = user(1);
        OutfitCandidate candidate = candidate(
                clothing(1, user, ClothingCategory.TOP, ClothingColor.WHITE, ClothingMaterial.WOOL, 0, 30, false),
                clothing(2, user, ClothingCategory.BOTTOM, ClothingColor.BLACK, ClothingMaterial.DENIM, 0, 30, false)
        );
        WeatherCondition weather = WeatherCondition.of(20, WeatherType.RAINY, true, false);
        RecommendationScore score = scorer.score(candidate, weather, List.of(), List.of(), requestedAt);

        List<String> reasons = generator.generate(candidate, score, weather, List.of(), List.of(), requestedAt);

        assertThat(reasons).contains("비 오는 날 울 소재는 젖었을 때 불편할 수 있어 날씨 점수가 낮아졌습니다.");
    }

    @Test
    void generatesPreferenceReasonOnlyWhenPreferenceScoreIsPositive() {
        User user = user(1);
        OutfitCandidate candidate = candidate(
                clothing(1, user, ClothingCategory.TOP, ClothingColor.NAVY, ClothingMaterial.COTTON, 0, 30, false),
                clothing(2, user, ClothingCategory.BOTTOM, ClothingColor.BLACK, ClothingMaterial.DENIM, 0, 30, false)
        );
        WeatherCondition weather = WeatherCondition.of(20, WeatherType.CLOUDY, false, false);
        RecommendationScore noPreferenceScore = scorer.score(candidate, weather, List.of(), List.of(), requestedAt);
        RecommendationScore preferenceScore = scorer.score(
                candidate,
                weather,
                List.of(),
                List.of(),
                requestedAt,
                List.of(ClothingColor.NAVY),
                List.of()
        );

        List<String> noPreferenceReasons = generator.generate(
                candidate,
                noPreferenceScore,
                weather,
                List.of(),
                List.of(),
                requestedAt
        );
        List<String> preferenceReasons = generator.generate(
                candidate,
                preferenceScore,
                weather,
                List.of(),
                List.of(),
                requestedAt
        );

        assertThat(noPreferenceReasons).doesNotContain("선호 색상 또는 소재와 맞는 옷이 포함되어 있습니다.");
        assertThat(preferenceReasons).contains("선호 색상 또는 소재와 맞는 옷이 포함되어 있습니다.");
    }
}

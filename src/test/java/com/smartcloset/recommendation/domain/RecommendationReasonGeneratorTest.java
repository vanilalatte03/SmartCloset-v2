package com.smartcloset.recommendation.domain;

import static com.smartcloset.recommendation.domain.RecommendationDomainTestFixtures.candidate;
import static com.smartcloset.recommendation.domain.RecommendationDomainTestFixtures.clothing;
import static com.smartcloset.recommendation.domain.RecommendationDomainTestFixtures.feedbackHistory;
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
    void generatesPreferenceAndStyleTagReasonsWhenScoreIsPositive() {
        User user = user(1);
        OutfitCandidate candidate = candidate(
                clothing(
                        1,
                        user,
                        ClothingCategory.TOP,
                        ClothingColor.NAVY,
                        ClothingMaterial.COTTON,
                        0,
                        30,
                        false,
                        List.of("OFFICE")
                ),
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
                List.of(),
                List.of("office"),
                RecommendationSituation.WORK
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
                requestedAt,
                List.of("office"),
                RecommendationSituation.WORK
        );

        assertThat(noPreferenceReasons).doesNotContain("선호 색상, 소재 또는 스타일 태그를 일부 반영했습니다.");
        assertThat(preferenceReasons)
                .contains("선호하는 스타일 태그와 겹치는 옷을 반영했어요.")
                .contains("출근 상황에 맞는 스타일 태그를 반영했어요.");
    }

    @Test
    void generatesFeedbackReasonWithNegativePriorityOverPositive() {
        User user = user(1);
        var top = clothing(1, user, ClothingCategory.TOP, ClothingColor.WHITE, ClothingMaterial.COTTON, 0, 30, false);
        var bottom = clothing(2, user, ClothingCategory.BOTTOM, ClothingColor.BLACK, ClothingMaterial.DENIM, 0, 30, false);
        OutfitCandidate candidate = candidate(top, bottom);
        WeatherCondition weather = WeatherCondition.of(20, WeatherType.CLOUDY, false, false);
        RecommendationScore score = scorer.score(
                candidate,
                weather,
                List.of(),
                List.of(feedbackHistory(
                        1,
                        requestedAt.minusDays(2),
                        20,
                        RecommendationFeedbackSentiment.DISLIKED,
                        null,
                        requestedAt.minusDays(1),
                        top,
                        bottom
                )),
                requestedAt
        );

        List<String> reasons = generator.generate(
                candidate,
                score,
                weather,
                List.of(),
                List.of(
                        feedbackHistory(
                                1,
                                requestedAt.minusDays(2),
                                20,
                                RecommendationFeedbackSentiment.LIKED,
                                null,
                                requestedAt.minusDays(1),
                                top,
                                bottom
                        ),
                        feedbackHistory(
                                2,
                                requestedAt.minusDays(2),
                                20,
                                RecommendationFeedbackSentiment.DISLIKED,
                                null,
                                requestedAt.minusHours(12),
                                top,
                                bottom
                        )
                ),
                requestedAt
        );

        assertThat(reasons).contains("최근 별로였거나 온도가 맞지 않았던 피드백을 피해 점수에 반영했어요.");
        assertThat(reasons).doesNotContain("최근 마음에 든 조합과 일부 겹쳐 선호를 반영했어요.");
    }
}

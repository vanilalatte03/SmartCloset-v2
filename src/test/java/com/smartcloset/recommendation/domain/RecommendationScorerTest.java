package com.smartcloset.recommendation.domain;

import static com.smartcloset.recommendation.domain.RecommendationDomainTestFixtures.candidate;
import static com.smartcloset.recommendation.domain.RecommendationDomainTestFixtures.clothing;
import static com.smartcloset.recommendation.domain.RecommendationDomainTestFixtures.recommendation;
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
import org.junit.jupiter.api.Test;

class RecommendationScorerTest {

    private final RecommendationScorer scorer = new RecommendationScorer();
    private final LocalDateTime requestedAt = LocalDateTime.of(2026, 5, 21, 12, 0);

    @Test
    void calculatesWeatherScoreWithTemperatureOuterRainAndColdMaterials() {
        User user = user(1);
        OutfitCandidate candidate = candidate(
                clothing(1, user, ClothingCategory.TOP, ClothingColor.WHITE, ClothingMaterial.KNIT, 0, 16, false),
                clothing(2, user, ClothingCategory.BOTTOM, ClothingColor.BLACK, ClothingMaterial.DENIM, 0, 20, false),
                clothing(3, user, ClothingCategory.OUTER, ClothingColor.NAVY, ClothingMaterial.WOOL, -10, 12, false)
        );

        int weatherScore = scorer.calculateWeatherScore(
                candidate,
                WeatherCondition.of(12, WeatherType.CLOUDY, false, false)
        );

        assertThat(weatherScore).isEqualTo(34);
    }

    @Test
    void doesNotAdjustMaterialWeatherScoreForUnknownMaterial() {
        User user = user(1);
        OutfitCandidate candidate = candidate(
                clothing(1, user, ClothingCategory.TOP, ClothingColor.WHITE, ClothingMaterial.UNKNOWN, 0, 16, false),
                clothing(2, user, ClothingCategory.BOTTOM, ClothingColor.BLACK, ClothingMaterial.COTTON, 0, 20, false),
                clothing(3, user, ClothingCategory.OUTER, ClothingColor.NAVY, ClothingMaterial.POLYESTER, -10, 12, false)
        );

        int weatherScore = scorer.calculateWeatherScore(
                candidate,
                WeatherCondition.of(12, WeatherType.CLOUDY, false, false)
        );

        assertThat(weatherScore).isEqualTo(32);
    }

    @Test
    void appliesHotWeatherAndRainyMaterialRules() {
        User user = user(1);
        OutfitCandidate hotKnitWool = candidate(
                clothing(1, user, ClothingCategory.TOP, ClothingColor.WHITE, ClothingMaterial.KNIT, 0, 30, false),
                clothing(2, user, ClothingCategory.BOTTOM, ClothingColor.BLACK, ClothingMaterial.WOOL, 0, 30, false)
        );
        OutfitCandidate rainyNylon = candidate(
                clothing(3, user, ClothingCategory.TOP, ClothingColor.WHITE, ClothingMaterial.NYLON, 0, 30, true),
                clothing(4, user, ClothingCategory.BOTTOM, ClothingColor.BLACK, ClothingMaterial.COTTON, 0, 30, true)
        );
        OutfitCandidate rainyWool = candidate(
                clothing(5, user, ClothingCategory.TOP, ClothingColor.WHITE, ClothingMaterial.WOOL, 0, 30, false),
                clothing(6, user, ClothingCategory.BOTTOM, ClothingColor.BLACK, ClothingMaterial.COTTON, 0, 30, false)
        );

        assertThat(scorer.calculateWeatherScore(hotKnitWool, WeatherCondition.of(25, WeatherType.SUNNY, false, false)))
                .isEqualTo(28);
        assertThat(scorer.calculateWeatherScore(rainyNylon, WeatherCondition.of(20, WeatherType.RAINY, true, false)))
                .isEqualTo(33);
        assertThat(scorer.calculateWeatherScore(rainyWool, WeatherCondition.of(20, WeatherType.RAINY, true, false)))
                .isEqualTo(24);
    }

    @Test
    void calculatesColorScoreByDocumentedPairRules() {
        assertThat(colorScore(ClothingColor.BLACK, ClothingColor.WHITE)).isEqualTo(24);
        assertThat(colorScore(ClothingColor.GRAY, ClothingColor.RED)).isEqualTo(25);
        assertThat(colorScore(ClothingColor.NAVY, ClothingColor.BEIGE)).isEqualTo(22);
        assertThat(colorScore(ClothingColor.BLUE, ClothingColor.NAVY)).isEqualTo(20);
        assertThat(colorScore(ClothingColor.RED, ClothingColor.GREEN)).isEqualTo(17);
        assertThat(colorScore(ClothingColor.UNKNOWN, ClothingColor.BLACK)).isEqualTo(15);
        assertThat(colorScore(ClothingColor.RED, ClothingColor.YELLOW)).isEqualTo(10);
    }

    @Test
    void appliesRecentWearRecommendationAndDiversityHistoryScores() {
        User user = user(1);
        ClothingItem top = clothing(1, user, ClothingCategory.TOP, ClothingColor.WHITE, ClothingMaterial.COTTON, 0, 30, false);
        ClothingItem bottom = clothing(2, user, ClothingCategory.BOTTOM, ClothingColor.BLACK, ClothingMaterial.DENIM, 0, 30, false);
        OutfitCandidate candidate = candidate(top, bottom);
        RecommendationResult sameRecentRecommendation = recommendation(1, user, requestedAt.minusDays(2), top, bottom);
        WearHistory wearHistory = wearHistory(1, user, sameRecentRecommendation, requestedAt.minusDays(2));

        RecommendationScore score = scorer.score(
                candidate,
                WeatherCondition.of(20, WeatherType.CLOUDY, false, false),
                List.of(wearHistory),
                List.of(sameRecentRecommendation),
                requestedAt
        );

        assertThat(score.wearHistoryScore()).isEqualTo(10);
        assertThat(score.recommendationHistoryScore()).isEqualTo(2);
        assertThat(score.diversityScore()).isZero();
    }

    @Test
    void givesPartialRecentRecommendationPenaltyWeakerThanSameCombinationPenalty() {
        User user = user(1);
        ClothingItem top = clothing(1, user, ClothingCategory.TOP, ClothingColor.WHITE, ClothingMaterial.COTTON, 0, 30, false);
        ClothingItem bottom = clothing(2, user, ClothingCategory.BOTTOM, ClothingColor.BLACK, ClothingMaterial.DENIM, 0, 30, false);
        ClothingItem otherBottom = clothing(3, user, ClothingCategory.BOTTOM, ClothingColor.BLUE, ClothingMaterial.DENIM, 0, 30, false);
        OutfitCandidate candidate = candidate(top, bottom);
        RecommendationResult partialRecentRecommendation = recommendation(
                1,
                user,
                requestedAt.minusDays(2),
                top,
                otherBottom
        );

        int recommendationHistoryScore = scorer.calculateRecommendationHistoryScore(
                candidate,
                List.of(partialRecentRecommendation),
                requestedAt
        );

        assertThat(recommendationHistoryScore).isEqualTo(7);
    }

    @Test
    void selectsBestCandidateByScoreTieBreakAndGenerationOrder() {
        User user = user(1);
        ClothingItem top = clothing(1, user, ClothingCategory.TOP, ClothingColor.WHITE, ClothingMaterial.COTTON, 0, 30, false);
        ClothingItem bottom = clothing(2, user, ClothingCategory.BOTTOM, ClothingColor.BLACK, ClothingMaterial.DENIM, 0, 30, false);
        ScoredOutfitCandidate first = new ScoredOutfitCandidate(
                OutfitCandidate.withoutOuter(top, bottom, 0),
                RecommendationScore.of(80, 30, 20, 20, 10, 0)
        );
        ScoredOutfitCandidate second = new ScoredOutfitCandidate(
                OutfitCandidate.withoutOuter(top, bottom, 1),
                RecommendationScore.of(80, 30, 20, 20, 10, 0)
        );

        assertThat(scorer.selectBest(List.of(second, first))).isSameAs(first);
    }

    @Test
    void returnsSameBestRecommendationForSameInput() {
        User user = user(1);
        List<ClothingItem> clothes = List.of(
                clothing(2, user, ClothingCategory.BOTTOM, ClothingColor.BLACK, ClothingMaterial.DENIM, 0, 22, false),
                clothing(4, user, ClothingCategory.OUTER, ClothingColor.BLACK, ClothingMaterial.NYLON, 5, 18, true),
                clothing(1, user, ClothingCategory.TOP, ClothingColor.WHITE, ClothingMaterial.KNIT, 0, 16, false),
                clothing(3, user, ClothingCategory.OUTER, ClothingColor.NAVY, ClothingMaterial.WOOL, -10, 12, false)
        );
        WeatherCondition weather = WeatherCondition.of(12, WeatherType.CLOUDY, false, false);
        WeatherSuitabilityFilter filter = new WeatherSuitabilityFilter();
        OutfitCandidateGenerator generator = new OutfitCandidateGenerator();

        ScoredOutfitCandidate first = scorer.selectBest(scorer.scoreAll(
                generator.generate(filter.filter(clothes, weather), weather),
                weather,
                List.of(),
                List.of(),
                requestedAt
        ));
        ScoredOutfitCandidate second = scorer.selectBest(scorer.scoreAll(
                generator.generate(filter.filter(clothes, weather), weather),
                weather,
                List.of(),
                List.of(),
                requestedAt
        ));

        assertThat(first.candidate().top().getId()).isEqualTo(second.candidate().top().getId());
        assertThat(first.candidate().bottom().getId()).isEqualTo(second.candidate().bottom().getId());
        assertThat(first.candidate().outer().getId()).isEqualTo(second.candidate().outer().getId());
        assertThat(first.score()).isEqualTo(second.score());
    }

    private int colorScore(ClothingColor topColor, ClothingColor bottomColor) {
        User user = user(1);
        return scorer.calculateColorScore(candidate(
                clothing(1, user, ClothingCategory.TOP, topColor, ClothingMaterial.COTTON, 0, 30, false),
                clothing(2, user, ClothingCategory.BOTTOM, bottomColor, ClothingMaterial.DENIM, 0, 30, false)
        ));
    }
}

package com.smartcloset.recommendation.domain;

import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.domain.ClothingMaterial;
import com.smartcloset.weather.domain.WeatherCondition;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class RecommendationScorer {

    private static final int MAX_WEATHER_SCORE = 35;
    private static final int MAX_COLOR_SCORE = 25;
    private static final int MAX_WEAR_HISTORY_SCORE = 20;
    private static final int MAX_RECOMMENDATION_HISTORY_SCORE = 10;
    private static final int MAX_PREFERENCE_SCORE = 10;

    private static final Comparator<ScoredOutfitCandidate> BEST_CANDIDATE_COMPARATOR =
            Comparator.comparingInt((ScoredOutfitCandidate candidate) -> candidate.score().totalScore()).reversed()
                    .thenComparing(Comparator.comparingInt(
                            (ScoredOutfitCandidate candidate) -> candidate.score().weatherScore()).reversed())
                    .thenComparing(Comparator.comparingInt(
                            (ScoredOutfitCandidate candidate) -> candidate.score().preferenceScore()).reversed())
                    .thenComparing(Comparator.comparingInt(
                            (ScoredOutfitCandidate candidate) -> candidate.score().colorScore()).reversed())
                    .thenComparing(Comparator.comparingInt(
                            (ScoredOutfitCandidate candidate) -> candidate.score().wearHistoryScore()).reversed())
                    .thenComparing(Comparator.comparingInt(
                            (ScoredOutfitCandidate candidate) -> candidate.score().recommendationHistoryScore()).reversed())
                    .thenComparing(candidate -> candidate.candidate().top().getId(), Comparator.nullsLast(Long::compareTo))
                    .thenComparing(candidate -> candidate.candidate().bottom().getId(), Comparator.nullsLast(Long::compareTo))
                    .thenComparing(candidate -> candidate.candidate().hasOuter()
                            ? candidate.candidate().outer().getId()
                            : null, Comparator.nullsLast(Long::compareTo))
                    .thenComparingInt(candidate -> candidate.candidate().generationOrder());

    public List<ScoredOutfitCandidate> scoreAll(
            List<OutfitCandidate> candidates,
            WeatherCondition weather,
            List<WearHistory> wearHistories,
            List<RecommendationResult> recommendationHistories,
            LocalDateTime requestedAt,
            RecommendationPreferences preferences
    ) {
        Objects.requireNonNull(candidates, "candidates must not be null");
        Objects.requireNonNull(preferences, "preferences must not be null");
        return candidates.stream()
                .map(candidate -> new ScoredOutfitCandidate(
                        candidate,
                        score(candidate, weather, wearHistories, recommendationHistories, requestedAt, preferences)
                ))
                .toList();
    }

    public List<ScoredOutfitCandidate> scoreAll(
            List<OutfitCandidate> candidates,
            WeatherCondition weather,
            List<WearHistory> wearHistories,
            List<RecommendationResult> recommendationHistories,
            LocalDateTime requestedAt
    ) {
        return scoreAll(
                candidates,
                weather,
                wearHistories,
                recommendationHistories,
                requestedAt,
                RecommendationPreferences.empty()
        );
    }

    public RecommendationScore score(
            OutfitCandidate candidate,
            WeatherCondition weather,
            List<WearHistory> wearHistories,
            List<RecommendationResult> recommendationHistories,
            LocalDateTime requestedAt,
            RecommendationPreferences preferences
    ) {
        Objects.requireNonNull(candidate, "candidate must not be null");
        Objects.requireNonNull(weather, "weather must not be null");
        Objects.requireNonNull(wearHistories, "wearHistories must not be null");
        Objects.requireNonNull(recommendationHistories, "recommendationHistories must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        Objects.requireNonNull(preferences, "preferences must not be null");

        int weatherScore = calculateWeatherScore(candidate, weather);
        int colorScore = calculateColorScore(candidate);
        int wearHistoryScore = calculateWearHistoryScore(candidate, wearHistories, requestedAt);
        int recommendationHistoryScore = calculateRecommendationHistoryScore(
                candidate,
                recommendationHistories,
                requestedAt
        );
        int preferenceScore = calculatePreferenceScore(candidate, preferences);
        int totalScore = weatherScore + colorScore + wearHistoryScore + recommendationHistoryScore + preferenceScore;

        return RecommendationScore.of(
                totalScore,
                weatherScore,
                colorScore,
                wearHistoryScore,
                recommendationHistoryScore,
                preferenceScore
        );
    }

    public RecommendationScore score(
            OutfitCandidate candidate,
            WeatherCondition weather,
            List<WearHistory> wearHistories,
            List<RecommendationResult> recommendationHistories,
            LocalDateTime requestedAt
    ) {
        return score(
                candidate,
                weather,
                wearHistories,
                recommendationHistories,
                requestedAt,
                RecommendationPreferences.empty()
        );
    }

    public ScoredOutfitCandidate selectBest(List<ScoredOutfitCandidate> candidates) {
        Objects.requireNonNull(candidates, "candidates must not be null");
        return candidates.stream()
                .min(BEST_CANDIDATE_COMPARATOR)
                .orElseThrow(() -> new RecommendationFailureException(RecommendationFailureCode.INSUFFICIENT_CLOSET_ITEMS));
    }

    int calculateWeatherScore(OutfitCandidate candidate, WeatherCondition weather) {
        int temperatureRangeScore = 15;
        int outerScore = calculateOuterScore(candidate, weather);
        int rainScore = calculateRainScore(candidate, weather);
        int materialWeatherScore = calculateMaterialWeatherScore(candidate, weather);
        return clamp(
                temperatureRangeScore + outerScore + rainScore + materialWeatherScore,
                0,
                MAX_WEATHER_SCORE
        );
    }

    int calculateColorScore(OutfitCandidate candidate) {
        if (!candidate.hasOuter()) {
            return calculatePairColorScore(candidate.top().getColor(), candidate.bottom().getColor());
        }

        int topBottom = calculatePairColorScore(candidate.top().getColor(), candidate.bottom().getColor());
        int topOuter = calculatePairColorScore(candidate.top().getColor(), candidate.outer().getColor());
        int bottomOuter = calculatePairColorScore(candidate.bottom().getColor(), candidate.outer().getColor());
        return clamp(Math.round((topBottom + topOuter + bottomOuter) / 3.0f), 0, MAX_COLOR_SCORE);
    }

    int calculateWearHistoryScore(
            OutfitCandidate candidate,
            List<WearHistory> wearHistories,
            LocalDateTime requestedAt
    ) {
        return wearHistories.stream()
                .filter(history -> history.getRecommendationResult() != null)
                .filter(history -> candidate.intersects(itemIds(history.getRecommendationResult())))
                .map(WearHistory::getWornAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .map(wornAt -> scoreRecentWornAt(wornAt, requestedAt))
                .orElse(MAX_WEAR_HISTORY_SCORE);
    }

    int calculateRecommendationHistoryScore(
            OutfitCandidate candidate,
            List<RecommendationResult> recommendationHistories,
            LocalDateTime requestedAt
    ) {
        int score = MAX_RECOMMENDATION_HISTORY_SCORE;
        for (RecommendationResult history : recommendationHistories) {
            LocalDateTime createdAt = history.getCreatedAt();
            if (createdAt == null || createdAt.isBefore(requestedAt.minusDays(7))) {
                continue;
            }

            Set<Long> historyItemIds = itemIds(history);
            if (candidate.hasSameItemSet(historyItemIds)) {
                if (!createdAt.isBefore(requestedAt.minusDays(3))) {
                    score = Math.min(score, 2);
                } else {
                    score = Math.min(score, 5);
                }
            } else if (candidate.intersects(historyItemIds)) {
                if (!createdAt.isBefore(requestedAt.minusDays(3))) {
                    score = Math.min(score, 7);
                } else {
                    score = Math.min(score, 8);
                }
            }
        }
        return score;
    }

    int calculatePreferenceScore(OutfitCandidate candidate, RecommendationPreferences preferences) {
        int score = 0;
        if (!preferences.preferredColors().isEmpty()
                && candidate.stream().anyMatch(item -> preferences.preferredColors().contains(item.getColor()))) {
            score += 5;
        }
        if (!preferences.preferredMaterials().isEmpty()
                && candidate.stream().anyMatch(item -> preferences.preferredMaterials().contains(item.getMaterial()))) {
            score += 5;
        }
        return clamp(score, 0, MAX_PREFERENCE_SCORE);
    }

    private int calculateOuterScore(OutfitCandidate candidate, WeatherCondition weather) {
        int score;
        if (weather.temperature() <= 12) {
            score = candidate.hasOuter() ? 8 : 0;
        } else if (weather.temperature() <= 16) {
            score = candidate.hasOuter() ? 7 : 5;
        } else {
            score = candidate.hasOuter() ? 5 : 7;
        }

        if ((weather.rainy() || weather.windy()) && candidate.hasOuter()) {
            score += 1;
        }
        return clamp(score, 0, 8);
    }

    private int calculateRainScore(OutfitCandidate candidate, WeatherCondition weather) {
        if (!weather.rainy()) {
            return 6;
        }

        long rainSuitableCount = candidate.stream().filter(ClothingItem::isRainSuitable).count();
        if (rainSuitableCount == candidate.items().size()) {
            return 6;
        }
        if (rainSuitableCount > 0) {
            return 3;
        }
        return 1;
    }

    private int calculateMaterialWeatherScore(OutfitCandidate candidate, WeatherCondition weather) {
        int score = 3;

        if (weather.temperature() <= 12) {
            long warmMaterialCount = candidate.stream()
                    .filter(item -> item.getMaterial() == ClothingMaterial.KNIT
                            || item.getMaterial() == ClothingMaterial.WOOL)
                    .count();
            score += (int) Math.min(warmMaterialCount, 3);
        }

        if (weather.temperature() >= 25) {
            long hotPenaltyCount = candidate.stream()
                    .filter(item -> item.getMaterial() == ClothingMaterial.KNIT
                            || item.getMaterial() == ClothingMaterial.WOOL)
                    .count();
            score -= (int) hotPenaltyCount * 2;
        }

        if (weather.rainy()) {
            long nylonCount = candidate.stream()
                    .filter(item -> item.getMaterial() == ClothingMaterial.NYLON)
                    .count();
            long woolCount = candidate.stream()
                    .filter(item -> item.getMaterial() == ClothingMaterial.WOOL)
                    .count();
            score += Math.min(nylonCount * 2, 2);
            score -= woolCount * 2;
        }

        return clamp(score, 0, 6);
    }

    private int calculatePairColorScore(ClothingColor left, ClothingColor right) {
        if (left == ClothingColor.UNKNOWN || right == ClothingColor.UNKNOWN) {
            return 15;
        }
        if (isStrongClash(left, right)) {
            return 10;
        }
        if (isComplementary(left, right)) {
            return 17;
        }

        ColorGroup leftGroup = ColorGroup.from(left);
        ColorGroup rightGroup = ColorGroup.from(right);
        if (leftGroup == ColorGroup.NEUTRAL && rightGroup == ColorGroup.NEUTRAL) {
            return 24;
        }
        if (leftGroup == ColorGroup.NEUTRAL || rightGroup == ColorGroup.NEUTRAL) {
            return 25;
        }
        if (isBlueEarthPair(leftGroup, rightGroup)) {
            return 22;
        }
        if (leftGroup == rightGroup) {
            return 20;
        }
        return 15;
    }

    private int scoreRecentWornAt(LocalDateTime wornAt, LocalDateTime requestedAt) {
        if (!wornAt.isBefore(requestedAt.minusDays(1))) {
            return 5;
        }
        if (!wornAt.isBefore(requestedAt.minusDays(3))) {
            return 10;
        }
        if (!wornAt.isBefore(requestedAt.minusDays(7))) {
            return 15;
        }
        return MAX_WEAR_HISTORY_SCORE;
    }

    private boolean isBlueEarthPair(ColorGroup left, ColorGroup right) {
        return (left == ColorGroup.BLUE && right == ColorGroup.EARTH)
                || (left == ColorGroup.EARTH && right == ColorGroup.BLUE);
    }

    private boolean isComplementary(ClothingColor left, ClothingColor right) {
        return pairMatches(left, right, ClothingColor.RED, ClothingColor.GREEN)
                || pairMatches(left, right, ClothingColor.BLUE, ClothingColor.YELLOW)
                || pairMatches(left, right, ClothingColor.NAVY, ClothingColor.YELLOW);
    }

    private boolean isStrongClash(ClothingColor left, ClothingColor right) {
        return pairMatches(left, right, ClothingColor.RED, ClothingColor.YELLOW)
                || pairMatches(left, right, ClothingColor.GREEN, ClothingColor.YELLOW);
    }

    private boolean pairMatches(ClothingColor left, ClothingColor right, ClothingColor first, ClothingColor second) {
        return (left == first && right == second) || (left == second && right == first);
    }

    private Set<Long> itemIds(RecommendationResult recommendationResult) {
        return recommendationResult.getItems().stream()
                .map(RecommendationResultItem::getClothingItem)
                .map(ClothingItem::getId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private int clamp(int score, int min, int max) {
        return Math.max(min, Math.min(score, max));
    }

    private enum ColorGroup {
        NEUTRAL(EnumSet.of(ClothingColor.BLACK, ClothingColor.WHITE, ClothingColor.GRAY)),
        BLUE(EnumSet.of(ClothingColor.NAVY, ClothingColor.BLUE)),
        EARTH(EnumSet.of(ClothingColor.BROWN, ClothingColor.BEIGE)),
        ACCENT(EnumSet.of(ClothingColor.RED, ClothingColor.GREEN, ClothingColor.YELLOW));

        private final Set<ClothingColor> colors;

        ColorGroup(Set<ClothingColor> colors) {
            this.colors = colors;
        }

        private static ColorGroup from(ClothingColor color) {
            for (ColorGroup group : values()) {
                if (group.colors.contains(color)) {
                    return group;
                }
            }
            throw new IllegalArgumentException("unsupported color: " + color);
        }
    }
}

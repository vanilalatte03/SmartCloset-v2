package com.smartcloset.recommendation.domain;

import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.domain.ClothingMaterial;
import com.smartcloset.weather.domain.WeatherType;
import com.smartcloset.weather.domain.WeatherCondition;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class RecommendationScorer {

    private static final int MAX_WEATHER_SCORE = 35;
    private static final int MAX_COLOR_SCORE = 25;
    private static final int MAX_WEAR_HISTORY_SCORE = 20;
    private static final int MAX_RECOMMENDATION_HISTORY_SCORE = 10;
    private static final int MAX_PREFERENCE_SCORE = 10;
    private static final Comparator<Long> ID_ASC = Comparator.nullsLast(Long::compareTo);

    public List<ScoredOutfitCandidate> scoreAll(
            List<OutfitCandidate> candidates,
            WeatherCondition weather,
            List<WearHistorySnapshot> wearHistories,
            List<RecommendationHistorySnapshot> recommendationHistories,
            LocalDateTime requestedAt,
            List<ClothingColor> preferredColors,
            List<ClothingMaterial> preferredMaterials,
            List<String> preferredStyleTags,
            RecommendationSituation situation
    ) {
        Objects.requireNonNull(candidates, "candidates must not be null");
        return candidates.stream()
                .map(candidate -> new ScoredOutfitCandidate(
                        candidate,
                        score(
                                candidate,
                                weather,
                                wearHistories,
                                recommendationHistories,
                                requestedAt,
                                preferredColors,
                                preferredMaterials,
                                preferredStyleTags,
                                situation
                        )
                ))
                .toList();
    }

    public List<ScoredOutfitCandidate> scoreAll(
            List<OutfitCandidate> candidates,
            WeatherCondition weather,
            List<WearHistorySnapshot> wearHistories,
            List<RecommendationHistorySnapshot> recommendationHistories,
            LocalDateTime requestedAt,
            List<ClothingColor> preferredColors,
            List<ClothingMaterial> preferredMaterials
    ) {
        return scoreAll(
                candidates,
                weather,
                wearHistories,
                recommendationHistories,
                requestedAt,
                preferredColors,
                preferredMaterials,
                List.of(),
                RecommendationSituation.CASUAL
        );
    }

    public List<ScoredOutfitCandidate> scoreAll(
            List<OutfitCandidate> candidates,
            WeatherCondition weather,
            List<WearHistorySnapshot> wearHistories,
            List<RecommendationHistorySnapshot> recommendationHistories,
            LocalDateTime requestedAt
    ) {
        return scoreAll(candidates, weather, wearHistories, recommendationHistories, requestedAt, List.of(), List.of());
    }

    public RecommendationScore score(
            OutfitCandidate candidate,
            WeatherCondition weather,
            List<WearHistorySnapshot> wearHistories,
            List<RecommendationHistorySnapshot> recommendationHistories,
            LocalDateTime requestedAt,
            List<ClothingColor> preferredColors,
            List<ClothingMaterial> preferredMaterials,
            List<String> preferredStyleTags,
            RecommendationSituation situation
    ) {
        Objects.requireNonNull(candidate, "candidate must not be null");
        Objects.requireNonNull(weather, "weather must not be null");
        Objects.requireNonNull(wearHistories, "wearHistories must not be null");
        Objects.requireNonNull(recommendationHistories, "recommendationHistories must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        Objects.requireNonNull(preferredColors, "preferredColors must not be null");
        Objects.requireNonNull(preferredMaterials, "preferredMaterials must not be null");
        Objects.requireNonNull(preferredStyleTags, "preferredStyleTags must not be null");

        int weatherScore = calculateWeatherScore(candidate, weather);
        int colorScore = calculateColorScore(candidate);
        int wearHistoryScore = calculateWearHistoryScore(candidate, wearHistories, requestedAt);
        int recommendationHistoryScore = calculateRecommendationHistoryScore(
                candidate,
                recommendationHistories,
                requestedAt
        );
        int preferenceScore = calculatePreferenceScore(
                candidate,
                preferredColors,
                preferredMaterials,
                preferredStyleTags,
                situation,
                recommendationHistories,
                requestedAt,
                weather
        );
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
            List<WearHistorySnapshot> wearHistories,
            List<RecommendationHistorySnapshot> recommendationHistories,
            LocalDateTime requestedAt,
            List<ClothingColor> preferredColors,
            List<ClothingMaterial> preferredMaterials
    ) {
        return score(
                candidate,
                weather,
                wearHistories,
                recommendationHistories,
                requestedAt,
                preferredColors,
                preferredMaterials,
                List.of(),
                RecommendationSituation.CASUAL
        );
    }

    public RecommendationScore score(
            OutfitCandidate candidate,
            WeatherCondition weather,
            List<WearHistorySnapshot> wearHistories,
            List<RecommendationHistorySnapshot> recommendationHistories,
            LocalDateTime requestedAt
    ) {
        return score(candidate, weather, wearHistories, recommendationHistories, requestedAt, List.of(), List.of());
    }

    public ScoredOutfitCandidate selectBest(List<ScoredOutfitCandidate> candidates) {
        return selectBest(candidates, null);
    }

    public ScoredOutfitCandidate selectBest(List<ScoredOutfitCandidate> candidates, WeatherCondition weather) {
        Objects.requireNonNull(candidates, "candidates must not be null");
        return candidates.stream()
                .min((left, right) -> compareBest(left, right, weather))
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
            List<WearHistorySnapshot> wearHistories,
            LocalDateTime requestedAt
    ) {
        return wearHistories.stream()
                .filter(history -> candidate.intersects(history.clothingItemIds()))
                .map(WearHistorySnapshot::wornAt)
                .max(LocalDateTime::compareTo)
                .map(wornAt -> scoreRecentWornAt(wornAt, requestedAt))
                .orElse(MAX_WEAR_HISTORY_SCORE);
    }

    int calculateRecommendationHistoryScore(
            OutfitCandidate candidate,
            List<RecommendationHistorySnapshot> recommendationHistories,
            LocalDateTime requestedAt
    ) {
        int score = MAX_RECOMMENDATION_HISTORY_SCORE;
        for (RecommendationHistorySnapshot history : recommendationHistories) {
            LocalDateTime createdAt = history.createdAt();
            if (createdAt == null || createdAt.isBefore(requestedAt.minusDays(7))) {
                continue;
            }

            Set<Long> historyItemIds = history.clothingItemIds();
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

    int calculatePreferenceScore(
            OutfitCandidate candidate,
            List<ClothingColor> preferredColors,
            List<ClothingMaterial> preferredMaterials
    ) {
        return calculatePreferenceScore(
                candidate,
                preferredColors,
                preferredMaterials,
                List.of(),
                RecommendationSituation.CASUAL,
                List.of(),
                LocalDateTime.now(),
                WeatherCondition.of(12, WeatherType.CLOUDY, false, false)
        );
    }

    int calculatePreferenceScore(
            OutfitCandidate candidate,
            List<ClothingColor> preferredColors,
            List<ClothingMaterial> preferredMaterials,
            List<String> preferredStyleTags,
            RecommendationSituation situation,
            List<RecommendationHistorySnapshot> recommendationHistories,
            LocalDateTime requestedAt,
            WeatherCondition weather
    ) {
        Objects.requireNonNull(candidate, "candidate must not be null");
        Objects.requireNonNull(preferredColors, "preferredColors must not be null");
        Objects.requireNonNull(preferredMaterials, "preferredMaterials must not be null");
        Objects.requireNonNull(preferredStyleTags, "preferredStyleTags must not be null");
        Objects.requireNonNull(recommendationHistories, "recommendationHistories must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        Objects.requireNonNull(weather, "weather must not be null");

        int score = 0;
        if (!preferredColors.isEmpty()
                && candidate.stream().map(ClothingItem::getColor).anyMatch(preferredColors::contains)) {
            score += 2;
        }
        if (!preferredMaterials.isEmpty()
                && candidate.stream().map(ClothingItem::getMaterial).anyMatch(preferredMaterials::contains)) {
            score += 2;
        }
        score += calculateStyleTagScore(candidate, preferredStyleTags, situation);
        score += calculateFeedbackAdjustment(candidate, recommendationHistories, requestedAt, weather);
        return clamp(score, 0, MAX_PREFERENCE_SCORE);
    }

    int calculateStyleTagScore(
            OutfitCandidate candidate,
            List<String> preferredStyleTags,
            RecommendationSituation situation
    ) {
        Set<String> candidateTags = candidateStyleTagKeys(candidate);
        int score = 0;
        if (!candidateTags.isEmpty() && intersects(candidateTags, styleTagKeys(preferredStyleTags))) {
            score += 2;
        }
        if (!candidateTags.isEmpty() && intersects(candidateTags, situationStyleTagKeys(situation))) {
            score += 1;
        }
        return score;
    }

    int calculateFeedbackAdjustment(
            OutfitCandidate candidate,
            List<RecommendationHistorySnapshot> recommendationHistories,
            LocalDateTime requestedAt,
            WeatherCondition weather
    ) {
        int positiveAdjustment = 0;
        int negativeAdjustment = 0;
        LocalDateTime feedbackWindowStart = requestedAt.minusDays(14);

        for (RecommendationHistorySnapshot history : recommendationHistories) {
            if (!history.hasFeedback() || history.feedbackUpdatedAt().isBefore(feedbackWindowStart)) {
                continue;
            }

            Overlap overlap = overlap(candidate, history.clothingItemIds());
            if (overlap == Overlap.NONE) {
                continue;
            }

            if (history.sentimentFeedback() == RecommendationFeedbackSentiment.DISLIKED) {
                negativeAdjustment = Math.min(negativeAdjustment, overlap == Overlap.SAME ? -3 : -1);
            } else if (history.sentimentFeedback() == RecommendationFeedbackSentiment.LIKED) {
                positiveAdjustment = Math.max(positiveAdjustment, overlap == Overlap.SAME ? 3 : 1);
            }

            if (history.thermalFeedback() == RecommendationThermalFeedback.TOO_COLD
                    && weather.temperature() <= history.weatherTemperature() + 3) {
                negativeAdjustment = Math.min(negativeAdjustment, overlap == Overlap.SAME ? -2 : -1);
            }
            if (history.thermalFeedback() == RecommendationThermalFeedback.TOO_HOT
                    && weather.temperature() >= history.weatherTemperature() - 3) {
                negativeAdjustment = Math.min(negativeAdjustment, overlap == Overlap.SAME ? -2 : -1);
            }
        }

        if (negativeAdjustment < 0) {
            return clamp(negativeAdjustment, -3, 3);
        }
        return clamp(positiveAdjustment, -3, 3);
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

    private int compareBest(ScoredOutfitCandidate left, ScoredOutfitCandidate right, WeatherCondition weather) {
        int result = compareHigher(left.score().totalScore(), right.score().totalScore());
        if (result != 0) {
            return result;
        }
        result = compareHigher(left.score().weatherScore(), right.score().weatherScore());
        if (result != 0) {
            return result;
        }
        result = compareHigher(left.score().preferenceScore(), right.score().preferenceScore());
        if (result != 0) {
            return result;
        }
        result = compareHigher(left.score().colorScore(), right.score().colorScore());
        if (result != 0) {
            return result;
        }
        result = compareHigher(left.score().wearHistoryScore(), right.score().wearHistoryScore());
        if (result != 0) {
            return result;
        }
        result = compareHigher(
                left.score().recommendationHistoryScore(),
                right.score().recommendationHistoryScore()
        );
        if (result != 0) {
            return result;
        }
        result = ID_ASC.compare(left.candidate().top().getId(), right.candidate().top().getId());
        if (result != 0) {
            return result;
        }
        result = ID_ASC.compare(left.candidate().bottom().getId(), right.candidate().bottom().getId());
        if (result != 0) {
            return result;
        }
        result = compareOuter(left.candidate(), right.candidate(), weather);
        if (result != 0) {
            return result;
        }
        return Integer.compare(left.candidate().generationOrder(), right.candidate().generationOrder());
    }

    private int compareHigher(int left, int right) {
        return Integer.compare(right, left);
    }

    private int compareOuter(OutfitCandidate left, OutfitCandidate right, WeatherCondition weather) {
        if (left.hasOuter() && right.hasOuter()) {
            return ID_ASC.compare(left.outer().getId(), right.outer().getId());
        }
        if (left.hasOuter() == right.hasOuter()) {
            return 0;
        }
        boolean preferOuter = weather != null && weather.temperature() <= 16;
        if (left.hasOuter() == preferOuter) {
            return -1;
        }
        if (right.hasOuter() == preferOuter) {
            return 1;
        }
        return Boolean.compare(left.hasOuter(), right.hasOuter());
    }

    private int clamp(int score, int min, int max) {
        return Math.max(min, Math.min(score, max));
    }

    private Overlap overlap(OutfitCandidate candidate, Set<Long> historyItemIds) {
        if (candidate.hasSameItemSet(historyItemIds)) {
            return Overlap.SAME;
        }
        if (candidate.intersects(historyItemIds)) {
            return Overlap.PARTIAL;
        }
        return Overlap.NONE;
    }

    private Set<String> candidateStyleTagKeys(OutfitCandidate candidate) {
        return candidate.stream()
                .flatMap(item -> readStyleTags(item.getStyleTagsJson()).stream())
                .map(this::styleTagKey)
                .filter(key -> !key.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    private Set<String> styleTagKeys(List<String> styleTags) {
        return styleTags.stream()
                .map(this::styleTagKey)
                .filter(key -> !key.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    private Set<String> situationStyleTagKeys(RecommendationSituation situation) {
        RecommendationSituation resolvedSituation = situation == null ? RecommendationSituation.CASUAL : situation;
        return switch (resolvedSituation) {
            case WORK -> styleTagKeys(List.of("WORK", "OFFICE", "MINIMAL", "SMART", "출근", "오피스", "미니멀", "단정"));
            case CASUAL -> styleTagKeys(List.of("CASUAL", "DAILY", "COMFORT", "MINIMAL", "캐주얼", "데일리", "편안함", "미니멀"));
            case WORKOUT -> styleTagKeys(List.of("WORKOUT", "SPORTY", "ACTIVE", "COMFORT", "운동", "스포티", "활동적", "편안함"));
            case DATE -> styleTagKeys(List.of("DATE", "NEAT", "POINT", "MINIMAL", "데이트", "깔끔", "포인트", "미니멀"));
            case FORMAL -> styleTagKeys(List.of("FORMAL", "OFFICIAL", "SMART", "MINIMAL", "격식", "포멀", "단정", "미니멀"));
        };
    }

    private boolean intersects(Set<String> left, Set<String> right) {
        return left.stream().anyMatch(right::contains);
    }

    private String styleTagKey(String styleTag) {
        if (styleTag == null) {
            return "";
        }
        return styleTag.trim().toLowerCase(Locale.ROOT);
    }

    private List<String> readStyleTags(String styleTagsJson) {
        return RecommendationStyleTags.fromJson(styleTagsJson);
    }

    private enum Overlap {
        NONE,
        PARTIAL,
        SAME
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

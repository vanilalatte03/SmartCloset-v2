package com.smartcloset.recommendation.domain;

import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.domain.ClothingMaterial;
import com.smartcloset.weather.domain.WeatherCondition;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RecommendationReasonGenerator {

    public List<String> generate(
            OutfitCandidate candidate,
            RecommendationScore score,
            WeatherCondition weather,
            List<WearHistory> wearHistories,
            List<RecommendationResult> recommendationHistories,
            LocalDateTime requestedAt
    ) {
        Objects.requireNonNull(candidate, "candidate must not be null");
        Objects.requireNonNull(score, "score must not be null");
        Objects.requireNonNull(weather, "weather must not be null");
        Objects.requireNonNull(wearHistories, "wearHistories must not be null");
        Objects.requireNonNull(recommendationHistories, "recommendationHistories must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");

        List<String> reasons = new ArrayList<>();
        reasons.add(weatherReason(candidate, weather));
        reasons.add(colorReason(candidate));
        reasons.add(historyReason(score));

        materialReason(candidate, weather).ifPresent(reasons::add);
        reasons.add(diversityReason(score));

        return reasons.stream()
                .distinct()
                .limit(5)
                .toList();
    }

    private String weatherReason(OutfitCandidate candidate, WeatherCondition weather) {
        if (weather.temperature() <= 12 && candidate.hasOuter()) {
            return "현재 기온이 낮아 아우터를 포함한 조합을 추천했습니다.";
        }
        if (weather.rainy()) {
            boolean allRainSuitable = candidate.stream().allMatch(ClothingItem::isRainSuitable);
            if (allRainSuitable) {
                return "비가 오는 조건에서도 착용하기 좋은 옷이 포함되어 있습니다.";
            }
            return "비에 적합하지 않은 옷이 포함되어 날씨 점수가 일부 낮아졌습니다.";
        }
        return "선택된 옷들이 현재 기온에 맞는 온도 범위에 있습니다.";
    }

    private String colorReason(OutfitCandidate candidate) {
        ClothingColor top = candidate.top().getColor();
        ClothingColor bottom = candidate.bottom().getColor();

        if (top == ClothingColor.UNKNOWN || bottom == ClothingColor.UNKNOWN) {
            return "색상 정보가 부족해 색상 점수는 중립으로 반영했습니다.";
        }
        if (isStrongClash(top, bottom)) {
            return "색상 대비가 강해 색상 점수가 낮게 반영되었습니다.";
        }
        if (isComplementary(top, bottom)) {
            return "대비가 있는 색상 조합이라 포인트가 분명합니다.";
        }
        if (isNeutral(top) && isNeutral(bottom)) {
            return "상의와 하의 색상이 무채색 중심이라 안정적인 조합입니다.";
        }
        if (isNeutral(top) || isNeutral(bottom)) {
            return "무채색과 포인트 색상이 함께 있어 균형 잡힌 조합입니다.";
        }
        if (sameNonNeutralGroup(top, bottom)) {
            return "비슷한 색상 계열이어서 자연스럽게 이어지는 조합입니다.";
        }
        return "무채색과 포인트 색상이 함께 있어 균형 잡힌 조합입니다.";
    }

    private String historyReason(RecommendationScore score) {
        if (score.wearHistoryScore() <= 5) {
            return "어제 또는 오늘 착용한 옷이 포함되어 착용 이력 점수가 크게 낮아졌습니다.";
        }
        if (score.wearHistoryScore() <= 10) {
            return "최근 3일 이내 착용한 옷이 포함되어 반복 착용 부담을 반영했습니다.";
        }
        if (score.wearHistoryScore() <= 15) {
            return "최근 7일 이내 착용한 옷이 포함되어 착용 이력 점수가 일부 낮아졌습니다.";
        }
        return "최근 착용 이력이 적어 반복 착용 부담이 낮습니다.";
    }

    private java.util.Optional<String> materialReason(OutfitCandidate candidate, WeatherCondition weather) {
        if (weather.temperature() <= 12 && candidate.stream().anyMatch(this::isWarmMaterial)) {
            return java.util.Optional.of("니트 또는 울 소재가 현재 기온에 적합해 보온성을 보완합니다.");
        }
        if (weather.temperature() >= 25 && candidate.stream().anyMatch(this::isWarmMaterial)) {
            return java.util.Optional.of("더운 날씨에는 니트 또는 울 소재가 부담스러울 수 있어 날씨 점수가 낮아졌습니다.");
        }
        if (weather.rainy() && candidate.stream().anyMatch(item -> item.getMaterial() == ClothingMaterial.NYLON)) {
            return java.util.Optional.of("나일론 소재는 비 오는 날 착용에 유리해 날씨 점수에 긍정적으로 반영되었습니다.");
        }
        if (weather.rainy() && candidate.stream().anyMatch(item -> item.getMaterial() == ClothingMaterial.WOOL)) {
            return java.util.Optional.of("비 오는 날 울 소재는 젖었을 때 불편할 수 있어 날씨 점수가 낮아졌습니다.");
        }
        if (candidate.stream().anyMatch(item -> item.getMaterial() == ClothingMaterial.UNKNOWN)) {
            return java.util.Optional.of("소재 정보가 부족해 소재 기반 보정은 적용하지 않았습니다.");
        }
        return java.util.Optional.empty();
    }

    private String diversityReason(RecommendationScore score) {
        if (score.diversityScore() == 0) {
            return "최근 추천된 동일 조합이라 다양성 점수는 낮게 반영되었습니다.";
        }
        return "최근 추천된 동일 조합이 아니어서 반복 추천 부담이 낮습니다.";
    }

    private boolean isWarmMaterial(ClothingItem item) {
        return item.getMaterial() == ClothingMaterial.KNIT || item.getMaterial() == ClothingMaterial.WOOL;
    }

    private boolean isNeutral(ClothingColor color) {
        return color == ClothingColor.BLACK || color == ClothingColor.WHITE || color == ClothingColor.GRAY;
    }

    private boolean sameNonNeutralGroup(ClothingColor left, ClothingColor right) {
        return (isBlue(left) && isBlue(right))
                || (isEarth(left) && isEarth(right))
                || (isAccent(left) && isAccent(right));
    }

    private boolean isBlue(ClothingColor color) {
        return color == ClothingColor.NAVY || color == ClothingColor.BLUE;
    }

    private boolean isEarth(ClothingColor color) {
        return color == ClothingColor.BROWN || color == ClothingColor.BEIGE;
    }

    private boolean isAccent(ClothingColor color) {
        return color == ClothingColor.RED || color == ClothingColor.GREEN || color == ClothingColor.YELLOW;
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
}

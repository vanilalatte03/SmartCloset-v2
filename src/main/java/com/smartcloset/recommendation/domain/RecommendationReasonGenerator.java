package com.smartcloset.recommendation.domain;

import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.domain.ClothingMaterial;
import com.smartcloset.weather.domain.WeatherCondition;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 추천 점수의 주요 근거를 사용자가 읽을 수 있는 문장으로 변환한다.
 *
 * <p>이 클래스는 템플릿 기반 설명만 만든다. 추천 이유가 점수 계산과 어긋나지 않도록
 * {@link RecommendationScorer}의 조건과 같은 판단 기준을 반복해서 사용한다.</p>
 */
public class RecommendationReasonGenerator {

    /**
     * 개인화 입력이 없는 기존 추천 이유 생성 경로는 CASUAL 상황과 빈 선호 tag를 사용한다.
     */
    public List<String> generate(
            OutfitCandidate candidate,
            RecommendationScore score,
            WeatherCondition weather,
            List<WearHistorySnapshot> wearHistories,
            List<RecommendationHistorySnapshot> recommendationHistories,
            LocalDateTime requestedAt
    ) {
        return generate(
                candidate,
                score,
                weather,
                wearHistories,
                recommendationHistories,
                requestedAt,
                List.of(),
                RecommendationSituation.CASUAL
        );
    }

    /**
     * 점수, 날씨, 이력, 상황, 선호 tag를 바탕으로 중복 없는 추천 이유를 최대 5개 생성한다.
     */
    public List<String> generate(
            OutfitCandidate candidate,
            RecommendationScore score,
            WeatherCondition weather,
            List<WearHistorySnapshot> wearHistories,
            List<RecommendationHistorySnapshot> recommendationHistories,
            LocalDateTime requestedAt,
            List<String> preferredStyleTags,
            RecommendationSituation situation
    ) {
        Objects.requireNonNull(candidate, "candidate must not be null");
        Objects.requireNonNull(score, "score must not be null");
        Objects.requireNonNull(weather, "weather must not be null");
        Objects.requireNonNull(wearHistories, "wearHistories must not be null");
        Objects.requireNonNull(recommendationHistories, "recommendationHistories must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        Objects.requireNonNull(preferredStyleTags, "preferredStyleTags must not be null");

        List<String> reasons = new ArrayList<>();
        reasons.add(weatherReason(candidate, weather));
        reasons.add(colorReason(candidate));
        reasons.add(historyReason(score));

        // 기본 3개 이유 뒤에 개인화/소재/피드백 이유를 보충하고 중복 문장은 제거한다.
        userStyleTagReason(candidate, preferredStyleTags).ifPresent(reasons::add);
        situationReason(candidate, situation).ifPresent(reasons::add);
        feedbackReason(candidate, recommendationHistories, requestedAt, weather).ifPresent(reasons::add);
        materialReason(candidate, weather).ifPresent(reasons::add);
        preferenceReason(score).ifPresent(reasons::add);

        return reasons.stream()
                .distinct()
                .limit(5)
                .toList();
    }

    /**
     * 날씨 설명은 추천 성공 여부와 가장 직접적으로 연결되므로 항상 하나 이상 제공한다.
     */
    private String weatherReason(OutfitCandidate candidate, WeatherCondition weather) {
        int temperature = weather.temperature();
        if (temperature <= 12 && candidate.hasOuter()) {
            return "현재 기온이 낮아 아우터를 포함한 조합을 추천했습니다.";
        }
        if (weather.rainy()) {
            boolean allRainSuitable = candidate.stream().allMatch(ClothingItem::isRainSuitable);
            if (allRainSuitable) {
                return "비가 오는 조건에서도 착용하기 좋은 옷이 포함되어 있습니다.";
            }
            return "비에 적합하지 않은 옷이 포함되어 날씨 점수가 일부 낮아졌습니다.";
        }
        if (temperature >= 25 && !candidate.hasOuter()) {
            return temperature + "°C 기준으로 아우터 없이 가볍게 입기 좋은 조합입니다.";
        }
        if (temperature >= 25) {
            return temperature + "°C에도 각 옷의 권장 온도 범위 안에 있어 실내외 이동에 맞췄습니다.";
        }
        if (temperature >= 17 && !candidate.hasOuter()) {
            return temperature + "°C의 온화한 날씨에 상하의만으로 부담이 적은 조합입니다.";
        }
        if (temperature >= 17) {
            return temperature + "°C 날씨에 가볍게 걸칠 수 있는 범위의 옷을 골랐습니다.";
        }
        if (temperature >= 13 && candidate.hasOuter()) {
            return temperature + "°C의 선선한 기온이라 아우터로 체온 조절하기 좋습니다.";
        }
        if (temperature >= 13) {
            return temperature + "°C 기준으로 상의와 하의가 모두 권장 온도 안에 있어 무리가 적습니다.";
        }
        return temperature + "°C 기준으로 선택된 옷들이 모두 낮은 기온 범위에 맞습니다.";
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

    private java.util.Optional<String> preferenceReason(RecommendationScore score) {
        if (score.preferenceScore() <= 0) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of("선호 색상, 소재 또는 스타일 태그를 일부 반영했습니다.");
    }

    private java.util.Optional<String> userStyleTagReason(OutfitCandidate candidate, List<String> preferredStyleTags) {
        if (intersects(candidateStyleTagKeys(candidate), styleTagKeys(preferredStyleTags))) {
            return java.util.Optional.of("선호하는 스타일 태그와 겹치는 옷을 반영했어요.");
        }
        return java.util.Optional.empty();
    }

    private java.util.Optional<String> situationReason(OutfitCandidate candidate, RecommendationSituation situation) {
        if (intersects(candidateStyleTagKeys(candidate), situationStyleTagKeys(situation))) {
            return java.util.Optional.of(situationLabel(situation) + " 상황에 맞는 스타일 태그를 반영했어요.");
        }
        return java.util.Optional.empty();
    }

    /**
     * 최근 피드백이 현재 후보와 겹칠 때만 이유로 노출한다.
     */
    private java.util.Optional<String> feedbackReason(
            OutfitCandidate candidate,
            List<RecommendationHistorySnapshot> recommendationHistories,
            LocalDateTime requestedAt,
            WeatherCondition weather
    ) {
        LocalDateTime feedbackWindowStart = requestedAt.minusDays(14);
        boolean hasPositive = false;
        boolean hasNegative = false;

        for (RecommendationHistorySnapshot history : recommendationHistories) {
            if (!history.hasFeedback() || history.feedbackUpdatedAt().isBefore(feedbackWindowStart)) {
                continue;
            }
            boolean overlaps = candidate.intersects(history.clothingItemIds());
            if (!overlaps) {
                continue;
            }
            // 온도 피드백은 당시 기온과 현재 기온이 비슷할 때만 같은 문제로 간주한다.
            if (history.sentimentFeedback() == RecommendationFeedbackSentiment.DISLIKED
                    || (history.thermalFeedback() == RecommendationThermalFeedback.TOO_COLD
                    && weather.temperature() <= history.weatherTemperature() + 3)
                    || (history.thermalFeedback() == RecommendationThermalFeedback.TOO_HOT
                    && weather.temperature() >= history.weatherTemperature() - 3)) {
                hasNegative = true;
            }
            if (history.sentimentFeedback() == RecommendationFeedbackSentiment.LIKED) {
                hasPositive = true;
            }
        }

        if (hasNegative) {
            return java.util.Optional.of("최근 별로였거나 온도가 맞지 않았던 피드백을 피해 점수에 반영했어요.");
        }
        if (hasPositive) {
            return java.util.Optional.of("최근 마음에 든 조합과 일부 겹쳐 선호를 반영했어요.");
        }
        return java.util.Optional.empty();
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

    /**
     * 추천 상황을 사용자가 직접 입력한 태그와 같은 key 체계로 바꿔 비교한다.
     */
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

    private String situationLabel(RecommendationSituation situation) {
        RecommendationSituation resolvedSituation = situation == null ? RecommendationSituation.CASUAL : situation;
        return switch (resolvedSituation) {
            case WORK -> "출근";
            case CASUAL -> "캐주얼";
            case WORKOUT -> "운동";
            case DATE -> "데이트";
            case FORMAL -> "격식";
        };
    }
}

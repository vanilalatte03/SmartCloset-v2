package com.smartcloset.clothing.infrastructure.analysis;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * HTTP DTO와 분리된 옷 사진 분석 결과 모델이다.
 *
 * <p>낮은 confidence와 모델이 직접 표시한 확인 필요 field를 하나의 {@code reviewRequiredFields}
 * 목록으로 합쳐 프론트가 사용자 확인 UX를 일관되게 표시할 수 있게 한다.</p>
 */
public record ClothingAnalysisResult(
        boolean analyzable,
        ClothingAnalysisSuggestion suggestion,
        Map<ClothingAnalysisField, Double> fieldConfidence,
        List<ClothingAnalysisField> reviewRequiredFields,
        double lowConfidenceThreshold
) {

    public ClothingAnalysisResult {
        fieldConfidence = Map.copyOf(Objects.requireNonNull(fieldConfidence, "fieldConfidence must not be null"));
        reviewRequiredFields = List.copyOf(
                Objects.requireNonNull(reviewRequiredFields, "reviewRequiredFields must not be null")
        );
        if (lowConfidenceThreshold < 0.0 || lowConfidenceThreshold > 1.0) {
            throw new IllegalArgumentException("lowConfidenceThreshold must be between 0.0 and 1.0");
        }
        if (!analyzable && suggestion != null) {
            throw new IllegalArgumentException("suggestion must be null when image is not analyzable");
        }
    }

    /**
     * 옷으로 보기 어려운 이미지에 대한 성공 응답 모델을 만든다.
     */
    public static ClothingAnalysisResult notAnalyzable(double lowConfidenceThreshold) {
        return new ClothingAnalysisResult(false, null, Map.of(), List.of(), lowConfidenceThreshold);
    }

    /**
     * 분석 가능한 이미지의 후보값과 confidence를 검증하고 확인 필요 field를 계산한다.
     */
    public static ClothingAnalysisResult analyzable(
            ClothingAnalysisSuggestion suggestion,
            Map<ClothingAnalysisField, Double> fieldConfidence,
            List<ClothingAnalysisField> modelReviewRequiredFields,
            double lowConfidenceThreshold
    ) {
        Objects.requireNonNull(suggestion, "suggestion must not be null");
        Map<ClothingAnalysisField, Double> copiedConfidence = new EnumMap<>(ClothingAnalysisField.class);
        copiedConfidence.putAll(Objects.requireNonNull(fieldConfidence, "fieldConfidence must not be null"));
        copiedConfidence.forEach((field, confidence) -> {
            if (confidence == null || confidence < 0.0 || confidence > 1.0) {
                throw new IllegalArgumentException("fieldConfidence values must be between 0.0 and 1.0");
            }
        });

        EnumMap<ClothingAnalysisField, Boolean> reviewFields = new EnumMap<>(ClothingAnalysisField.class);
        for (ClothingAnalysisField field : Objects.requireNonNull(
                modelReviewRequiredFields,
                "modelReviewRequiredFields must not be null"
        )) {
            reviewFields.put(field, true);
        }
        copiedConfidence.entrySet().stream()
                .filter(entry -> entry.getValue() < lowConfidenceThreshold)
                .map(Map.Entry::getKey)
                .forEach(field -> reviewFields.put(field, true));

        return new ClothingAnalysisResult(
                true,
                suggestion,
                copiedConfidence,
                reviewFields.keySet().stream().toList(),
                lowConfidenceThreshold
        );
    }
}

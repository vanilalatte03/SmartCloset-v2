package com.smartcloset.clothing.dto;

import com.smartcloset.clothing.infrastructure.analysis.ClothingAnalysisField;
import com.smartcloset.clothing.infrastructure.analysis.ClothingAnalysisResult;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@code POST /api/clothes/analyze-image} 성공 응답 DTO다.
 *
 * <p>field 이름은 프론트의 옷 등록 form field와 맞춘 문자열로 반환한다.</p>
 */
public record ClothingAnalysisResponse(
        boolean analyzable,
        ClothingAnalysisSuggestionResponse suggestion,
        Map<String, Double> fieldConfidence,
        List<String> reviewRequiredFields,
        double lowConfidenceThreshold
) {

    /**
     * provider 결과 모델을 공개 API response shape로 변환한다.
     */
    public static ClothingAnalysisResponse from(ClothingAnalysisResult result) {
        return new ClothingAnalysisResponse(
                result.analyzable(),
                ClothingAnalysisSuggestionResponse.from(result.suggestion()),
                result.fieldConfidence()
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(
                                entry -> entry.getKey().propertyName(),
                                Map.Entry::getValue
                        )),
                result.reviewRequiredFields()
                        .stream()
                        .map(ClothingAnalysisField::propertyName)
                        .toList(),
                result.lowConfidenceThreshold()
        );
    }
}

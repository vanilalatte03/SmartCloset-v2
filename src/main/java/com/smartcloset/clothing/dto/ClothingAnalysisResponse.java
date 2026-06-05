package com.smartcloset.clothing.dto;

import com.smartcloset.clothing.infrastructure.analysis.ClothingAnalysisField;
import com.smartcloset.clothing.infrastructure.analysis.ClothingAnalysisResult;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record ClothingAnalysisResponse(
        boolean analyzable,
        ClothingAnalysisSuggestionResponse suggestion,
        Map<String, Double> fieldConfidence,
        List<String> reviewRequiredFields,
        double lowConfidenceThreshold
) {

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

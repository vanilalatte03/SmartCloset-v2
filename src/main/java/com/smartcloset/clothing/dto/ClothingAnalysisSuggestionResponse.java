package com.smartcloset.clothing.dto;

import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingMaterial;
import com.smartcloset.clothing.infrastructure.analysis.ClothingAnalysisSuggestion;
import java.util.List;

/**
 * 기존 옷 등록 request field와 같은 형태로 반환되는 AI 후보 DTO다.
 */
public record ClothingAnalysisSuggestionResponse(
        String name,
        ClothingCategory category,
        ClothingColor color,
        ClothingMaterial material,
        int minTemperature,
        int maxTemperature,
        boolean rainSuitable,
        List<String> styleTags
) {

    /**
     * 옷으로 보기 어려운 이미지에서는 {@code null} suggestion을 그대로 유지한다.
     */
    public static ClothingAnalysisSuggestionResponse from(ClothingAnalysisSuggestion suggestion) {
        if (suggestion == null) {
            return null;
        }
        return new ClothingAnalysisSuggestionResponse(
                suggestion.name(),
                suggestion.category(),
                suggestion.color(),
                suggestion.material(),
                suggestion.minTemperature(),
                suggestion.maxTemperature(),
                suggestion.rainSuitable(),
                suggestion.styleTags()
        );
    }
}

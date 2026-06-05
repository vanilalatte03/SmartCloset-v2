package com.smartcloset.clothing.dto;

import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingMaterial;
import com.smartcloset.clothing.infrastructure.analysis.ClothingAnalysisSuggestion;
import java.util.List;

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

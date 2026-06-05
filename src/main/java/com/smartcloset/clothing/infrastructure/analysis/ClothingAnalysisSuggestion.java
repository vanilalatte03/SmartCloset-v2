package com.smartcloset.clothing.infrastructure.analysis;

import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingMaterial;
import java.util.List;
import java.util.Objects;

/**
 * 기존 ClothingRequest에 대응하는 저장 전 후보값이다.
 *
 * <p>이 값은 AI 분석 응답으로만 반환되며, 사용자가 확인하거나 수정한 뒤 기존 옷 저장 API를 호출해야
 * 실제 {@code ClothingItem}으로 저장된다.</p>
 */
public record ClothingAnalysisSuggestion(
        String name,
        ClothingCategory category,
        ClothingColor color,
        ClothingMaterial material,
        int minTemperature,
        int maxTemperature,
        boolean rainSuitable,
        List<String> styleTags
) {

    public ClothingAnalysisSuggestion {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(color, "color must not be null");
        Objects.requireNonNull(material, "material must not be null");
        if (minTemperature > maxTemperature) {
            throw new IllegalArgumentException("minTemperature must be less than or equal to maxTemperature");
        }
        styleTags = List.copyOf(Objects.requireNonNullElse(styleTags, List.of()));
    }
}

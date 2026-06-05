package com.smartcloset.clothing.infrastructure.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingMaterial;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ClothingAnalysisResultTest {

    @Test
    void addsLowConfidenceFieldsToReviewRequiredFields() {
        ClothingAnalysisSuggestion suggestion = new ClothingAnalysisSuggestion(
                "흰색 셔츠",
                ClothingCategory.TOP,
                ClothingColor.WHITE,
                ClothingMaterial.COTTON,
                18,
                28,
                false,
                List.of("미니멀")
        );

        ClothingAnalysisResult result = ClothingAnalysisResult.analyzable(
                suggestion,
                Map.of(
                        ClothingAnalysisField.CATEGORY, 0.95,
                        ClothingAnalysisField.MATERIAL, 0.5,
                        ClothingAnalysisField.STYLE_TAGS, 0.7
                ),
                List.of(ClothingAnalysisField.NAME),
                0.75
        );

        assertThat(result.analyzable()).isTrue();
        assertThat(result.suggestion()).isEqualTo(suggestion);
        assertThat(result.reviewRequiredFields())
                .containsExactlyInAnyOrder(
                        ClothingAnalysisField.NAME,
                        ClothingAnalysisField.MATERIAL,
                        ClothingAnalysisField.STYLE_TAGS
                );
    }

    @Test
    void representsNotAnalyzableImageWithoutSuggestion() {
        ClothingAnalysisResult result = ClothingAnalysisResult.notAnalyzable(0.75);

        assertThat(result.analyzable()).isFalse();
        assertThat(result.suggestion()).isNull();
        assertThat(result.fieldConfidence()).isEmpty();
        assertThat(result.reviewRequiredFields()).isEmpty();
        assertThat(result.lowConfidenceThreshold()).isEqualTo(0.75);
    }
}

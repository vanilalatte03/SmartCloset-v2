package com.smartcloset.clothing.infrastructure.analysis;

/**
 * 기능 비활성 또는 provider 미설정 상태를 명시적으로 표현하는 analyzer다.
 */
public class DisabledClothingImageAnalyzer implements ClothingImageAnalyzer {

    @Override
    public ClothingAnalysisResult analyze(ClothingAnalysisImage image) {
        throw new ClothingImageAnalysisDisabledException("Clothing image analysis is disabled");
    }
}

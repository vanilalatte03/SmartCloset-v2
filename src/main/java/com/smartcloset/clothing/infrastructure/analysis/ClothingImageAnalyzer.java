package com.smartcloset.clothing.infrastructure.analysis;

/**
 * 옷 사진 분석 provider boundary다.
 */
public interface ClothingImageAnalyzer {

    ClothingAnalysisResult analyze(ClothingAnalysisImage image);
}

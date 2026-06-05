package com.smartcloset.clothing.infrastructure.analysis;

/**
 * AI 옷 등록 후보에서 confidence와 확인 필요 상태를 추적하는 저장 후보 field다.
 */
public enum ClothingAnalysisField {

    NAME("name"),
    CATEGORY("category"),
    COLOR("color"),
    MATERIAL("material"),
    MIN_TEMPERATURE("minTemperature"),
    MAX_TEMPERATURE("maxTemperature"),
    RAIN_SUITABLE("rainSuitable"),
    STYLE_TAGS("styleTags");

    private final String propertyName;

    ClothingAnalysisField(String propertyName) {
        this.propertyName = propertyName;
    }

    public String propertyName() {
        return propertyName;
    }
}

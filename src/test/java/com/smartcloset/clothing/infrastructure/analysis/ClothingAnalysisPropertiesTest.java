package com.smartcloset.clothing.infrastructure.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ClothingAnalysisPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ClothingAnalysisPropertiesConfig.class);

    @Test
    void usesDocumentedDisabledDefaults() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ClothingAnalysisProperties.class);

            ClothingAnalysisProperties properties = context.getBean(ClothingAnalysisProperties.class);

            assertThat(properties.enabled()).isFalse();
            assertThat(properties.lowConfidenceThreshold())
                    .isEqualTo(ClothingAnalysisProperties.DEFAULT_LOW_CONFIDENCE_THRESHOLD);
            assertThat(properties.dailyLimit()).isEqualTo(ClothingAnalysisProperties.DEFAULT_DAILY_LIMIT);
            assertThat(properties.timeoutSeconds()).isEqualTo(ClothingAnalysisProperties.DEFAULT_TIMEOUT_SECONDS);
        });
    }

    @Test
    void bindsConfiguredValues() {
        contextRunner
                .withPropertyValues(
                        "smartcloset.clothing.analysis.enabled=true",
                        "smartcloset.clothing.analysis.low-confidence-threshold=0.6",
                        "smartcloset.clothing.analysis.daily-limit=3",
                        "smartcloset.clothing.analysis.timeout-seconds=5"
                )
                .run(context -> {
                    ClothingAnalysisProperties properties = context.getBean(ClothingAnalysisProperties.class);

                    assertThat(properties.enabled()).isTrue();
                    assertThat(properties.lowConfidenceThreshold()).isEqualTo(0.6);
                    assertThat(properties.dailyLimit()).isEqualTo(3);
                    assertThat(properties.timeoutSeconds()).isEqualTo(5);
                });
    }
}

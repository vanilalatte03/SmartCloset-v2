package com.smartcloset.clothing.infrastructure.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

class SpringAiClothingImageAnalyzerTest {

    @Test
    void failsWhenProviderCallExceedsConfiguredTimeout() {
        ClothingAnalysisProperties properties = new ClothingAnalysisProperties();
        properties.setTimeoutSeconds(1);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        SpringAiClothingImageAnalyzer analyzer = new SpringAiClothingImageAnalyzer(
                mock(ChatClient.class),
                properties,
                executorService
        ) {
            @Override
            OpenAiClothingAnalysisResponse callProvider(ClothingAnalysisImage image) {
                try {
                    Thread.sleep(5_000);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
                return new OpenAiClothingAnalysisResponse(false, null, null, null);
            }
        };

        long startedAt = System.nanoTime();

        try {
            assertThatThrownBy(() -> analyzer.analyze(new ClothingAnalysisImage(new byte[] {1}, "image/png")))
                    .isInstanceOf(ClothingImageAnalysisUnavailableException.class)
                    .hasMessageContaining("timed out");
        } finally {
            analyzer.close();
        }

        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
        assertThat(elapsedMillis).isLessThan(3_000);
    }

    @Test
    void mapsUnknownSuggestionEnumValueToProviderUnavailable() {
        SpringAiClothingImageAnalyzer analyzer = analyzerReturning(
                analyzableResponse(validSuggestionWithCategory("HAT"), validConfidence())
        );

        assertMalformedUnavailable(analyzer);
    }

    @Test
    void retriesProviderUnavailableAndOpensCircuitBreaker() {
        AtomicInteger callCount = new AtomicInteger();
        ClothingAnalysisProviderResilience resilience = new ClothingAnalysisProviderResilience(
                2,
                java.time.Duration.ZERO,
                1,
                java.time.Duration.ofMinutes(1)
        );
        SpringAiClothingImageAnalyzer analyzer = failingAnalyzer(callCount, resilience);

        try {
            assertThatThrownBy(() -> analyzer.analyze(new ClothingAnalysisImage(new byte[] {1}, "image/png")))
                    .isInstanceOf(ClothingImageAnalysisUnavailableException.class)
                    .hasMessageContaining("provider unavailable");
            assertThat(callCount).hasValue(2);

            assertThatThrownBy(() -> analyzer.analyze(new ClothingAnalysisImage(new byte[] {1}, "image/png")))
                    .isInstanceOf(ClothingImageAnalysisUnavailableException.class)
                    .hasMessageContaining("circuit breaker is open");
            assertThat(callCount).hasValue(2);
        } finally {
            analyzer.close();
        }
    }

    @Test
    void mapsInvalidSuggestionValidationToProviderUnavailable() {
        SpringAiClothingImageAnalyzer blankNameAnalyzer = analyzerReturning(
                analyzableResponse(
                        new SpringAiClothingImageAnalyzer.OpenAiSuggestion(
                                " ",
                                "TOP",
                                "WHITE",
                                "COTTON",
                                18,
                                28,
                                false,
                                List.of("미니멀")
                        ),
                        validConfidence()
                )
        );
        SpringAiClothingImageAnalyzer invalidTemperatureAnalyzer = analyzerReturning(
                analyzableResponse(
                        new SpringAiClothingImageAnalyzer.OpenAiSuggestion(
                                "화이트 셔츠",
                                "TOP",
                                "WHITE",
                                "COTTON",
                                30,
                                10,
                                false,
                                List.of("미니멀")
                        ),
                        validConfidence()
                )
        );

        assertMalformedUnavailable(blankNameAnalyzer);
        assertMalformedUnavailable(invalidTemperatureAnalyzer);
    }

    @Test
    void keepsMissingFieldConfidenceAsProviderUnavailable() {
        SpringAiClothingImageAnalyzer analyzer = analyzerReturning(
                analyzableResponse(validSuggestionWithCategory("TOP"), null)
        );

        assertMalformedUnavailable(analyzer);
    }

    private SpringAiClothingImageAnalyzer analyzerReturning(
            SpringAiClothingImageAnalyzer.OpenAiClothingAnalysisResponse response
    ) {
        return new SpringAiClothingImageAnalyzer(
                mock(ChatClient.class),
                new ClothingAnalysisProperties()
        ) {
            @Override
            OpenAiClothingAnalysisResponse callProvider(ClothingAnalysisImage image) {
                return response;
            }
        };
    }

    private SpringAiClothingImageAnalyzer failingAnalyzer(
            AtomicInteger callCount,
            ClothingAnalysisProviderResilience resilience
    ) {
        return new SpringAiClothingImageAnalyzer(
                mock(ChatClient.class),
                new ClothingAnalysisProperties(),
                Executors.newSingleThreadExecutor(),
                resilience
        ) {
            @Override
            OpenAiClothingAnalysisResponse callProvider(ClothingAnalysisImage image) {
                callCount.incrementAndGet();
                throw new ClothingImageAnalysisUnavailableException("provider unavailable");
            }
        };
    }

    private SpringAiClothingImageAnalyzer.OpenAiSuggestion validSuggestionWithCategory(String category) {
        return new SpringAiClothingImageAnalyzer.OpenAiSuggestion(
                "화이트 셔츠",
                category,
                "WHITE",
                "COTTON",
                18,
                28,
                false,
                List.of("미니멀")
        );
    }

    private SpringAiClothingImageAnalyzer.OpenAiClothingAnalysisResponse analyzableResponse(
            SpringAiClothingImageAnalyzer.OpenAiSuggestion suggestion,
            Map<String, Double> confidence
    ) {
        return new SpringAiClothingImageAnalyzer.OpenAiClothingAnalysisResponse(
                true,
                suggestion,
                confidence,
                List.of()
        );
    }

    private Map<String, Double> validConfidence() {
        return Map.of(
                "name", 0.92,
                "category", 0.91,
                "color", 0.9,
                "material", 0.88
        );
    }

    private void assertMalformedUnavailable(SpringAiClothingImageAnalyzer analyzer) {
        try {
            assertThatThrownBy(() -> analyzer.analyze(new ClothingAnalysisImage(new byte[] {1}, "image/png")))
                    .isInstanceOf(ClothingImageAnalysisUnavailableException.class)
                    .hasMessageContaining("Malformed clothing image analysis output");
        } finally {
            analyzer.close();
        }
    }
}

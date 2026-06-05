package com.smartcloset.clothing.infrastructure.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
}

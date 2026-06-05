package com.smartcloset.clothing.infrastructure.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class ClothingImageAnalyzerConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    ClothingAnalysisPropertiesConfig.class,
                    ClothingImageAnalyzerConfig.class
            );

    @Test
    void usesDisabledAnalyzerByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ClothingImageAnalyzer.class);
            ClothingImageAnalyzer analyzer = context.getBean(ClothingImageAnalyzer.class);

            assertThat(analyzer).isInstanceOf(DisabledClothingImageAnalyzer.class);
            assertThatThrownBy(() -> analyzer.analyze(new ClothingAnalysisImage(new byte[] {1}, "image/png")))
                    .isInstanceOf(ClothingImageAnalysisDisabledException.class);
        });
    }

    @Test
    void keepsDisabledAnalyzerWhenOpenAiChatClientIsUnavailable() {
        contextRunner
                .withPropertyValues(
                        "smartcloset.clothing.analysis.enabled=true",
                        "spring.ai.model.chat=openai",
                        "spring.ai.openai.api-key=test-key"
                )
                .run(context -> assertThat(context.getBean(ClothingImageAnalyzer.class))
                        .isInstanceOf(DisabledClothingImageAnalyzer.class));
    }

    @Test
    void usesSpringAiAnalyzerWhenEnabledWithOpenAiAndChatClientBuilder() {
        contextRunner
                .withUserConfiguration(ChatClientBuilderTestConfig.class)
                .withPropertyValues(
                        "smartcloset.clothing.analysis.enabled=true",
                        "spring.ai.model.chat=openai",
                        "spring.ai.openai.api-key=test-key"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(ClothingImageAnalyzer.class);
                    assertThat(context.getBean(ClothingImageAnalyzer.class))
                            .isInstanceOf(SpringAiClothingImageAnalyzer.class);
                });
    }

    @Configuration
    static class ChatClientBuilderTestConfig {

        @Bean
        ChatClient.Builder chatClientBuilder() {
            ChatClient.Builder builder = mock(ChatClient.Builder.class);
            when(builder.build()).thenReturn(mock(ChatClient.class));
            return builder;
        }
    }
}

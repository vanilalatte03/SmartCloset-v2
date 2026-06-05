package com.smartcloset.clothing.infrastructure.analysis;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@Configuration
public class ClothingImageAnalyzerConfig {

    @Bean
    @ConditionalOnProperty(name = "smartcloset.clothing.analysis.enabled", havingValue = "true")
    @ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "openai")
    @ConditionalOnBean(ChatModel.class)
    public ClothingImageAnalyzer springAiClothingImageAnalyzer(
            ClothingAnalysisProperties properties,
            ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            Environment environment
    ) {
        ChatClient.Builder chatClientBuilder = chatClientBuilderProvider.getIfAvailable();
        if (chatClientBuilder == null || !hasOpenAiApiKey(environment)) {
            return new DisabledClothingImageAnalyzer();
        }
        return new SpringAiClothingImageAnalyzer(chatClientBuilder.build(), properties);
    }

    @Bean
    @ConditionalOnMissingBean(ClothingImageAnalyzer.class)
    public ClothingImageAnalyzer disabledClothingImageAnalyzer() {
        return new DisabledClothingImageAnalyzer();
    }

    private boolean hasOpenAiApiKey(Environment environment) {
        String apiKey = environment.getProperty("spring.ai.openai.api-key", "");
        return StringUtils.hasText(apiKey);
    }
}

package com.smartcloset.clothing.infrastructure.analysis;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@Configuration
public class ClothingImageAnalyzerConfig {

    /**
     * 기능 flag, Spring AI chat model, OpenAI API key가 모두 준비된 경우에만 실제 analyzer를 만든다.
     *
     * <p>조건이 부족하면 disabled analyzer를 반환해 local/Compose 기본 실행이 OpenAI 설정 없이도
     * 깨지지 않게 한다.</p>
     */
    @Bean
    @ConditionalOnProperty(name = "smartcloset.clothing.analysis.enabled", havingValue = "true")
    @ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "openai")
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

    /**
     * 분석 기능이 명시적으로 준비되지 않은 모든 profile의 기본 analyzer다.
     */
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

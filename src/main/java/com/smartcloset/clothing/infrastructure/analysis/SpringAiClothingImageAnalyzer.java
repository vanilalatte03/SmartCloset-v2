package com.smartcloset.clothing.infrastructure.analysis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingMaterial;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeTypeUtils;

/**
 * Spring AI {@link ChatClient} 기반 OpenAI 옷 사진 분석 adapter다.
 *
 * <p>이 class는 외부 AI 응답을 내부 {@link ClothingAnalysisResult}로 정규화하는 adapter이며,
 * 분석 이미지를 저장하거나 추천 domain service와 연결하지 않는다.</p>
 */
public class SpringAiClothingImageAnalyzer implements ClothingImageAnalyzer, AutoCloseable {

    private static final String SYSTEM_PROMPT = """
            You analyze one clothing photo for SmartCloset clothing registration.
            Return only structured data matching the requested schema.
            Use only these enum values:
            category: TOP, BOTTOM, OUTER
            color: BLACK, WHITE, GRAY, NAVY, BLUE, BROWN, BEIGE, RED, GREEN, YELLOW, UNKNOWN
            material: COTTON, DENIM, KNIT, WOOL, POLYESTER, NYLON, UNKNOWN
            If the image is not clearly a clothing item, set analyzable=false and suggestion=null.
            Confidence values must be between 0 and 1.
            Keep styleTags short and suitable for a Korean clothing app.
            """;

    private static final String USER_PROMPT = """
            Suggest clothing registration fields from this image:
            name, category, color, material, minTemperature, maxTemperature, rainSuitable, styleTags.
            Also provide fieldConfidence and reviewRequiredFields for uncertain fields.
            """;

    private final ChatClient chatClient;
    private final ClothingAnalysisProperties properties;
    private final ExecutorService executorService;

    public SpringAiClothingImageAnalyzer(ChatClient chatClient, ClothingAnalysisProperties properties) {
        this(chatClient, properties, Executors.newVirtualThreadPerTaskExecutor());
    }

    SpringAiClothingImageAnalyzer(
            ChatClient chatClient,
            ClothingAnalysisProperties properties,
            ExecutorService executorService
    ) {
        this.chatClient = Objects.requireNonNull(chatClient, "chatClient must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.executorService = Objects.requireNonNull(executorService, "executorService must not be null");
    }

    @Override
    public ClothingAnalysisResult analyze(ClothingAnalysisImage image) {
        Future<OpenAiClothingAnalysisResponse> responseFuture = executorService.submit(() -> callProvider(image));
        try {
            OpenAiClothingAnalysisResponse response = responseFuture.get(timeoutSeconds(), TimeUnit.SECONDS);
            return toResult(response);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ClothingImageAnalysisUnavailableException("Clothing image analysis provider was interrupted", exception);
        } catch (TimeoutException exception) {
            responseFuture.cancel(true);
            throw new ClothingImageAnalysisUnavailableException("Clothing image analysis provider timed out", exception);
        } catch (ExecutionException exception) {
            throw providerFailure(exception.getCause());
        }
    }

    OpenAiClothingAnalysisResponse callProvider(ClothingAnalysisImage image) {
        try {
            return chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(user -> user.text(USER_PROMPT)
                            .media(Media.builder()
                                    .mimeType(MimeTypeUtils.parseMimeType(image.contentType()))
                                    .data(new NamedByteArrayResource(image.bytes(), "clothing-analysis-image"))
                                    .build()))
                    .call()
                    .entity(OpenAiClothingAnalysisResponse.class);
        } catch (ClothingImageAnalysisUnavailableException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ClothingImageAnalysisUnavailableException("Clothing image analysis provider failed", exception);
        }
    }

    @Override
    public void close() {
        executorService.shutdownNow();
    }

    private long timeoutSeconds() {
        return Math.max(1, properties.timeoutSeconds());
    }

    private ClothingImageAnalysisUnavailableException providerFailure(Throwable cause) {
        if (cause instanceof ClothingImageAnalysisUnavailableException unavailableException) {
            return unavailableException;
        }
        if (cause == null) {
            return new ClothingImageAnalysisUnavailableException("Clothing image analysis provider failed");
        }
        return new ClothingImageAnalysisUnavailableException("Clothing image analysis provider failed", cause);
    }

    /**
     * Spring AI structured output을 내부 결과 모델로 변환하고, malformed output은 provider 장애로 격리한다.
     */
    private ClothingAnalysisResult toResult(OpenAiClothingAnalysisResponse response) {
        if (response == null || response.analyzable == null) {
            throw malformedOutput("Missing analyzable field");
        }
        if (!response.analyzable) {
            return ClothingAnalysisResult.notAnalyzable(properties.lowConfidenceThreshold());
        }
        if (response.suggestion == null) {
            throw malformedOutput("Missing suggestion for analyzable image");
        }

        try {
            return ClothingAnalysisResult.analyzable(
                    response.suggestion.toSuggestion(),
                    toConfidence(response.fieldConfidence),
                    toFields(response.reviewRequiredFields),
                    properties.lowConfidenceThreshold()
            );
        } catch (ClothingImageAnalysisUnavailableException exception) {
            throw exception;
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw malformedOutput("Invalid analyzable response", exception);
        }
    }

    /**
     * 모델이 반환한 property name 기반 confidence map을 내부 enum key map으로 변환한다.
     */
    private Map<ClothingAnalysisField, Double> toConfidence(Map<String, Double> rawConfidence) {
        if (rawConfidence == null || rawConfidence.isEmpty()) {
            throw malformedOutput("Missing fieldConfidence");
        }
        EnumMap<ClothingAnalysisField, Double> confidence = new EnumMap<>(ClothingAnalysisField.class);
        rawConfidence.forEach((field, value) -> confidence.put(toField(field), value));
        return confidence;
    }

    /**
     * 모델이 직접 확인 필요로 표시한 field 이름을 내부 enum 목록으로 변환한다.
     */
    private List<ClothingAnalysisField> toFields(List<String> rawFields) {
        if (rawFields == null) {
            return List.of();
        }
        return rawFields.stream()
                .map(this::toField)
                .distinct()
                .toList();
    }

    private ClothingAnalysisField toField(String rawField) {
        if (rawField == null || rawField.isBlank()) {
            throw malformedOutput("Blank review field");
        }
        String normalized = rawField.trim();
        for (ClothingAnalysisField field : ClothingAnalysisField.values()) {
            if (field.propertyName().equals(normalized)) {
                return field;
            }
        }
        throw malformedOutput("Unknown review field: " + rawField);
    }

    private ClothingImageAnalysisUnavailableException malformedOutput(String message) {
        return new ClothingImageAnalysisUnavailableException("Malformed clothing image analysis output: " + message);
    }

    private ClothingImageAnalysisUnavailableException malformedOutput(String message, Throwable cause) {
        return new ClothingImageAnalysisUnavailableException(
                "Malformed clothing image analysis output: " + message,
                cause
        );
    }

    /**
     * OpenAI structured output schema와 맞춘 transport record다.
     *
     * <p>외부 응답 shape를 내부 결과 모델과 분리해 enum 변환, confidence 검증, not-analyzable 처리를
     * adapter 안에서 끝낸다.</p>
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record OpenAiClothingAnalysisResponse(
            Boolean analyzable,
            OpenAiSuggestion suggestion,
            Map<String, Double> fieldConfidence,
            List<String> reviewRequiredFields
    ) {
    }

    /**
     * 기존 ClothingRequest field와 1:1로 맞춘 OpenAI 후보 field set이다.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record OpenAiSuggestion(
            String name,
            String category,
            String color,
            String material,
            Integer minTemperature,
            Integer maxTemperature,
            Boolean rainSuitable,
            List<String> styleTags
    ) {

        /**
         * 문자열 enum 값을 domain enum으로 변환해 저장 전 후보 모델을 만든다.
         */
        private ClothingAnalysisSuggestion toSuggestion() {
            if (name == null || category == null || color == null || material == null
                    || minTemperature == null || maxTemperature == null || rainSuitable == null) {
                throw new ClothingImageAnalysisUnavailableException(
                        "Malformed clothing image analysis output: Missing suggestion field"
                );
            }
            return new ClothingAnalysisSuggestion(
                    name,
                    ClothingCategory.valueOf(category.trim().toUpperCase(Locale.ROOT)),
                    ClothingColor.valueOf(color.trim().toUpperCase(Locale.ROOT)),
                    ClothingMaterial.valueOf(material.trim().toUpperCase(Locale.ROOT)),
                    minTemperature,
                    maxTemperature,
                    rainSuitable,
                    styleTags
            );
        }
    }

    /**
     * Spring AI media upload에 안정적인 filename을 제공하기 위한 in-memory resource다.
     */
    private static final class NamedByteArrayResource extends ByteArrayResource {

        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}

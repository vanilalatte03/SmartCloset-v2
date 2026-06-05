package com.smartcloset.clothing.infrastructure.analysis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingMaterial;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeTypeUtils;

/**
 * Spring AI ChatClient 기반 OpenAI 옷 사진 분석 adapter다.
 */
public class SpringAiClothingImageAnalyzer implements ClothingImageAnalyzer {

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

    public SpringAiClothingImageAnalyzer(ChatClient chatClient, ClothingAnalysisProperties properties) {
        this.chatClient = chatClient;
        this.properties = properties;
    }

    @Override
    public ClothingAnalysisResult analyze(ClothingAnalysisImage image) {
        try {
            OpenAiClothingAnalysisResponse response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(user -> user.text(USER_PROMPT)
                            .media(Media.builder()
                                    .mimeType(MimeTypeUtils.parseMimeType(image.contentType()))
                                    .data(new NamedByteArrayResource(image.bytes(), "clothing-analysis-image"))
                                    .build()))
                    .call()
                    .entity(OpenAiClothingAnalysisResponse.class);
            return toResult(response);
        } catch (ClothingImageAnalysisUnavailableException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ClothingImageAnalysisUnavailableException("Clothing image analysis provider failed", exception);
        }
    }

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

        return ClothingAnalysisResult.analyzable(
                response.suggestion.toSuggestion(),
                toConfidence(response.fieldConfidence),
                toFields(response.reviewRequiredFields),
                properties.lowConfidenceThreshold()
        );
    }

    private Map<ClothingAnalysisField, Double> toConfidence(Map<String, Double> rawConfidence) {
        if (rawConfidence == null || rawConfidence.isEmpty()) {
            throw malformedOutput("Missing fieldConfidence");
        }
        EnumMap<ClothingAnalysisField, Double> confidence = new EnumMap<>(ClothingAnalysisField.class);
        rawConfidence.forEach((field, value) -> confidence.put(toField(field), value));
        return confidence;
    }

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

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OpenAiClothingAnalysisResponse(
            Boolean analyzable,
            OpenAiSuggestion suggestion,
            Map<String, Double> fieldConfidence,
            List<String> reviewRequiredFields
    ) {
    }

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

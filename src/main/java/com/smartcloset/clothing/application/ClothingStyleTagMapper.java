package com.smartcloset.clothing.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ClothingStyleTagMapper {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public ClothingStyleTagMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toJson(List<String> styleTags) {
        try {
            return objectMapper.writeValueAsString(normalize(styleTags));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("styleTags must be serializable to a JSON array", exception);
        }
    }

    public List<String> fromJson(String styleTagsJson) {
        if (styleTagsJson == null || styleTagsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(styleTagsJson, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("styleTagsJson must contain a valid JSON array", exception);
        }
    }

    private List<String> normalize(List<String> styleTags) {
        if (styleTags == null || styleTags.isEmpty()) {
            return List.of();
        }
        Map<String, String> uniqueTags = new LinkedHashMap<>();
        for (String styleTag : styleTags) {
            String trimmed = styleTag.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            uniqueTags.putIfAbsent(comparisonKey(trimmed), trimmed);
        }
        return List.copyOf(uniqueTags.values());
    }

    private String comparisonKey(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}

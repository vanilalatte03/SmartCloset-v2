package com.smartcloset.recommendation.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

final class RecommendationStyleTags {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private RecommendationStyleTags() {
    }

    static List<String> fromJson(String styleTagsJson) {
        if (styleTagsJson == null || styleTagsJson.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(styleTagsJson, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("styleTagsJson must contain a valid JSON array", exception);
        }
    }
}

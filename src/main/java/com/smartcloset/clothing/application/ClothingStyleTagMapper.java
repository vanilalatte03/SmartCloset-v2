package com.smartcloset.clothing.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * style tag 배열을 DB의 JSON 문자열과 API 배열 사이에서 변환한다.
 *
 * <p>태그 비교는 ASCII 대소문자를 무시하지만, 사용자에게 보여줄 원래 표기는 처음 입력된 값을 보존한다.</p>
 */
@Component
public class ClothingStyleTagMapper {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public ClothingStyleTagMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * blank tag를 제거하고 ASCII 대소문자 중복을 정리한 뒤 JSON 배열 문자열로 직렬화한다.
     */
    public String toJson(List<String> styleTags) {
        try {
            return objectMapper.writeValueAsString(normalize(styleTags));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("styleTags must be serializable to a JSON array", exception);
        }
    }

    /**
     * DB JSON 문자열을 API 배열로 복원하며 비어 있는 값은 빈 배열로 취급한다.
     */
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

package com.smartcloset.user.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingMaterial;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * users 테이블의 JSON 문자열 선호도 컬럼을 enum/string 배열로 변환한다.
 *
 * <p>선호도는 추천 점수 계산에 자주 쓰이지만 별도 정규화 테이블은 MVP 범위 밖이라,
 * 이 mapper가 JSON 컬럼 접근을 한곳에 모아둔다.</p>
 */
@Component
public class PreferenceJsonMapper {

    private static final TypeReference<List<ClothingColor>> COLOR_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<ClothingMaterial>> MATERIAL_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public PreferenceJsonMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 선호 색상 enum 배열을 users 테이블에 저장할 JSON 배열 문자열로 직렬화한다.
     */
    public String toColorsJson(List<ClothingColor> colors) {
        return write(colors);
    }

    /**
     * 선호 소재 enum 배열을 users 테이블에 저장할 JSON 배열 문자열로 직렬화한다.
     */
    public String toMaterialsJson(List<ClothingMaterial> materials) {
        return write(materials);
    }

    /**
     * 선호 style tag 문자열 배열을 users 테이블에 저장할 JSON 배열 문자열로 직렬화한다.
     */
    public String toStyleTagsJson(List<String> styleTags) {
        return write(styleTags);
    }

    /**
     * 저장된 선호 색상 JSON 배열을 추천 계산용 enum 배열로 복원한다.
     */
    public List<ClothingColor> readColors(String json) {
        return read(json, COLOR_LIST_TYPE, "preferredColorsJson");
    }

    /**
     * 저장된 선호 소재 JSON 배열을 추천 계산용 enum 배열로 복원한다.
     */
    public List<ClothingMaterial> readMaterials(String json) {
        return read(json, MATERIAL_LIST_TYPE, "preferredMaterialsJson");
    }

    /**
     * 저장된 선호 style tag JSON 배열을 추천 계산용 문자열 배열로 복원한다.
     */
    public List<String> readStyleTags(String json) {
        return read(json, STRING_LIST_TYPE, "styleTagsJson");
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("preferences must be serializable to JSON arrays", exception);
        }
    }

    private <T> T read(String json, TypeReference<T> typeReference, String fieldName) {
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(fieldName + " must contain a valid JSON array", exception);
        }
    }
}

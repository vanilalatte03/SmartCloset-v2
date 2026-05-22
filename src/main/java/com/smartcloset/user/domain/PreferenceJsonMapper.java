package com.smartcloset.user.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingMaterial;
import java.util.List;
import org.springframework.stereotype.Component;

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

    public String toColorsJson(List<ClothingColor> colors) {
        return write(colors);
    }

    public String toMaterialsJson(List<ClothingMaterial> materials) {
        return write(materials);
    }

    public String toStyleTagsJson(List<String> styleTags) {
        return write(styleTags);
    }

    public List<ClothingColor> readColors(String json) {
        return read(json, COLOR_LIST_TYPE, "preferredColorsJson");
    }

    public List<ClothingMaterial> readMaterials(String json) {
        return read(json, MATERIAL_LIST_TYPE, "preferredMaterialsJson");
    }

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

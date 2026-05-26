package com.smartcloset.location.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record LocationOption(
        String code,
        String name,
        String fullName,
        String region1,
        String region2,
        String region3,
        int nx,
        int ny,
        BigDecimal latitude,
        BigDecimal longitude
) {

    private static final LocationOption DEFAULT_SEOUL = new LocationOption(
            "SEOUL",
            "서울특별시",
            "서울특별시",
            "서울특별시",
            null,
            null,
            60,
            127,
            null,
            null
    );

    public LocationOption(String code, String name, int nx, int ny) {
        this(code, name, name, name, null, null, nx, ny, null, null);
    }

    public LocationOption {
        code = requireText(code, "code", 30);
        name = requireText(name, "name", 50);
        fullName = requireText(fullName, "fullName", 100);
        region1 = requireText(region1, "region1", 50);
        region2 = requireNullableText(region2, "region2", 50);
        region3 = requireNullableText(region3, "region3", 50);
        if (nx <= 0) {
            throw new IllegalArgumentException("nx must be positive");
        }
        if (ny <= 0) {
            throw new IllegalArgumentException("ny must be positive");
        }
    }

    public static LocationOption defaultSeoul() {
        return DEFAULT_SEOUL;
    }

    private static String requireText(String value, String fieldName, int maxLength) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be " + maxLength + " characters or less");
        }
        return value;
    }

    private static String requireNullableText(String value, String fieldName, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be " + maxLength + " characters or less");
        }
        return value;
    }
}

package com.smartcloset.location.domain;

import java.util.Objects;

public record LocationOption(String code, String name, int nx, int ny) {

    private static final LocationOption DEFAULT_SEOUL = new LocationOption("SEOUL", "서울특별시", 60, 127);

    public LocationOption {
        code = requireText(code, "code", 30);
        name = requireText(name, "name", 50);
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
}

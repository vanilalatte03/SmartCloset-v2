package com.smartcloset.clothing.infrastructure.analysis;

import java.util.Arrays;
import java.util.Objects;

/**
 * HTTP multipart와 분리된 analyzer 입력 모델이다.
 */
public final class ClothingAnalysisImage {

    private final byte[] bytes;
    private final String contentType;

    public ClothingAnalysisImage(byte[] bytes, String contentType) {
        this.bytes = Arrays.copyOf(Objects.requireNonNull(bytes, "bytes must not be null"), bytes.length);
        this.contentType = Objects.requireNonNull(contentType, "contentType must not be null");
    }

    public byte[] bytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    public String contentType() {
        return contentType;
    }
}

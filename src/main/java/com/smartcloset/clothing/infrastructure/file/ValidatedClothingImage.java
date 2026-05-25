package com.smartcloset.clothing.infrastructure.file;

public record ValidatedClothingImage(String extension, String contentType, long sizeBytes) {
}

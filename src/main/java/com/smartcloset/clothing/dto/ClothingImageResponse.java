package com.smartcloset.clothing.dto;

import com.smartcloset.clothing.domain.ClothingItem;
import java.time.LocalDateTime;

public record ClothingImageResponse(
        String url,
        String contentType,
        long sizeBytes,
        LocalDateTime uploadedAt
) {

    public static ClothingImageResponse from(ClothingItem clothingItem) {
        if (clothingItem.getImageStoredFilename() == null) {
            return null;
        }
        return new ClothingImageResponse(
                "/api/clothes/" + clothingItem.getId() + "/image",
                clothingItem.getImageContentType(),
                clothingItem.getImageSizeBytes(),
                clothingItem.getImageUploadedAt()
        );
    }
}

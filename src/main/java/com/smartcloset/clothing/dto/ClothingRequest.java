package com.smartcloset.clothing.dto;

import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingMaterial;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClothingRequest(
        @NotBlank(message = "name must not be blank")
        @Size(max = 50, message = "name must be 50 characters or less")
        String name,

        @NotNull(message = "category is required")
        ClothingCategory category,

        @NotNull(message = "color is required")
        ClothingColor color,

        @NotNull(message = "material is required")
        ClothingMaterial material,

        @NotNull(message = "minTemperature is required")
        Integer minTemperature,

        @NotNull(message = "maxTemperature is required")
        Integer maxTemperature,

        @NotNull(message = "rainSuitable is required")
        Boolean rainSuitable
) {

    @AssertTrue(message = "minTemperature must be less than or equal to maxTemperature")
    public boolean isTemperatureRangeValid() {
        if (minTemperature == null || maxTemperature == null) {
            return true;
        }
        return minTemperature <= maxTemperature;
    }
}

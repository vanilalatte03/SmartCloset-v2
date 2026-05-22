package com.smartcloset.user.dto;

import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingMaterial;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateUserPreferencesRequest(
        @NotNull List<@NotNull ClothingColor> preferredColors,
        @NotNull List<@NotNull ClothingMaterial> preferredMaterials,
        @NotNull List<@NotBlank @Size(max = 30) String> styleTags
) {
}

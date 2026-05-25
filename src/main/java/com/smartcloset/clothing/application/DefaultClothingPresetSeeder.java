package com.smartcloset.clothing.application;

import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.domain.ClothingMaterial;
import com.smartcloset.clothing.infrastructure.file.ClothingImageStorage;
import com.smartcloset.clothing.infrastructure.file.StoredClothingImage;
import com.smartcloset.clothing.repository.ClothingItemRepository;
import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import com.smartcloset.user.domain.User;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
public class DefaultClothingPresetSeeder {

    private static final String IMAGE_CONTENT_TYPE = "image/jpeg";
    private static final String IMAGE_EXTENSION = "jpg";
    private static final String PRESET_RESOURCE_PREFIX = "classpath:default-clothing-presets/";
    private static final List<Preset> PRESETS = List.of(
            new Preset(
                    "화이트 반팔 티셔츠",
                    ClothingCategory.TOP,
                    ClothingColor.WHITE,
                    ClothingMaterial.COTTON,
                    8,
                    30,
                    false,
                    "white-short-sleeve-tshirt.jpg"
            ),
            new Preset(
                    "블랙 반팔 티셔츠",
                    ClothingCategory.TOP,
                    ClothingColor.BLACK,
                    ClothingMaterial.COTTON,
                    8,
                    30,
                    false,
                    "black-short-sleeve-tshirt.jpg"
            ),
            new Preset(
                    "흑청 데님 팬츠",
                    ClothingCategory.BOTTOM,
                    ClothingColor.BLACK,
                    ClothingMaterial.DENIM,
                    0,
                    28,
                    false,
                    "black-denim-jeans.jpg"
            ),
            new Preset(
                    "진청 데님 팬츠",
                    ClothingCategory.BOTTOM,
                    ClothingColor.BLUE,
                    ClothingMaterial.DENIM,
                    0,
                    28,
                    false,
                    "dark-blue-denim-jeans.jpg"
            ),
            new Preset(
                    "블랙 가디건",
                    ClothingCategory.OUTER,
                    ClothingColor.BLACK,
                    ClothingMaterial.KNIT,
                    8,
                    20,
                    false,
                    "black-cardigan.jpg"
            )
    );

    private final ClothingItemRepository clothingItemRepository;
    private final ClothingImageStorage clothingImageStorage;
    private final ResourceLoader resourceLoader;

    public DefaultClothingPresetSeeder(
            ClothingItemRepository clothingItemRepository,
            ClothingImageStorage clothingImageStorage,
            ResourceLoader resourceLoader
    ) {
        this.clothingItemRepository = clothingItemRepository;
        this.clothingImageStorage = clothingImageStorage;
        this.resourceLoader = resourceLoader;
    }

    public void seedIfEmpty(User user) {
        User requiredUser = Objects.requireNonNull(user, "user must not be null");
        if (clothingItemRepository.countByUserId(requiredUser.getId()) > 0) {
            return;
        }

        List<String> storedFilenames = new ArrayList<>();
        try {
            for (Preset preset : PRESETS) {
                byte[] imageBytes = readPresetImage(preset.imageFilename());
                StoredClothingImage storedImage = clothingImageStorage.store(imageBytes, IMAGE_EXTENSION);
                storedFilenames.add(storedImage.storedFilename());

                ClothingItem clothingItem = ClothingItem.create(
                        requiredUser,
                        preset.name(),
                        preset.category(),
                        preset.color(),
                        preset.material(),
                        preset.minTemperature(),
                        preset.maxTemperature(),
                        preset.rainSuitable()
                );
                clothingItem.updateImageMetadata(
                        storedImage.storedFilename(),
                        IMAGE_CONTENT_TYPE,
                        imageBytes.length,
                        LocalDateTime.now()
                );
                clothingItemRepository.save(clothingItem);
            }
            clothingItemRepository.flush();
        } catch (RuntimeException exception) {
            cleanupStoredImages(storedFilenames, exception);
            throw exception;
        }
    }

    private byte[] readPresetImage(String imageFilename) {
        Resource resource = resourceLoader.getResource(PRESET_RESOURCE_PREFIX + imageFilename);
        try (InputStream inputStream = resource.getInputStream()) {
            return inputStream.readAllBytes();
        } catch (IOException exception) {
            throw new SmartClosetException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void cleanupStoredImages(List<String> storedFilenames, RuntimeException originalException) {
        for (String storedFilename : storedFilenames) {
            try {
                clothingImageStorage.delete(storedFilename);
            } catch (RuntimeException cleanupException) {
                originalException.addSuppressed(cleanupException);
            }
        }
    }

    private record Preset(
            String name,
            ClothingCategory category,
            ClothingColor color,
            ClothingMaterial material,
            int minTemperature,
            int maxTemperature,
            boolean rainSuitable,
            String imageFilename
    ) {
    }
}

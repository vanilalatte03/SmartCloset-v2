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

/**
 * 새 계정이 바로 추천을 실험할 수 있도록 기본 옷과 이미지를 준비한다.
 *
 * <p>이미 옷이 있는 사용자는 seed하지 않고, 과거 기본 옷의 더운 날 온도 범위만 보정한다.</p>
 */
@Service
public class DefaultClothingPresetSeeder {

    private static final String IMAGE_CONTENT_TYPE = "image/jpeg";
    private static final String IMAGE_EXTENSION = "jpg";
    private static final String PRESET_RESOURCE_PREFIX = "classpath:default-clothing-presets/";
    private static final int HOT_WEATHER_MAX_TEMPERATURE = 35;
    private static final List<Preset> PRESETS = List.of(
            new Preset(
                    "화이트 반팔 티셔츠",
                    ClothingCategory.TOP,
                    ClothingColor.WHITE,
                    ClothingMaterial.COTTON,
                    8,
                    HOT_WEATHER_MAX_TEMPERATURE,
                    false,
                    List.of("CASUAL", "DAILY", "캐주얼"),
                    "white-short-sleeve-tshirt.jpg"
            ),
            new Preset(
                    "블랙 반팔 티셔츠",
                    ClothingCategory.TOP,
                    ClothingColor.BLACK,
                    ClothingMaterial.COTTON,
                    8,
                    HOT_WEATHER_MAX_TEMPERATURE,
                    false,
                    List.of("CASUAL", "MINIMAL", "미니멀"),
                    "black-short-sleeve-tshirt.jpg"
            ),
            new Preset(
                    "흑청 데님 팬츠",
                    ClothingCategory.BOTTOM,
                    ClothingColor.BLACK,
                    ClothingMaterial.DENIM,
                    0,
                    HOT_WEATHER_MAX_TEMPERATURE,
                    false,
                    List.of("CASUAL", "DAILY", "데일리"),
                    "black-denim-jeans.jpg"
            ),
            new Preset(
                    "진청 데님 팬츠",
                    ClothingCategory.BOTTOM,
                    ClothingColor.BLUE,
                    ClothingMaterial.DENIM,
                    0,
                    HOT_WEATHER_MAX_TEMPERATURE,
                    false,
                    List.of("CASUAL", "DAILY", "데일리"),
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
                    List.of("MINIMAL", "OFFICE", "미니멀"),
                    "black-cardigan.jpg"
            )
    );
    private static final List<LegacyTemperatureRangeUpgrade> LEGACY_HOT_WEATHER_UPGRADES = List.of(
            new LegacyTemperatureRangeUpgrade(
                    "화이트 반팔 티셔츠",
                    ClothingCategory.TOP,
                    ClothingColor.WHITE,
                    ClothingMaterial.COTTON,
                    8,
                    30
            ),
            new LegacyTemperatureRangeUpgrade(
                    "블랙 반팔 티셔츠",
                    ClothingCategory.TOP,
                    ClothingColor.BLACK,
                    ClothingMaterial.COTTON,
                    8,
                    30
            ),
            new LegacyTemperatureRangeUpgrade(
                    "흑청 데님 팬츠",
                    ClothingCategory.BOTTOM,
                    ClothingColor.BLACK,
                    ClothingMaterial.DENIM,
                    0,
                    28
            ),
            new LegacyTemperatureRangeUpgrade(
                    "진청 데님 팬츠",
                    ClothingCategory.BOTTOM,
                    ClothingColor.BLUE,
                    ClothingMaterial.DENIM,
                    0,
                    28
            )
    );

    private final ClothingItemRepository clothingItemRepository;
    private final ClothingImageStorage clothingImageStorage;
    private final ClothingImageCleanupScheduler clothingImageCleanupScheduler;
    private final ResourceLoader resourceLoader;
    private final ClothingStyleTagMapper clothingStyleTagMapper;

    public DefaultClothingPresetSeeder(
            ClothingItemRepository clothingItemRepository,
            ClothingImageStorage clothingImageStorage,
            ClothingImageCleanupScheduler clothingImageCleanupScheduler,
            ResourceLoader resourceLoader,
            ClothingStyleTagMapper clothingStyleTagMapper
    ) {
        this.clothingItemRepository = clothingItemRepository;
        this.clothingImageStorage = clothingImageStorage;
        this.clothingImageCleanupScheduler = clothingImageCleanupScheduler;
        this.resourceLoader = resourceLoader;
        this.clothingStyleTagMapper = clothingStyleTagMapper;
    }

    /**
     * 로그인/세션 복구 시에도 호출될 수 있으므로 멱등성을 유지한다.
     */
    public void seedIfEmpty(User user) {
        User requiredUser = Objects.requireNonNull(user, "user must not be null");
        if (clothingItemRepository.countByUserId(requiredUser.getId()) == 0) {
            seedDefaults(requiredUser);
            return;
        }

        upgradeLegacyHotWeatherRanges(requiredUser);
    }

    private void seedDefaults(User requiredUser) {
        List<String> storedFilenames = new ArrayList<>();
        try {
            for (Preset preset : PRESETS) {
                byte[] imageBytes = readPresetImage(preset.imageFilename());
                StoredClothingImage storedImage = clothingImageStorage.store(imageBytes, IMAGE_EXTENSION);
                storedFilenames.add(storedImage.storedFilename());
                clothingImageCleanupScheduler.deleteAfterRollback(storedImage.storedFilename());

                ClothingItem clothingItem = ClothingItem.create(
                        requiredUser,
                        preset.name(),
                        preset.category(),
                        preset.color(),
                        preset.material(),
                        preset.minTemperature(),
                        preset.maxTemperature(),
                        preset.rainSuitable(),
                        clothingStyleTagMapper.toJson(preset.styleTags())
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
            // DB 저장이 실패하면 이미 복사한 preset 이미지를 제거해 storage와 DB의 불일치를 줄인다.
            cleanupStoredImages(storedFilenames, exception);
            throw exception;
        }
    }

    private void upgradeLegacyHotWeatherRanges(User user) {
        clothingItemRepository.findByUserIdAndArchivedFalseOrderByIdAsc(user.getId()).stream()
                .filter(this::matchesLegacyHotWeatherRange)
                .forEach(item -> item.updateDetails(
                        item.getName(),
                        item.getCategory(),
                        item.getColor(),
                        item.getMaterial(),
                        item.getMinTemperature(),
                        HOT_WEATHER_MAX_TEMPERATURE,
                        item.isRainSuitable()
                ));
    }

    private boolean matchesLegacyHotWeatherRange(ClothingItem item) {
        return LEGACY_HOT_WEATHER_UPGRADES.stream().anyMatch(upgrade -> upgrade.matches(item));
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
            List<String> styleTags,
            String imageFilename
    ) {
    }

    private record LegacyTemperatureRangeUpgrade(
            String name,
            ClothingCategory category,
            ClothingColor color,
            ClothingMaterial material,
            int minTemperature,
            int maxTemperature
    ) {

        private boolean matches(ClothingItem item) {
            return item.getName().equals(name)
                    && item.getCategory() == category
                    && item.getColor() == color
                    && item.getMaterial() == material
                    && item.getMinTemperature() == minTemperature
                    && item.getMaxTemperature() == maxTemperature;
        }
    }
}

package com.smartcloset.clothing;

import static org.assertj.core.api.Assertions.assertThat;

import com.smartcloset.clothing.application.DefaultClothingPresetSeeder;
import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.domain.ClothingMaterial;
import com.smartcloset.clothing.repository.ClothingItemRepository;
import com.smartcloset.recommendation.domain.WeatherFilteredClothes;
import com.smartcloset.recommendation.domain.WeatherSuitabilityFilter;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import com.smartcloset.weather.domain.WeatherCondition;
import com.smartcloset.weather.domain.WeatherType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class DefaultClothingPresetSeederTest {

    private final WeatherSuitabilityFilter filter = new WeatherSuitabilityFilter();
    private final WeatherCondition hotWeather = WeatherCondition.of(30, WeatherType.SUNNY, false, false);

    @Autowired
    private DefaultClothingPresetSeeder defaultClothingPresetSeeder;

    @Autowired
    private ClothingItemRepository clothingItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void seedsDefaultClosetThatSupportsThirtyDegreeRecommendations() {
        User user = userRepository.save(User.createPasswordSignup("hot-default@example.com", "password-hash", "Hot Default"));

        defaultClothingPresetSeeder.seedIfEmpty(user);

        WeatherFilteredClothes filtered = filter.filter(activeClothes(user), hotWeather);
        assertThat(filtered.tops()).isNotEmpty();
        assertThat(filtered.bottoms()).isNotEmpty();
        assertThat(filtered.outers()).isEmpty();
    }

    @Test
    void upgradesLegacyDefaultClosetRangesThatBlockedThirtyDegreeRecommendations() {
        User user = userRepository.save(User.createPasswordSignup("legacy-hot@example.com", "password-hash", "Legacy Hot"));
        clothingItemRepository.saveAllAndFlush(List.of(
                ClothingItem.create(
                        user,
                        "화이트 반팔 티셔츠",
                        ClothingCategory.TOP,
                        ClothingColor.WHITE,
                        ClothingMaterial.COTTON,
                        8,
                        30,
                        false
                ),
                ClothingItem.create(
                        user,
                        "흑청 데님 팬츠",
                        ClothingCategory.BOTTOM,
                        ClothingColor.BLACK,
                        ClothingMaterial.DENIM,
                        0,
                        28,
                        false
                )
        ));

        defaultClothingPresetSeeder.seedIfEmpty(user);

        WeatherFilteredClothes filtered = filter.filter(activeClothes(user), hotWeather);
        assertThat(filtered.tops()).isNotEmpty();
        assertThat(filtered.bottoms()).isNotEmpty();
    }

    private List<ClothingItem> activeClothes(User user) {
        return clothingItemRepository.findByUserIdAndArchivedFalseOrderByIdAsc(user.getId());
    }
}

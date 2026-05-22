package com.smartcloset.common.config;

import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.domain.ClothingMaterial;
import com.smartcloset.clothing.repository.ClothingItemRepository;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SeedDataInitializer implements ApplicationRunner {

    private static final Long DEMO_USER_ID = 1L;

    private final UserRepository userRepository;
    private final ClothingItemRepository clothingItemRepository;

    public SeedDataInitializer(UserRepository userRepository, ClothingItemRepository clothingItemRepository) {
        this.userRepository = userRepository;
        this.clothingItemRepository = clothingItemRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User demoUser = userRepository.findById(DEMO_USER_ID)
                .orElseGet(() -> userRepository.save(User.createSeedUser("demo-user")));
        demoUser.ensureDefaultLocation();

        if (clothingItemRepository.countByUserId(demoUser.getId()) == 0) {
            seedClothes(demoUser);
        }
    }

    private void seedClothes(User user) {
        clothingItemRepository.save(ClothingItem.create(
                user,
                "아이보리 니트",
                ClothingCategory.TOP,
                ClothingColor.WHITE,
                ClothingMaterial.KNIT,
                0,
                16,
                false
        ));
        clothingItemRepository.save(ClothingItem.create(
                user,
                "블랙 데님",
                ClothingCategory.BOTTOM,
                ClothingColor.BLACK,
                ClothingMaterial.DENIM,
                0,
                22,
                false
        ));
        clothingItemRepository.save(ClothingItem.create(
                user,
                "네이비 코트",
                ClothingCategory.OUTER,
                ClothingColor.NAVY,
                ClothingMaterial.WOOL,
                -10,
                12,
                false
        ));
        clothingItemRepository.save(ClothingItem.create(
                user,
                "블랙 나일론 자켓",
                ClothingCategory.OUTER,
                ClothingColor.BLACK,
                ClothingMaterial.NYLON,
                5,
                18,
                true
        ));
        clothingItemRepository.save(ClothingItem.create(
                user,
                "베이지 치노",
                ClothingCategory.BOTTOM,
                ClothingColor.BEIGE,
                ClothingMaterial.COTTON,
                8,
                24,
                false
        ));
    }
}

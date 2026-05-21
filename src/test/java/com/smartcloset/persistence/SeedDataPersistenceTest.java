package com.smartcloset.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.repository.ClothingItemRepository;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class SeedDataPersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClothingItemRepository clothingItemRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void loadsDemoUserAndSeedClothes() {
        User demoUser = userRepository.findById(1L).orElseThrow();
        List<ClothingItem> clothes = clothingItemRepository.findByUserIdAndArchivedFalseOrderByIdAsc(1L);

        assertThat(demoUser.getName()).isEqualTo("demo-user");
        assertThat(clothes).hasSizeGreaterThanOrEqualTo(4);
        assertThat(clothes).isSortedAccordingTo((left, right) -> left.getId().compareTo(right.getId()));
        assertThat(clothes).allSatisfy(item -> {
            assertThat(item.isArchived()).isFalse();
            assertThat(item.getMinTemperature()).isLessThanOrEqualTo(12);
            assertThat(item.getMaxTemperature()).isGreaterThanOrEqualTo(12);
        });

        Set<ClothingCategory> categories = EnumSet.noneOf(ClothingCategory.class);
        clothes.forEach(item -> categories.add(item.getCategory()));
        assertThat(categories).contains(ClothingCategory.TOP, ClothingCategory.BOTTOM, ClothingCategory.OUTER);
    }

    @Test
    void auditingFillsCreatedAtAndUpdatedAt() {
        User user = userRepository.save(User.createSeedUser("audit-user"));

        entityManager.flush();
        entityManager.clear();

        User persisted = userRepository.findById(user.getId()).orElseThrow();

        assertThat(persisted.getCreatedAt()).isNotNull();
        assertThat(persisted.getUpdatedAt()).isNotNull();
    }
}

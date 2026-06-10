package com.smartcloset.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.repository.ClothingItemRepository;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:seed-disabled;MODE=MySQL;DATABASE_TO_LOWER=TRUE;"
                + "CASE_INSENSITIVE_IDENTIFIERS=TRUE"
})
@Transactional
class SeedDataPersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClothingItemRepository clothingItemRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void doesNotLoadDemoUserAndSeedClothesOutsideLocalOrDemoProfile() {
        List<ClothingItem> clothes = clothingItemRepository.findByUserIdAndArchivedFalseOrderByIdAsc(1L);

        assertThat(userRepository.findById(1L)).isEmpty();
        assertThat(clothes).isEmpty();
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

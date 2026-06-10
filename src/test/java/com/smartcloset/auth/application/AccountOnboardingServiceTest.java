package com.smartcloset.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloset.clothing.application.DefaultClothingPresetSeeder;
import com.smartcloset.clothing.repository.ClothingItemRepository;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@ActiveProfiles("test")
@SpringBootTest
class AccountOnboardingServiceTest {

    @Autowired
    private AccountOnboardingService accountOnboardingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClothingItemRepository clothingItemRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void seedsDefaultClothesAfterAccountTransactionCommits() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        AtomicLong userId = new AtomicLong();
        String email = "onboarding-after-commit-" + UUID.randomUUID() + "@example.com";

        transactionTemplate.executeWithoutResult(status -> {
            User user = userRepository.saveAndFlush(User.createPasswordSignup(
                    email,
                    "password-hash",
                    "Onboarding User"
            ));
            userId.set(user.getId());

            accountOnboardingService.seedDefaultClothesForNewAccountAfterCommit(user);

            assertThat(clothingItemRepository.countByUserId(user.getId())).isZero();
        });

        assertThat(clothingItemRepository.countByUserId(userId.get())).isEqualTo(5);
    }

    @Test
    void seedFailureDoesNotPropagateToCaller() {
        UserRepository userRepository = mock(UserRepository.class);
        DefaultClothingPresetSeeder defaultClothingPresetSeeder = mock(DefaultClothingPresetSeeder.class);
        AccountOnboardingService accountOnboardingService = new AccountOnboardingService(
                userRepository,
                defaultClothingPresetSeeder,
                new NoOpTransactionManager()
        );
        User user = User.createPasswordSignup("onboarding-failure@example.com", "password-hash", "Failure User");
        ReflectionTestUtils.setField(user, "id", 18001L);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        RuntimeException storageFailure = new RuntimeException("preset storage failed");
        org.mockito.Mockito.doThrow(storageFailure).when(defaultClothingPresetSeeder).seedIfEmpty(user);

        assertThatCode(() -> accountOnboardingService.seedDefaultClothesForNewAccountAfterCommit(user))
                .doesNotThrowAnyException();

        verify(defaultClothingPresetSeeder).seedIfEmpty(user);
    }

    private static class NoOpTransactionManager implements PlatformTransactionManager {

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }
}

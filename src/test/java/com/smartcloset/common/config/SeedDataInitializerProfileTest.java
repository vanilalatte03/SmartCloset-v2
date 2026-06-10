package com.smartcloset.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.smartcloset.clothing.repository.ClothingItemRepository;
import com.smartcloset.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SeedDataInitializerProfileTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(UserRepository.class, () -> mock(UserRepository.class))
            .withBean(ClothingItemRepository.class, () -> mock(ClothingItemRepository.class))
            .withUserConfiguration(SeedDataInitializer.class);

    @Test
    void createsInitializerWhenLocalProfileAndSeedEnabled() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=local",
                        "smartcloset.seed.enabled=true"
                )
                .run(context -> assertThat(context).hasSingleBean(SeedDataInitializer.class));
    }

    @Test
    void createsInitializerWhenDemoProfileAndSeedEnabled() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=demo",
                        "smartcloset.seed.enabled=true"
                )
                .run(context -> assertThat(context).hasSingleBean(SeedDataInitializer.class));
    }

    @Test
    void doesNotCreateInitializerWithoutLocalOrDemoProfileEvenWhenSeedEnabled() {
        contextRunner
                .withPropertyValues("smartcloset.seed.enabled=true")
                .run(context -> assertThat(context).doesNotHaveBean(SeedDataInitializer.class));
    }

    @Test
    void doesNotCreateInitializerForProdProfileEvenWhenSeedEnabled() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "smartcloset.seed.enabled=true"
                )
                .run(context -> assertThat(context).doesNotHaveBean(SeedDataInitializer.class));
    }

    @Test
    void doesNotCreateInitializerWhenLocalProfileDisablesSeed() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=local",
                        "smartcloset.seed.enabled=false"
                )
                .run(context -> assertThat(context).doesNotHaveBean(SeedDataInitializer.class));
    }
}

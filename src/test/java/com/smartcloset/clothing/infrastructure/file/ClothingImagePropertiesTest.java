package com.smartcloset.clothing.infrastructure.file;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ClothingImagePropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ClothingImagePropertiesConfig.class);

    @Test
    void usesDocumentedDefaults() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ClothingImageProperties.class);

            ClothingImageProperties properties = context.getBean(ClothingImageProperties.class);

            assertThat(properties.storageDir()).isEqualTo(ClothingImageProperties.DEFAULT_STORAGE_DIR);
            assertThat(properties.maxSizeBytes()).isEqualTo(ClothingImageProperties.DEFAULT_MAX_SIZE_BYTES);
        });
    }

    @Test
    void bindsConfiguredValues() {
        contextRunner
                .withPropertyValues(
                        "smartcloset.clothing.image.storage-dir=/tmp/smartcloset-images",
                        "smartcloset.clothing.image.max-size-bytes=1024"
                )
                .run(context -> {
                    ClothingImageProperties properties = context.getBean(ClothingImageProperties.class);

                    assertThat(properties.storageDir()).isEqualTo("/tmp/smartcloset-images");
                    assertThat(properties.maxSizeBytes()).isEqualTo(1024L);
                });
    }
}

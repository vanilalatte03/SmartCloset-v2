package com.smartcloset.clothing.infrastructure.file;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ClothingImageProperties.class)
public class ClothingImagePropertiesConfig {
}

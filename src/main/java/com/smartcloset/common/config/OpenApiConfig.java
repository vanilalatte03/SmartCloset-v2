package com.smartcloset.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "SmartCloset API",
                version = "1.0.0",
                description = "SmartCloset 1차 MVP API"
        )
)
public class OpenApiConfig {
}

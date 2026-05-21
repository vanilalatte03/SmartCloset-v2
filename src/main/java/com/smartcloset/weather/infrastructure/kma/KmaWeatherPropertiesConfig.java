package com.smartcloset.weather.infrastructure.kma;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KmaWeatherProperties.class)
public class KmaWeatherPropertiesConfig {
}

package com.smartcloset.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smartcloset.security.cors")
public record CorsProperties(List<String> allowedOrigins, boolean allowCredentials) {

    public CorsProperties {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            allowedOrigins = List.of("http://localhost:5173", "http://127.0.0.1:5173");
        }
    }
}

package com.smartcloset.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Frontend origin과 credential 허용 여부를 환경별로 바인딩하는 CORS 설정이다.
 *
 * <p>기본값은 local Vite frontend에서 refresh cookie를 포함한 요청을 보낼 수 있게 한다.</p>
 */
@ConfigurationProperties(prefix = "smartcloset.security.cors")
public record CorsProperties(List<String> allowedOrigins, boolean allowCredentials) {

    public CorsProperties {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            allowedOrigins = List.of("http://localhost:5173", "http://127.0.0.1:5173");
        }
    }
}

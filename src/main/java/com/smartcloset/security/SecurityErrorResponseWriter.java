package com.smartcloset.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Spring Security filter 단계에서 발생한 인증/인가 실패도 일반 API와 같은 error JSON 형태로 내려준다.
 */
@Component
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public SecurityErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Security filter 단계에서 직접 status와 공통 error JSON body를 response에 쓴다.
     */
    public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(errorCode.name(), errorCode.message()));
    }
}

package com.smartcloset.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Spring Security filter 단계에서 발생한 인증/인가 실패도 일반 API와 같은 error JSON 형태로 내려준다.
 */
@Component
public class SecurityErrorResponseWriter {

    private static final Logger log = LoggerFactory.getLogger(SecurityErrorResponseWriter.class);

    private final ObjectMapper objectMapper;

    public SecurityErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Security filter 단계에서 직접 status와 공통 error JSON body를 response에 쓴다.
     */
    public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        write(null, response, errorCode, null);
    }

    /**
     * Security filter 단계에서 request 정보와 함께 공통 error JSON body를 response에 쓴다.
     */
    public void write(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode)
            throws IOException {
        write(request, response, errorCode, null);
    }

    /**
     * Security filter 단계에서 request와 exception 정보를 로그로 남기고 공통 error JSON body를 response에 쓴다.
     */
    public void write(
            HttpServletRequest request,
            HttpServletResponse response,
            ErrorCode errorCode,
            Throwable exception
    ) throws IOException {
        logSecurityError(request, errorCode, exception);
        response.setStatus(errorCode.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(errorCode.name(), errorCode.message()));
    }

    private void logSecurityError(HttpServletRequest request, ErrorCode errorCode, Throwable exception) {
        String method = request == null ? "-" : request.getMethod();
        String path = request == null ? "-" : request.getRequestURI();
        String exceptionType = exception == null ? "none" : exception.getClass().getName();
        String message = errorCode.message();
        log.warn(
                "security_error code={} status={} method={} path={} exception={} message={}",
                errorCode.name(),
                errorCode.status().value(),
                method,
                path,
                exceptionType,
                message
        );
    }
}

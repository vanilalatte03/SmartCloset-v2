package com.smartcloset.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloset.common.exception.ErrorCode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(OutputCaptureExtension.class)
class SecurityErrorResponseWriterTest {

    @Test
    void logsSecurityFailureWithoutSensitiveRequestValues(CapturedOutput output) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/recommendations");
        request.setQueryString("token=SECRET_QUERY_VALUE");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer SECRET_ACCESS_TOKEN");
        request.setCookies(new Cookie("smartcloset.refreshToken", "SECRET_REFRESH_TOKEN"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        new SecurityErrorResponseWriter(new ObjectMapper())
                .write(request, response, ErrorCode.UNAUTHORIZED, new IllegalStateException("SECRET_EXCEPTION"));

        assertThat(response.getStatus()).isEqualTo(ErrorCode.UNAUTHORIZED.status().value());
        assertThat(output)
                .contains("security_error")
                .contains("UNAUTHORIZED")
                .contains("error_message")
                .contains("/api/recommendations")
                .doesNotContain("SECRET_QUERY_VALUE")
                .doesNotContain("SECRET_ACCESS_TOKEN")
                .doesNotContain("SECRET_REFRESH_TOKEN")
                .doesNotContain("SECRET_EXCEPTION");
    }
}

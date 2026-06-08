package com.smartcloset.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloset.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(OutputCaptureExtension.class)
class SecurityErrorResponseWriterTest {

    @Test
    void doesNotLogRawSecurityExceptionMessage(CapturedOutput output) throws Exception {
        SecurityErrorResponseWriter writer = new SecurityErrorResponseWriter(new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/clothes");
        MockHttpServletResponse response = new MockHttpServletResponse();
        JwtTokenException exception = JwtTokenException.invalid("raw bearer token SECRET_TOKEN_VALUE");

        writer.write(request, response, ErrorCode.INVALID_TOKEN, exception);

        assertThat(response.getStatus()).isEqualTo(ErrorCode.INVALID_TOKEN.status().value());
        assertThat(output)
                .contains("security_error")
                .contains("INVALID_TOKEN")
                .doesNotContain("SECRET_TOKEN_VALUE")
                .doesNotContain("raw bearer token");
    }
}

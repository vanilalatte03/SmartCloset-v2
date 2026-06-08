package com.smartcloset.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void mapsIllegalArgumentExceptionToIllegalArgument() throws Exception {
        mockMvc.perform(get("/test/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ILLEGAL_ARGUMENT"))
                .andExpect(jsonPath("$.message").value("요청 인자가 올바르지 않습니다."))
                .andExpect(jsonPath("$.details[0].message").value("name must not be blank"));
    }

    @Test
    void mapsRequestBodyValidationToMethodArgumentNotValid() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("METHOD_ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.details[0].field").value("name"));
    }

    @Test
    void mapsInvalidRequestBodyEnumToInvalidFormat() throws Exception {
        mockMvc.perform(post("/test/enum-body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"UNKNOWN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FORMAT"))
                .andExpect(jsonPath("$.details[0].field").value("mode"))
                .andExpect(jsonPath("$.details[0].message").value("UNKNOWN"));
    }

    @Test
    void mapsInvalidRequestParamEnumToMethodArgumentTypeMismatch() throws Exception {
        mockMvc.perform(get("/test/enum-query").param("mode", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("METHOD_ARGUMENT_TYPE_MISMATCH"))
                .andExpect(jsonPath("$.details[0].field").value("mode"))
                .andExpect(jsonPath("$.details[0].message").value("UNKNOWN"));
    }

    @Test
    void mapsMissingRequestParamToMissingServletRequestParameter() throws Exception {
        mockMvc.perform(get("/test/missing"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_SERVLET_REQUEST_PARAMETER"))
                .andExpect(jsonPath("$.details[0].field").value("name"));
    }

    @Test
    void doesNotLogRawRequestValues(CapturedOutput output) throws Exception {
        mockMvc.perform(post("/test/enum-body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"SECRET_BODY_VALUE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FORMAT"))
                .andExpect(jsonPath("$.details[0].message").value("SECRET_BODY_VALUE"));

        mockMvc.perform(get("/test/enum-query").param("mode", "SECRET_QUERY_VALUE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("METHOD_ARGUMENT_TYPE_MISMATCH"))
                .andExpect(jsonPath("$.details[0].message").value("SECRET_QUERY_VALUE"));

        assertThat(output)
                .contains("api_error")
                .contains("INVALID_FORMAT")
                .contains("METHOD_ARGUMENT_TYPE_MISMATCH")
                .doesNotContain("SECRET_BODY_VALUE")
                .doesNotContain("SECRET_QUERY_VALUE");
    }

    @RestController
    private static class TestController {

        @GetMapping("/test/illegal-argument")
        String illegalArgument() {
            throw new IllegalArgumentException("name must not be blank");
        }

        @PostMapping("/test/validation")
        String validation(@Valid @RequestBody TestRequest request) {
            return request.name();
        }

        @PostMapping("/test/enum-body")
        String enumBody(@RequestBody EnumRequest request) {
            return request.mode().name();
        }

        @GetMapping("/test/enum-query")
        String enumQuery(@RequestParam TestMode mode) {
            return mode.name();
        }

        @GetMapping("/test/missing")
        String missing(@RequestParam String name) {
            return name;
        }
    }

    private record TestRequest(@NotBlank String name) {
    }

    private record EnumRequest(TestMode mode) {
    }

    private enum TestMode {
        BASIC
    }
}

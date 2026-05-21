package com.smartcloset.common.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class ErrorResponseTest {

    @Test
    void createsErrorWithoutDetails() {
        ErrorResponse response = ErrorResponse.of("INVALID_REQUEST", "요청 값이 올바르지 않습니다.");

        assertThat(response.code()).isEqualTo("INVALID_REQUEST");
        assertThat(response.message()).isEqualTo("요청 값이 올바르지 않습니다.");
        assertThat(response.details()).isEmpty();
    }

    @Test
    void protectsDetailsFromExternalMutation() {
        ErrorDetail detail = ErrorDetail.of("name", "blank 불가");
        ErrorResponse response = ErrorResponse.of("INVALID_REQUEST", "요청 값이 올바르지 않습니다.", List.of(detail));

        assertThat(response.details()).containsExactly(detail);
        assertThatThrownBy(() -> response.details().add(ErrorDetail.of("category", "required")))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}

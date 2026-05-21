package com.smartcloset.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void wrapsData() {
        ApiResponse<Long> response = ApiResponse.of(1L);

        assertThat(response.data()).isEqualTo(1L);
    }
}

package com.smartcloset.common.response;

import java.util.List;

public record ErrorResponse(String code, String message, List<ErrorDetail> details) {

    public ErrorResponse {
        details = details == null ? List.of() : List.copyOf(details);
    }

    /**
     * field detail이 없는 실패 응답을 현재 API error shape로 생성한다.
     */
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, List.of());
    }

    /**
     * field detail 목록을 포함한 실패 응답을 현재 API error shape로 생성한다.
     */
    public static ErrorResponse of(String code, String message, List<ErrorDetail> details) {
        return new ErrorResponse(code, message, details);
    }
}

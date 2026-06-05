package com.smartcloset.common.response;

public record ErrorDetail(String field, String message) {

    /**
     * validation 실패 field와 사용자 표시 메시지를 담는 detail 항목을 생성한다.
     */
    public static ErrorDetail of(String field, String message) {
        return new ErrorDetail(field, message);
    }
}

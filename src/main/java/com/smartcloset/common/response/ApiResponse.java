package com.smartcloset.common.response;

public record ApiResponse<T>(T data) {

    /**
     * 성공 응답의 최상위 data wrapper를 생성한다.
     */
    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data);
    }
}

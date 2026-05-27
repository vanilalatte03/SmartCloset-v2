package com.smartcloset.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "인증 토큰이 올바르지 않습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    EMAIL_VERIFICATION_REQUIRED(HttpStatus.FORBIDDEN, "이메일 인증 후 로그인할 수 있습니다."),
    ACCOUNT_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "계정 인증 토큰이 올바르지 않습니다."),
    PASSWORD_LOGIN_DISABLED(HttpStatus.BAD_REQUEST, "비밀번호 로그인을 사용할 수 없는 계정입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    LOCATION_NOT_FOUND(HttpStatus.NOT_FOUND, "위치를 찾을 수 없습니다."),
    CLOTHING_NOT_FOUND(HttpStatus.NOT_FOUND, "옷을 찾을 수 없습니다."),
    CLOTHING_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "옷 이미지를 찾을 수 없습니다."),
    RECOMMENDATION_NOT_FOUND(HttpStatus.NOT_FOUND, "추천 결과를 찾을 수 없습니다."),
    NO_TOP_AVAILABLE(HttpStatus.UNPROCESSABLE_CONTENT, "현재 날씨에 입을 수 있는 상의가 없습니다."),
    NO_BOTTOM_AVAILABLE(HttpStatus.UNPROCESSABLE_CONTENT, "현재 날씨에 입을 수 있는 하의가 없습니다."),
    NO_WEATHER_SUITABLE_ITEM(HttpStatus.UNPROCESSABLE_CONTENT, "현재 기온에 맞는 옷이 없습니다."),
    OUTER_REQUIRED_BUT_NOT_AVAILABLE(HttpStatus.UNPROCESSABLE_CONTENT, "현재 기온에는 아우터가 필요하지만 추천 가능한 아우터가 없습니다."),
    INSUFFICIENT_CLOSET_ITEMS(HttpStatus.UNPROCESSABLE_CONTENT, "추천을 만들기 위해 옷을 더 등록해주세요."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "예상하지 못한 서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}

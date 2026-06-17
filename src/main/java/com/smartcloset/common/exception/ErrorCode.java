package com.smartcloset.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 공개 API error response에 노출되는 안정적인 오류 코드와 기본 HTTP status/message registry다.
 *
 * <p>Controller와 service는 임의 문자열 대신 이 enum을 통해 현재 API error shape를 유지한다.</p>
 */
public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    METHOD_ARGUMENT_NOT_VALID(HttpStatus.BAD_REQUEST, "요청 본문 검증에 실패했습니다."),
    HANDLER_METHOD_VALIDATION(HttpStatus.BAD_REQUEST, "요청 메서드 파라미터 검증에 실패했습니다."),
    CONSTRAINT_VIOLATION(HttpStatus.BAD_REQUEST, "요청 제약 조건 검증에 실패했습니다."),
    MISSING_SERVLET_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "필수 요청 파라미터가 누락되었습니다."),
    MISSING_SERVLET_REQUEST_PART(HttpStatus.BAD_REQUEST, "필수 multipart part가 누락되었습니다."),
    METHOD_ARGUMENT_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "요청 파라미터 타입이 올바르지 않습니다."),
    HTTP_MESSAGE_NOT_READABLE(HttpStatus.BAD_REQUEST, "요청 본문을 읽을 수 없습니다."),
    INVALID_FORMAT(HttpStatus.BAD_REQUEST, "요청 본문 값 형식이 올바르지 않습니다."),
    ILLEGAL_ARGUMENT(HttpStatus.BAD_REQUEST, "요청 인자가 올바르지 않습니다."),
    MAX_UPLOAD_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "업로드 가능한 파일 크기를 초과했습니다."),
    MULTIPART_EXCEPTION(HttpStatus.BAD_REQUEST, "multipart 요청이 올바르지 않습니다."),
    INVALID_PAGINATION(HttpStatus.BAD_REQUEST, "페이지 번호 또는 크기가 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "인증 토큰이 올바르지 않습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    EMAIL_VERIFICATION_REQUIRED(HttpStatus.FORBIDDEN, "이메일 인증 후 로그인할 수 있습니다."),
    ACCOUNT_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "계정 인증 토큰이 올바르지 않습니다."),
    PASSWORD_LOGIN_DISABLED(HttpStatus.BAD_REQUEST, "비밀번호 로그인을 사용할 수 없는 계정입니다."),
    LOGIN_ATTEMPT_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해주세요."),
    OAUTH2_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "OAuth2 제공자를 사용할 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    LOCATION_NOT_FOUND(HttpStatus.NOT_FOUND, "위치를 찾을 수 없습니다."),
    CLOTHING_NOT_FOUND(HttpStatus.NOT_FOUND, "옷을 찾을 수 없습니다."),
    CLOTHING_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "옷 이미지를 찾을 수 없습니다."),
    CLOTHING_ANALYSIS_DISABLED(HttpStatus.SERVICE_UNAVAILABLE, "옷 사진 분석 기능을 사용할 수 없습니다."),
    CLOTHING_ANALYSIS_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "옷 사진 분석 제공자를 사용할 수 없습니다."),
    CLOTHING_ANALYSIS_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "오늘의 옷 사진 분석 횟수를 초과했습니다."),
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

    /**
     * 이 error code가 API 응답에 사용할 HTTP status를 반환한다.
     */
    public HttpStatus status() {
        return status;
    }

    /**
     * 이 error code의 기본 사용자 표시 메시지를 반환한다.
     */
    public String message() {
        return message;
    }
}

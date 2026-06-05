package com.smartcloset.common.exception;

import com.smartcloset.common.response.ErrorDetail;
import java.util.List;

/**
 * 도메인/애플리케이션 계층에서 이미 결정된 {@link ErrorCode}와 field detail을 함께 전달하는 예외다.
 */
public class SmartClosetException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<ErrorDetail> details;

    public SmartClosetException(ErrorCode errorCode) {
        this(errorCode, errorCode.message(), List.of());
    }

    public SmartClosetException(ErrorCode errorCode, String message) {
        this(errorCode, message, List.of());
    }

    public SmartClosetException(ErrorCode errorCode, String message, List<ErrorDetail> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    /**
     * controller advice가 HTTP status와 code를 결정할 때 사용하는 ErrorCode를 반환한다.
     */
    public ErrorCode errorCode() {
        return errorCode;
    }

    /**
     * validation 또는 business failure의 field detail 목록을 반환한다.
     */
    public List<ErrorDetail> details() {
        return details;
    }
}

package com.smartcloset.common.exception;

import com.smartcloset.common.response.ErrorDetail;
import java.util.List;

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

    public ErrorCode errorCode() {
        return errorCode;
    }

    public List<ErrorDetail> details() {
        return details;
    }
}

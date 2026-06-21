package com.smartcloset.common.exception;

import com.smartcloset.common.response.ErrorDetail;
import com.smartcloset.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;

/**
 * Controller 바깥으로 나온 validation, business, multipart, unexpected 오류를 API error shape로 변환한다.
 *
 * <p>모든 실패 응답은 code/message/details 구조를 유지한다.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 도메인/애플리케이션 계층에서 던진 SmartClosetException은 이미 ErrorCode가 결정된 상태다.
     */
    @ExceptionHandler(SmartClosetException.class)
    public ResponseEntity<ErrorResponse> handleSmartClosetException(
            SmartClosetException exception,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = exception.errorCode();
        return errorResponse(errorCode, exception.getMessage(), exception.details(), exception, request);
    }

    /**
     * Request body DTO validation 실패를 field별 METHOD_ARGUMENT_NOT_VALID details로 변환한다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<ErrorDetail> details = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toErrorDetail)
                .toList();
        return errorResponse(ErrorCode.METHOD_ARGUMENT_NOT_VALID, details, exception, request);
    }

    /**
     * controller method parameter validation 실패를 HANDLER_METHOD_VALIDATION details로 변환한다.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(
            HandlerMethodValidationException exception,
            HttpServletRequest request
    ) {
        List<ErrorDetail> details = exception.getParameterValidationResults()
                .stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> ErrorDetail.of(
                                result.getMethodParameter().getParameterName(),
                                error.getDefaultMessage())))
                .toList();
        return errorResponse(ErrorCode.HANDLER_METHOD_VALIDATION, details, exception, request);
    }

    /**
     * Bean Validation constraint violation을 property path 기반 CONSTRAINT_VIOLATION details로 변환한다.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        List<ErrorDetail> details = exception.getConstraintViolations()
                .stream()
                .map(violation -> ErrorDetail.of(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()))
                .toList();
        return errorResponse(ErrorCode.CONSTRAINT_VIOLATION, details, exception, request);
    }

    /**
     * 필수 request parameter 누락을 MISSING_SERVLET_REQUEST_PARAMETER로 변환한다.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request
    ) {
        return errorResponse(ErrorCode.MISSING_SERVLET_REQUEST_PARAMETER, List.of(ErrorDetail.of(
                exception.getParameterName(),
                exception.getMessage())), exception, request);
    }

    /**
     * 필수 multipart part 누락을 MISSING_SERVLET_REQUEST_PART로 변환한다.
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestPart(
            MissingServletRequestPartException exception,
            HttpServletRequest request
    ) {
        return errorResponse(ErrorCode.MISSING_SERVLET_REQUEST_PART, List.of(ErrorDetail.of(
                exception.getRequestPartName(),
                exception.getMessage())), exception, request);
    }

    /**
     * query/path parameter type mismatch를 METHOD_ARGUMENT_TYPE_MISMATCH로 변환한다.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        return errorResponse(ErrorCode.METHOD_ARGUMENT_TYPE_MISMATCH, List.of(ErrorDetail.of(
                exception.getName(),
                String.valueOf(exception.getValue()))), exception, request);
    }

    /**
     * 읽을 수 없는 JSON 요청을 Jackson cause와 일반 JSON 오류로 나눠 변환한다.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        InvalidFormatException invalidFormatException = findCause(exception, InvalidFormatException.class);
        if (invalidFormatException != null) {
            return errorResponse(ErrorCode.INVALID_FORMAT, List.of(ErrorDetail.of(
                    jsonPath(invalidFormatException),
                    String.valueOf(invalidFormatException.getValue()))), exception, request);
        }
        return errorResponse(
                ErrorCode.HTTP_MESSAGE_NOT_READABLE,
                List.of(ErrorDetail.of(null, exception.getMessage())),
                exception,
                request
        );
    }

    /**
     * 도메인 객체 생성이나 service 입력 검증에서 발생한 IllegalArgumentException을 ILLEGAL_ARGUMENT로 변환한다.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return errorResponse(ErrorCode.ILLEGAL_ARGUMENT, detailOrEmpty(exception.getMessage()), exception, request);
    }

    /**
     * multipart 업로드 실패를 MaxUploadSizeExceededException과 MultipartException으로 나눠 변환한다.
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipartException(
            MultipartException exception,
            HttpServletRequest request
    ) {
        if (exception instanceof MaxUploadSizeExceededException) {
            return errorResponse(
                    ErrorCode.MAX_UPLOAD_SIZE_EXCEEDED,
                    List.of(ErrorDetail.of("image", "이미지 파일은 5MB 이하여야 합니다.")),
                    exception,
                    request
            );
        }
        return errorResponse(
                ErrorCode.MULTIPART_EXCEPTION,
                List.of(ErrorDetail.of("image", "이미지 업로드 요청이 올바르지 않습니다.")),
                exception,
                request
        );
    }

    /**
     * 예측하지 못한 예외의 내부 정보를 숨기고 안정적인 INTERNAL_SERVER_ERROR 응답만 내려준다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return errorResponse(errorCode, exception, request);
    }

    private ErrorDetail toErrorDetail(FieldError fieldError) {
        return ErrorDetail.of(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private List<ErrorDetail> detailOrEmpty(String message) {
        if (message == null || message.isBlank()) {
            return List.of();
        }
        return List.of(ErrorDetail.of(null, message));
    }

    private <T extends Throwable> T findCause(Throwable exception, Class<T> causeType) {
        Throwable current = exception;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return causeType.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }

    private String jsonPath(JacksonException exception) {
        List<String> paths = new ArrayList<>();
        for (JacksonException.Reference reference : exception.getPath()) {
            if (reference.getPropertyName() != null) {
                paths.add(reference.getPropertyName());
            } else if (reference.getIndex() >= 0) {
                paths.add("[" + reference.getIndex() + "]");
            }
        }
        return paths.isEmpty() ? null : String.join(".", paths);
    }

    private ResponseEntity<ErrorResponse> errorResponse(ErrorCode errorCode) {
        return errorResponse(errorCode, errorCode.message(), List.of());
    }

    private ResponseEntity<ErrorResponse> errorResponse(
            ErrorCode errorCode,
            Throwable exception,
            HttpServletRequest request
    ) {
        return errorResponse(errorCode, errorCode.message(), List.of(), exception, request);
    }

    private ResponseEntity<ErrorResponse> errorResponse(ErrorCode errorCode, List<ErrorDetail> details) {
        return errorResponse(errorCode, errorCode.message(), details);
    }

    private ResponseEntity<ErrorResponse> errorResponse(
            ErrorCode errorCode,
            List<ErrorDetail> details,
            Throwable exception,
            HttpServletRequest request
    ) {
        return errorResponse(errorCode, errorCode.message(), details, exception, request);
    }

    private ResponseEntity<ErrorResponse> errorResponse(ErrorCode errorCode, String message, List<ErrorDetail> details) {
        return errorResponse(errorCode, message, details, null, null);
    }

    private ResponseEntity<ErrorResponse> errorResponse(
            ErrorCode errorCode,
            String message,
            List<ErrorDetail> details,
            Throwable exception,
            HttpServletRequest request
    ) {
        logHandledException(errorCode, exception, request);
        return ResponseEntity
                .status(errorCode.status())
                .body(ErrorResponse.of(errorCode.name(), message, details));
    }

    private void logHandledException(ErrorCode errorCode, Throwable exception, HttpServletRequest request) {
        String method = request == null ? "-" : request.getMethod();
        String path = request == null ? "-" : request.getRequestURI();
        String exceptionType = exception == null ? "none" : exception.getClass().getName();
        String message = errorCode.message();

        if (errorCode.status().is5xxServerError()) {
            log.atError()
                    .setMessage("api_error")
                    .addKeyValue("code", errorCode.name())
                    .addKeyValue("status", errorCode.status().value())
                    .addKeyValue("method", method)
                    .addKeyValue("path", path)
                    .addKeyValue("exception", exceptionType)
                    .addKeyValue("error_message", message)
                    .log();
            return;
        }
        log.atWarn()
                .setMessage("api_error")
                .addKeyValue("code", errorCode.name())
                .addKeyValue("status", errorCode.status().value())
                .addKeyValue("method", method)
                .addKeyValue("path", path)
                .addKeyValue("exception", exceptionType)
                .addKeyValue("error_message", message)
                .log();
    }
}

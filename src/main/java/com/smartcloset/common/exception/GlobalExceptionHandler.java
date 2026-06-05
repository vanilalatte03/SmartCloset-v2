package com.smartcloset.common.exception;

import com.smartcloset.common.response.ErrorDetail;
import com.smartcloset.common.response.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
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

/**
 * Controller 바깥으로 나온 validation, business, multipart, unexpected 오류를 API error shape로 변환한다.
 *
 * <p>모든 실패 응답은 code/message/details 구조를 유지한다.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 도메인/애플리케이션 계층에서 던진 SmartClosetException은 이미 ErrorCode가 결정된 상태다.
     */
    @ExceptionHandler(SmartClosetException.class)
    public ResponseEntity<ErrorResponse> handleSmartClosetException(SmartClosetException exception) {
        ErrorCode errorCode = exception.errorCode();
        return ResponseEntity
                .status(errorCode.status())
                .body(ErrorResponse.of(errorCode.name(), exception.getMessage(), exception.details()));
    }

    /**
     * Request body DTO validation 실패를 field별 INVALID_REQUEST details로 변환한다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        List<ErrorDetail> details = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toErrorDetail)
                .toList();
        return invalidRequest(details);
    }

    /**
     * controller method parameter validation 실패를 INVALID_REQUEST details로 변환한다.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(HandlerMethodValidationException exception) {
        List<ErrorDetail> details = exception.getParameterValidationResults()
                .stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> ErrorDetail.of(
                                result.getMethodParameter().getParameterName(),
                                error.getDefaultMessage())))
                .toList();
        return invalidRequest(details);
    }

    /**
     * Bean Validation constraint violation을 property path 기반 INVALID_REQUEST details로 변환한다.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        List<ErrorDetail> details = exception.getConstraintViolations()
                .stream()
                .map(violation -> ErrorDetail.of(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()))
                .toList();
        return invalidRequest(details);
    }

    /**
     * 누락된 parameter, 잘못된 type, 읽을 수 없는 JSON 요청을 INVALID_REQUEST로 변환한다.
     */
    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidRequest(Exception exception) {
        return invalidRequest(List.of(ErrorDetail.of(null, exception.getMessage())));
    }

    /**
     * multipart 업로드 실패를 이미지 field 기준 INVALID_REQUEST로 변환한다.
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipartException(MultipartException exception) {
        if (exception instanceof MaxUploadSizeExceededException) {
            return invalidRequest(List.of(ErrorDetail.of("image", "이미지 파일은 5MB 이하여야 합니다.")));
        }
        return invalidRequest(List.of(ErrorDetail.of("image", "이미지 업로드 요청이 올바르지 않습니다.")));
    }

    /**
     * 예측하지 못한 예외의 내부 정보를 숨기고 안정적인 INTERNAL_SERVER_ERROR 응답만 내려준다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception exception) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(errorCode.status())
                .body(ErrorResponse.of(errorCode.name(), errorCode.message()));
    }

    private ErrorDetail toErrorDetail(FieldError fieldError) {
        return ErrorDetail.of(fieldError.getField(), fieldError.getDefaultMessage());
    }

    /**
     * validation 계열 실패는 details 배열을 유지해 프론트에서 field별 메시지를 표시할 수 있게 한다.
     */
    private ResponseEntity<ErrorResponse> invalidRequest(List<ErrorDetail> details) {
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        return ResponseEntity
                .status(errorCode.status())
                .body(ErrorResponse.of(errorCode.name(), errorCode.message(), details));
    }
}

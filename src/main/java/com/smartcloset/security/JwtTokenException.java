package com.smartcloset.security;

public class JwtTokenException extends RuntimeException {

    private final JwtTokenFailureReason reason;

    private JwtTokenException(JwtTokenFailureReason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    /**
     * 서명, claim, 형식 오류처럼 재시도로 해결되지 않는 access token 실패를 만든다.
     */
    public static JwtTokenException invalid(String message) {
        return new JwtTokenException(JwtTokenFailureReason.INVALID, message, null);
    }

    /**
     * 원인 예외를 함께 보존해야 하는 invalid access token 실패를 만든다.
     */
    public static JwtTokenException invalid(String message, Throwable cause) {
        return new JwtTokenException(JwtTokenFailureReason.INVALID, message, cause);
    }

    /**
     * 만료된 access token 실패를 만든다.
     */
    public static JwtTokenException expired() {
        return new JwtTokenException(JwtTokenFailureReason.EXPIRED, "JWT access token has expired", null);
    }

    /**
     * token 실패 원인을 내부 분기에서 사용할 수 있도록 반환한다.
     */
    public JwtTokenFailureReason reason() {
        return reason;
    }
}

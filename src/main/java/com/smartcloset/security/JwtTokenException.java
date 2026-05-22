package com.smartcloset.security;

public class JwtTokenException extends RuntimeException {

    private final JwtTokenFailureReason reason;

    private JwtTokenException(JwtTokenFailureReason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public static JwtTokenException invalid(String message) {
        return new JwtTokenException(JwtTokenFailureReason.INVALID, message, null);
    }

    public static JwtTokenException invalid(String message, Throwable cause) {
        return new JwtTokenException(JwtTokenFailureReason.INVALID, message, cause);
    }

    public static JwtTokenException expired() {
        return new JwtTokenException(JwtTokenFailureReason.EXPIRED, "JWT access token has expired", null);
    }

    public JwtTokenFailureReason reason() {
        return reason;
    }
}

package com.smartcloset.auth.dto;

public record SignupResponse(
        String email,
        boolean emailVerificationRequired,
        String message
) {

    public static SignupResponse emailVerificationRequired(String email) {
        return new SignupResponse(email, true, "이메일 인증 후 로그인할 수 있습니다.");
    }
}

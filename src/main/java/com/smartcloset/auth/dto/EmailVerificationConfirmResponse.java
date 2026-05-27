package com.smartcloset.auth.dto;

public record EmailVerificationConfirmResponse(boolean emailVerified) {

    public static EmailVerificationConfirmResponse success() {
        return new EmailVerificationConfirmResponse(true);
    }
}

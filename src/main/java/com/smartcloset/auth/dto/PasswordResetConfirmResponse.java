package com.smartcloset.auth.dto;

public record PasswordResetConfirmResponse(boolean passwordReset) {

    public static PasswordResetConfirmResponse success() {
        return new PasswordResetConfirmResponse(true);
    }
}

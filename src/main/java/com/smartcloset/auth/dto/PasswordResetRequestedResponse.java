package com.smartcloset.auth.dto;

public record PasswordResetRequestedResponse(boolean requested) {

    public static PasswordResetRequestedResponse success() {
        return new PasswordResetRequestedResponse(true);
    }
}

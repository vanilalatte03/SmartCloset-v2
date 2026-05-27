package com.smartcloset.auth.dto;

public record EmailVerificationRequestedResponse(boolean requested) {

    public static EmailVerificationRequestedResponse success() {
        return new EmailVerificationRequestedResponse(true);
    }
}

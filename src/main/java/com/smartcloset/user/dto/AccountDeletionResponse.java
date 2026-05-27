package com.smartcloset.user.dto;

public record AccountDeletionResponse(boolean deleted) {

    public static AccountDeletionResponse success() {
        return new AccountDeletionResponse(true);
    }
}

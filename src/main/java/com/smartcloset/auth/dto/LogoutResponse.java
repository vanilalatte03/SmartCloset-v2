package com.smartcloset.auth.dto;

public record LogoutResponse(boolean loggedOut) {

    public static LogoutResponse success() {
        return new LogoutResponse(true);
    }
}

package com.smartcloset.auth.infrastructure;

public interface EmailSender {

    void sendEmailVerification(String email, String token);

    void sendPasswordReset(String email, String token);
}

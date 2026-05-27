package com.smartcloset.auth.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ConsoleEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailSender.class);

    @Override
    public void sendEmailVerification(String email, String token) {
        log.info("SmartCloset email verification token for {}: {}", email, token);
    }

    @Override
    public void sendPasswordReset(String email, String token) {
        log.info("SmartCloset password reset token for {}: {}", email, token);
    }
}

package com.smartcloset.auth.application;

import com.smartcloset.auth.infrastructure.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 계정 액션 메일 발송을 DB commit 이후로 지연해 token 저장 상태와 외부 side effect를 맞춘다.
 */
@Service
class AccountEmailSendScheduler {

    private static final Logger log = LoggerFactory.getLogger(AccountEmailSendScheduler.class);

    private final EmailSender emailSender;

    AccountEmailSendScheduler(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    void sendEmailVerificationAfterCommit(String email, String token) {
        scheduleAfterCommit("email verification", email, () -> emailSender.sendEmailVerification(email, token));
    }

    void sendPasswordResetAfterCommit(String email, String token) {
        scheduleAfterCommit("password reset", email, () -> emailSender.sendPasswordReset(email, token));
    }

    private void scheduleAfterCommit(String purpose, String email, Runnable senderCall) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            sendSafely(purpose, email, senderCall);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendSafely(purpose, email, senderCall);
            }
        });
    }

    private void sendSafely(String purpose, String email, Runnable senderCall) {
        try {
            senderCall.run();
        } catch (RuntimeException exception) {
            log.warn("Failed to send {} account email for {}", purpose, email, exception);
        }
    }
}

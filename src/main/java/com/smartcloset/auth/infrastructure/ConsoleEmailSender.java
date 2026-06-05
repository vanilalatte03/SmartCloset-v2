package com.smartcloset.auth.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Local/demo profile에서 이메일 인증과 비밀번호 재설정 token을 로그로 남기는 sender다.
 *
 * <p>운영 발송 adapter가 아니므로 실제 메일 전송, SES, SMTP 설정을 포함하지 않는다.</p>
 */
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

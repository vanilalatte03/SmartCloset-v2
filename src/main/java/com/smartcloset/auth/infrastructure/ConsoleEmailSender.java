package com.smartcloset.auth.infrastructure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Local/demo profile에서 이메일 인증과 비밀번호 재설정 token을 local outbox 파일에 남기는 sender다.
 *
 * <p>운영 발송 adapter가 아니므로 실제 메일 전송, SES, SMTP 설정을 포함하지 않는다.</p>
 */
@Component
public class ConsoleEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailSender.class);

    private final Path outboxPath;
    private final Clock clock;

    @Autowired
    public ConsoleEmailSender(@Value("${smartcloset.email.local-outbox-path}") String outboxPath) {
        this(Path.of(outboxPath), Clock.systemUTC());
    }

    ConsoleEmailSender(Path outboxPath, Clock clock) {
        this.outboxPath = outboxPath;
        this.clock = clock;
    }

    @Override
    public void sendEmailVerification(String email, String token) {
        writeOutbox("EMAIL_VERIFICATION", email, token);
    }

    @Override
    public void sendPasswordReset(String email, String token) {
        writeOutbox("PASSWORD_RESET", email, token);
    }

    private void writeOutbox(String purpose, String email, String token) {
        try {
            Path absolutePath = outboxPath.toAbsolutePath();
            Path parent = absolutePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(
                    absolutePath,
                    outboxLine(purpose, email, token),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
            log.atInfo()
                    .setMessage("account_email_written")
                    .addKeyValue("purpose", purpose)
                    .addKeyValue("email", email)
                    .addKeyValue("outbox_path", absolutePath)
                    .log();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write local account email outbox", exception);
        }
    }

    private String outboxLine(String purpose, String email, String token) {
        return "%s\t%s\t%s\t%s%n".formatted(Instant.now(clock), purpose, email, token);
    }
}

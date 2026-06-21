package com.smartcloset.auth.infrastructure;

/**
 * 계정 액션 메일 발송을 application service에서 분리하는 adapter boundary다.
 *
 * <p>현재 local 구현은 파일 outbox이며, 운영 메일 provider가 추가되어도 인증 흐름은 이 interface에만 의존한다.</p>
 */
public interface EmailSender {

    /**
     * 이메일 인증을 완료할 수 있는 single-use token을 사용자 이메일로 전달한다.
     */
    void sendEmailVerification(String email, String token);

    /**
     * 비밀번호 재설정을 완료할 수 있는 single-use token을 사용자 이메일로 전달한다.
     */
    void sendPasswordReset(String email, String token);
}

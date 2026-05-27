# 단계 2: email-verification-password-reset

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/API.md`
- `docs/ARCHITECTURE.md`
- `docs/ERD.md`
- `docs/SHARING_GUIDE.md`
- `docs/adr/013-mvp8-account-stability.md`
- `src/main/java/com/smartcloset/auth/**`
- `src/main/java/com/smartcloset/user/**`
- `src/test/java/com/smartcloset/auth/**`

## 작업

- `users`에 email verification과 password login 상태를 추가한다.
- 기존 local user는 `emailVerified=true`, `passwordLoginEnabled=true`로 취급되게 한다.
- 새 password signup은 `emailVerified=false`, `passwordLoginEnabled=true`로 시작한다.
- Signup 성공 응답은 이메일 인증 필요 상태를 반환하고 access token을 발급하지 않는다.
- 미인증 password 계정 login은 `EMAIL_VERIFICATION_REQUIRED`로 실패한다.
- `AccountActionToken` entity/repository/service를 추가한다.
- Account action token purpose는 `EMAIL_VERIFICATION`, `PASSWORD_RESET`이다.
- Token 원문은 DB에 저장하지 않고 hash만 저장한다.
- Token은 만료와 single-use를 적용한다.
- `EmailSender` interface와 `ConsoleEmailSender`를 추가한다.
- 이메일 인증 요청/확인 API를 추가한다.
- 비밀번호 재설정 요청/확인 API를 추가한다.
- 비밀번호 재설정 성공 시 password hash를 BCrypt로 갱신하고 해당 사용자 refresh sessions를 revoke한다.

## 인수 기준

```bash
./gradlew test --tests '*Auth*'
git diff --check
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. 아키텍처 체크리스트를 확인한다:
   - Signup 직후 access token이 발급되지 않는가?
   - 미인증 login이 `EMAIL_VERIFICATION_REQUIRED`로 실패하는가?
   - Token 원문이 DB에 저장되지 않는가?
   - Reset request가 계정 존재 여부를 노출하지 않는가?
   - Reset 성공 후 refresh sessions가 revoke되는가?
3. 결과에 따라 `phases/8-smartcloset-account-stability/index.json`의 해당 단계를 업데이트한다:
   - 성공 -> `"status": "completed"`, `"summary": "이메일 인증과 비밀번호 재설정, ConsoleEmailSender 기반 account action token을 추가했다."`
   - 수정 3회 시도 후에도 실패 -> `"status": "error"`, `"error_message": "구체적 에러 내용"`
   - 사용자 개입 필요 -> `"status": "blocked"`, `"blocked_reason": "구체적 사유"` 후 즉시 중단

검증 또는 리뷰가 통과하지 못하면 `issues/8-smartcloset-account-stability/issue-N.md`에 재현 명령, 핵심 에러, 수정 방향을 기록하고 fix step을 추가한다.

## 금지사항

- SES/SMTP 실제 발송 구현체를 추가하지 마라. 이유: MVP8 이메일 발송은 `ConsoleEmailSender` 기준이다.
- Google login을 구현하지 마라. 이유: Step 3에서 처리한다.
- Token 원문을 DB에 저장하지 마라. 이유: 계정 안정성 보안 계약이다.
- 기존 테스트를 깨뜨리지 마라.

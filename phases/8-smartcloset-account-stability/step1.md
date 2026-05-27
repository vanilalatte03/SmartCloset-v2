# 단계 1: refresh-token-session

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/API.md`
- `docs/ARCHITECTURE.md`
- `docs/ERD.md`
- `docs/FRONTEND.md`
- `docs/COMMANDS.md`
- `docs/adr/013-mvp8-account-stability.md`
- `src/main/java/com/smartcloset/auth/**`
- `src/main/java/com/smartcloset/security/**`
- `src/main/java/com/smartcloset/user/**`
- `src/test/java/com/smartcloset/auth/**`
- `src/test/java/com/smartcloset/security/**`

## 작업

- DB-backed refresh session entity/repository/service를 추가한다.
- Refresh token 원문은 DB에 저장하지 않고 hash만 저장한다.
- Refresh session은 user, tokenHash, issuedAt, expiresAt, revokedAt, replacedByTokenHash를 가진다.
- Login 성공 시 access token JSON 응답과 refresh cookie를 함께 발급한다.
- `POST /api/auth/refresh`를 추가하고 refresh cookie 검증, rotation, 새 access token 발급, 새 refresh cookie 설정을 처리한다.
- `POST /api/auth/logout`을 추가하고 refresh session revoke와 cookie 만료를 멱등 처리한다.
- Refresh cookie name, max age, Secure, SameSite, domain, path를 properties/env로 분리한다.
- CORS credentials 설정을 refresh cookie 요청에 맞게 조정한다.
- 기존 access token JWT subject/claims와 보호 API 인증 경계는 유지한다.

## 인수 기준

```bash
./gradlew test --tests '*Auth*' --tests '*Security*'
git diff --check
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. 아키텍처 체크리스트를 확인한다:
   - Refresh token 원문이 DB, log, JSON response에 남지 않는가?
   - Login/refresh 성공 시 `AuthResponse`에 refresh token string이 없는가?
   - Logout이 refresh cookie가 없어도 성공하는가?
   - CORS credentials 설정이 local frontend origin과 맞는가?
3. 결과에 따라 `phases/8-smartcloset-account-stability/index.json`의 해당 단계를 업데이트한다:
   - 성공 -> `"status": "completed"`, `"summary": "DB-backed refresh session, rotation, logout, cookie 설정을 추가했다."`
   - 수정 3회 시도 후에도 실패 -> `"status": "error"`, `"error_message": "구체적 에러 내용"`
   - 사용자 개입 필요 -> `"status": "blocked"`, `"blocked_reason": "구체적 사유"` 후 즉시 중단

검증 또는 리뷰가 통과하지 못하면 `issues/8-smartcloset-account-stability/issue-N.md`에 재현 명령, 핵심 에러, 수정 방향을 기록하고 fix step을 추가한다.

## 금지사항

- 이메일 인증, 비밀번호 재설정, Google login을 구현하지 마라. 이유: Step 1은 refresh session만 담당한다.
- Redis를 추가하지 마라. 이유: MVP8 refresh session은 DB-backed로 검증한다.
- refresh token 원문을 DB나 JSON response에 저장하지 마라. 이유: MVP8 보안 계약이다.
- access token 저장 위치를 프론트에서 아직 변경하지 마라. 이유: 프론트 UX는 Step 5에서 처리한다.
- 기존 테스트를 깨뜨리지 마라.

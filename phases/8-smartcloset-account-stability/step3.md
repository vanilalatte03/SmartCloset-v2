# 단계 3: google-oauth-login

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/API.md`
- `docs/ARCHITECTURE.md`
- `docs/ERD.md`
- `docs/SHARING_GUIDE.md`
- `docs/adr/013-mvp8-account-stability.md`
- `build.gradle`
- `src/main/java/com/smartcloset/auth/**`
- `src/main/java/com/smartcloset/security/**`
- `src/main/resources/application.yml`

## 작업

- Spring OAuth2 client dependency와 Google OAuth 설정을 추가한다.
- `social_accounts` entity/repository/service를 추가한다.
- `GET /api/auth/oauth2/providers`가 Google provider enabled/disabled 상태를 반환하게 한다.
- Google client id/secret/redirect 설정이 없으면 provider disabled로 응답한다.
- `GET /api/auth/oauth2/google` login start와 callback 흐름을 추가한다.
- Google profile의 verified email만 허용한다.
- 기존 같은 email user가 있으면 social account를 link한다.
- 새 Google user는 `emailVerified=true`, `passwordLoginEnabled=false`로 생성한다.
- OAuth 성공 시 refresh cookie를 발급하고 frontend callback URL로 redirect한다.
- OAuth redirect/base URL과 frontend callback URL은 properties/env로 분리한다.

## 인수 기준

```bash
./gradlew test --tests '*Auth*' --tests '*Security*'
git diff --check
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. 아키텍처 체크리스트를 확인한다:
   - Google 설정이 없을 때 provider disabled로 응답하는가?
   - Google verified email만 계정 생성/link에 사용되는가?
   - 새 Google user가 password login disabled 상태인가?
   - OAuth 성공 후 refresh cookie를 발급하고 access token 원문을 URL에 싣지 않는가?
3. 결과에 따라 `phases/8-smartcloset-account-stability/index.json`의 해당 단계를 업데이트한다:
   - 성공 -> `"status": "completed"`, `"summary": "Google OAuth provider status, login/callback, social account link 흐름을 추가했다."`
   - 수정 3회 시도 후에도 실패 -> `"status": "error"`, `"error_message": "구체적 에러 내용"`
   - 사용자 개입 필요 -> `"status": "blocked"`, `"blocked_reason": "구체적 사유"` 후 즉시 중단

검증 또는 리뷰가 통과하지 못하면 `issues/8-smartcloset-account-stability/issue-N.md`에 재현 명령, 핵심 에러, 수정 방향을 기록하고 fix step을 추가한다.

## 금지사항

- AWS Secrets Manager를 추가하지 마라. 이유: MVP8은 local/env 설정만 사용한다.
- Google access token을 DB에 저장하지 마라. 이유: MVP8은 provider identity link만 필요하다.
- Refresh token이나 access token을 redirect URL query에 싣지 마라. 이유: token 노출 위험이 있다.
- 기존 테스트를 깨뜨리지 마라.

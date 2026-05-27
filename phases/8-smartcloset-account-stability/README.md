# Phase: SmartCloset 8차 Account Stability MVP

## 목표

MVP7 위치/날씨 신뢰도 완료 baseline 위에 refresh token, 이메일 인증, 비밀번호 재설정, Google login, 세션 만료 UX, 계정 삭제/데이터 삭제를 추가한다.

AWS 배포는 구현하지 않는다. MVP9에서 AWS 배포를 진행할 때 S3/SES/RDS/도메인 설정을 adapter와 profile 중심으로 추가할 수 있도록 경계만 준비한다.

## 작업 범위

- Must-have / MVP8 P0: MVP7 archive, MVP8 docs/ADR/agent 전환, DB-backed refresh session, HttpOnly refresh cookie, refresh/logout API, 이메일 인증, 비밀번호 재설정, `EmailSender`/`ConsoleEmailSender`, Google OAuth provider status/login/callback, 세션 만료 UX, account settings, 계정 hard delete, AWS-ready adapter boundary, Docker Compose 공유 검증
- Should-have / MVP8 P1: 계정 상태 표시 문구 polish, auth form error copy polish, provider disabled 안내 polish
- MVP8 제외: AWS 배포 구현, S3 구현체, SES/SMTP 실제 발송 구현체, Secrets Manager, CD 자동화, Redis, admin 기능, soft delete/복구 정책, production DB migration 도구 전환, 추천 규칙 변경

## Steps

| Step | Name | Range |
| ---: | --- | --- |
| 0 | mvp8-scope-docs-archive | Must-have / MVP8 P0 |
| 1 | refresh-token-session | Must-have / MVP8 P0 |
| 2 | email-verification-password-reset | Must-have / MVP8 P0 |
| 3 | google-oauth-login | Must-have / MVP8 P0 |
| 4 | account-hard-delete | Must-have / MVP8 P0 |
| 5 | frontend-account-stability-ux | Must-have / MVP8 P0 |
| 6 | aws-ready-local-profile-boundaries | Must-have / MVP8 P0 |
| 7 | compose-docs-qa | Must-have / MVP8 P0 |

## 단계 진행 원칙

- Step 0은 문서 전환, archive, ADR, phase 정의만 다룬다.
- Step 1은 refresh session, cookie, refresh/logout API만 다룬다. 이메일 인증과 OAuth는 추가하지 않는다.
- Step 2는 이메일 인증과 비밀번호 재설정만 다룬다. 실제 SMTP/SES 구현은 추가하지 않는다.
- Step 3은 Google OAuth provider 상태와 login/callback만 다룬다.
- Step 4는 계정 hard delete와 소유 데이터/이미지 cleanup만 다룬다.
- Step 5는 frontend account stability UX만 다룬다.
- Step 6은 local/prod profile 경계, properties/env, AWS-ready adapter boundary 문서/설정 정리만 다룬다. AWS 구현은 하지 않는다.
- Step 7은 문서 동기화, Docker Compose, QA 기록, 최종 검증을 수행한다.

## 완료 기준

- Password signup은 access token 없이 이메일 인증 필요 상태를 반환한다.
- 미인증 password 계정 login은 `EMAIL_VERIFICATION_REQUIRED`로 실패한다.
- 이메일 인증 완료 후 login은 access token과 refresh cookie를 발급한다.
- `POST /api/auth/refresh`가 refresh cookie를 검증하고 rotation한다.
- `POST /api/auth/logout`이 refresh session을 revoke하고 cookie를 만료한다.
- 비밀번호 reset 성공 후 이전 refresh session은 사용할 수 없다.
- Google provider disabled/enabled 상태를 확인할 수 있다.
- 프론트가 앱 시작 또는 새로고침 시 refresh cookie로 세션을 복구한다.
- 보호 API 401 발생 시 refresh 후 원 요청을 한 번만 재시도한다.
- refresh 실패 시 세션 만료 안내가 표시된다.
- `DELETE /api/users/me`가 현재 사용자 데이터와 이미지 파일을 삭제한다.
- MVP5 이미지, MVP6 피드백/개인화, MVP7 위치/날씨 source snapshot 기능이 유지된다.
- AWS 구현은 포함하지 않지만 Email/Image/Cookie/CORS/OAuth URL adapter 경계가 유지된다.
- 공개 `userId` query parameter와 today 추천 GET endpoint가 추가되지 않는다.

## 검증 명령

```bash
git diff --check
./gradlew test
./gradlew build
(cd frontend && npm run build)
docker compose config --quiet
```

최종 step에서는 아래를 추가로 실행한다.

```bash
python3 scripts/checks.py --docs-check-config phases/8-smartcloset-account-stability/docs-checks.json --docs-check
docker compose down -v
test -f .env || cp .env.example .env
docker compose up --build -d
curl -fsS http://localhost:8080/v3/api-docs >/dev/null
curl -fsS http://localhost:5173 >/dev/null
docker compose down
```

## 실행 예시

```bash
python3 scripts/execute.py 8-smartcloset-account-stability --next-step-only
python3 scripts/execute.py 8-smartcloset-account-stability
python3 scripts/autopilot.py 8-smartcloset-account-stability --base main --max-review-fixes 2 --unsafe
```

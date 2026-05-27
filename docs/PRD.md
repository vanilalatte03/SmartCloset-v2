# PRD: SmartCloset MVP8 계정 안정성

## 문서 목적

이 문서는 SmartCloset MVP8의 확정 범위를 정의한다. MVP8은 MVP7 위치/날씨 신뢰도 완료 baseline 위에 refresh token, 이메일 인증, 비밀번호 재설정, Google 로그인, 세션 만료 UX, 계정 삭제/데이터 삭제를 추가한다.

현재 코드 출발점은 MVP7 구현 완료 상태다. MVP8 구현 기준은 이 문서와 `docs/` 아래 현재 문서, ADR-013이다.

## 문서 책임

| 계약 영역 | Source of truth |
| --- | --- |
| HTTP endpoint, request/response DTO, 인증/에러 계약 | `docs/API.md` |
| 추천 후보, 점수, 추천 이유, 예보 시간대 입력 | `docs/RECOMMENDATION_RULES.md` |
| 백엔드 구조, transaction, account/auth adapter 정책 | `docs/ARCHITECTURE.md` |
| DB schema와 JPA/entity 기준 | `docs/ERD.md` |
| 프론트 API client, 타입, UX, 반응형 기준 | `docs/FRONTEND.md` |
| 데모/공유 검증 | `docs/DEMO_SCENARIO.md`, `docs/SHARING_GUIDE.md` |
| 결정 배경 | `docs/ADR.md`, `docs/adr/013-mvp8-account-stability.md` |

## MVP8 한 줄 정의

SmartCloset 사용자가 세션을 안정적으로 유지하고, 이메일/비밀번호/Google 계정을 복구하거나 연결하며, 본인 계정과 데이터를 삭제할 수 있게 한다.

## 목표

- Access token 만료와 새로고침 상황에서도 refresh cookie로 세션을 복구한다.
- 이메일 소유 확인 전 password 계정 로그인을 차단한다.
- 비밀번호 재설정 flow를 제공한다.
- Google social login을 제공한다.
- 세션 만료 시 사용자가 무엇을 해야 하는지 명확히 안내한다.
- 로그인 화면에서 이메일 저장 체크박스로 반복 입력 부담을 줄인다.
- 계정 삭제 시 사용자 소유 데이터와 이미지 파일을 삭제한다.
- MVP9 AWS 배포를 위해 email/image/cookie/CORS/OAuth URL 경계를 adapter와 env 중심으로 설계한다.

## 현재 Baseline

- Spring Security + JWT Bearer access token 인증을 사용한다.
- 기존 access token은 `HS256`과 `JWT_SECRET`으로 서명한다.
- 공개 HTTP API는 `userId` query parameter를 받지 않는다.
- 현재 사용자 전용 응답 DTO는 `userId`를 노출하지 않는다.
- 사용자 소유 옷장, 위치, 선호도, 추천 이력, 착용 이력, 추천 피드백은 인증 사용자별로 분리한다.
- 추천 생성 API는 `POST /api/recommendations`다.
- 추천 이력 조회 API는 `GET /api/recommendations?limit={limit}`이며 기본 20, 최소 1, 최대 50, 최신순이다.
- 현재 날씨 요약 API는 `GET /api/weather/current`이며 보호 API다.
- MVP5의 이미지 API, MVP6의 피드백/개인화, MVP7의 위치/날씨 source snapshot은 유지한다.
- Docker Compose local 공유 흐름을 유지한다.

## 해결하려는 문제

- Access token 만료 후 사용자가 갑자기 로그인 화면으로 떨어질 수 있다.
- 새로고침과 브라우저 재방문 시 세션 복구 경험이 불안정하다.
- 이메일 소유 확인 없이 password 계정이 바로 활성화된다.
- 사용자가 비밀번호를 잊었을 때 계정을 복구할 수 없다.
- Google 계정을 통한 간편 로그인이 없다.
- 계정 삭제와 데이터 삭제 경로가 없다.
- MVP9 AWS 배포에서 S3/SES/도메인 설정을 추가할 때 코드 전반을 다시 건드릴 위험이 있다.

## 핵심 사용자 시나리오

1. 사용자가 이메일/password로 가입한다.
2. 앱은 이메일 인증이 필요하다고 안내하고, 개발 환경에서는 console/log로 인증 링크 또는 token을 출력한다.
3. 사용자가 이메일 인증을 완료한 뒤 로그인한다.
4. 사용자가 이메일 저장 체크박스를 선택하면 앱은 이메일 주소만 저장해 다음 로그인 화면에 복원한다.
5. 앱은 access token을 memory state에 저장하고 refresh token은 HttpOnly cookie로 유지한다.
6. 사용자가 새로고침하면 앱은 refresh API로 세션을 복구한다.
7. 보호 API가 access token 만료로 401을 반환하면 앱은 refresh 후 원 요청을 한 번 재시도한다.
8. refresh도 실패하면 앱은 세션 만료 안내와 로그인 화면을 보여준다.
9. 사용자가 비밀번호를 잊으면 reset 요청 후 console/log token으로 새 비밀번호를 설정한다.
10. 사용자는 Google login으로 가입 또는 로그인할 수 있다.
11. 사용자는 계정 설정에서 계정 삭제를 확인하고 본인 데이터 삭제를 수행한다.

## MVP8 우선순위

### P0: Refresh token session

- Refresh token은 DB-backed session으로 저장한다.
- Raw refresh token은 DB에 저장하지 않고 hash만 저장한다.
- Refresh token은 HttpOnly cookie로만 전달한다.
- Refresh API는 rotation을 수행한다.
- Logout은 session revoke와 cookie 만료를 수행한다.
- Access token은 기존 bearer JSON 응답 형태를 유지한다.

### P0: 이메일 인증

- Password signup은 이메일 인증 필요 상태를 반환하고 access token을 발급하지 않는다.
- 미인증 password 계정은 login을 차단한다.
- 인증 token은 hash로 저장하고 만료와 single-use를 적용한다.
- MVP8 이메일 발송은 `EmailSender` 인터페이스와 `ConsoleEmailSender` 기준이다.

### P0: 비밀번호 재설정

- Reset 요청은 계정 존재 여부를 노출하지 않는다.
- Reset token은 hash로 저장하고 만료와 single-use를 적용한다.
- Reset 확인 성공 시 BCrypt password hash를 갱신한다.
- Reset 성공 시 기존 refresh sessions를 revoke한다.

### P0: Google login

- Google OAuth2 provider status API를 제공한다.
- Google 설정이 없으면 provider disabled 상태를 반환한다.
- Google verified email은 이메일 인증 완료로 취급한다.
- 기존 이메일 계정이 있으면 social account link 정책으로 연결한다.

### P0: 세션 만료 UX

- 프론트는 access token을 memory state에 저장한다.
- 앱 시작 또는 새로고침 시 refresh API로 세션을 복구한다.
- 보호 API 401은 refresh 후 원 요청 retry-once로 처리한다.
- refresh 실패 시 명확한 세션 만료 안내를 보여준다.
- 로그인 이메일 저장 체크박스는 이메일 주소만 저장하고 비밀번호나 token은 저장하지 않는다.

### P0: 계정 삭제/데이터 삭제

- `DELETE /api/users/me`를 보호 API로 제공한다.
- Password login enabled 계정은 현재 비밀번호 확인을 요구한다.
- Google-only 계정은 confirmation string을 요구한다.
- 사용자 row와 소유 데이터, token/session/social account/action token, 이미지 파일을 즉시 삭제한다.
- 다른 사용자 데이터는 삭제하지 않는다.

### P0: AWS-ready adapter boundary

- `EmailSender`는 MVP9에서 SES/SMTP 구현체를 추가할 수 있게 둔다.
- `ClothingImageStorage`는 MVP9에서 S3 구현체를 추가할 수 있게 유지한다.
- cookie/CORS/OAuth redirect/base URL은 properties/env로 분리한다.
- AWS 구현 자체는 MVP8에서 제외한다.

## 포함 범위

- `refresh_sessions` table
- `account_action_tokens` table
- `social_accounts` table
- `users.email_verified`
- `users.password_login_enabled`
- refresh/logout API
- email verification request/confirm API
- password reset request/confirm API
- Google OAuth provider status/login/callback API
- `CurrentUserResponse` account 상태 필드
- account deletion API
- frontend auth/session/account settings UX와 로그인 이메일 저장 체크박스
- AWS-ready adapter boundary 문서화
- MVP8 phase 문서와 docs-check 규칙

## 제외 범위

- AWS 배포 구현
- S3 storage 구현체
- SES/SMTP 실제 발송 구현체
- Secrets Manager
- Redis
- admin 계정 관리
- soft delete/복구 정책
- production DB migration 도구 전환
- native mobile app 또는 PWA 배포
- 추천 규칙 변경
- AI/GPT 추천
- AI 자동 태깅
- 다중 이미지 업로드

## 완료 기준

- Password signup은 access token 없이 이메일 인증 필요 상태를 반환한다.
- 미인증 password 계정 login은 `EMAIL_VERIFICATION_REQUIRED`로 실패한다.
- 이메일 인증 완료 후 login은 access token과 refresh cookie를 발급한다.
- Refresh API는 token을 회전하고 새 access token을 발급한다.
- Logout은 refresh session을 revoke하고 cookie를 만료한다.
- 비밀번호 reset 성공 후 이전 refresh session은 사용할 수 없다.
- Google provider disabled 상태와 enabled login flow를 확인할 수 있다.
- 프론트는 새로고침 후 refresh cookie로 세션을 복구한다.
- 401 발생 시 refresh 후 원 요청을 한 번만 재시도한다.
- refresh 실패 시 세션 만료 안내가 표시된다.
- 이메일 저장 체크박스는 이메일 주소만 저장/삭제하며 비밀번호, access token, refresh token은 저장하지 않는다.
- 계정 삭제 후 해당 사용자 데이터와 이미지 파일이 삭제된다.
- MVP5/MVP6/MVP7 핵심 흐름이 유지된다.
- AWS 구현은 포함하지 않지만 adapter 경계가 문서와 코드 구조에 반영된다.

## 테스트/검증 기준

문서 전환 검증:

- `git diff --check`
- `python3 scripts/checks.py --docs-check-config phases/8-smartcloset-account-stability/docs-checks.json --docs-check`

MVP8 구현 phase 최종 검증:

- `git diff --check`
- `./gradlew test`
- `./gradlew build`
- `cd frontend && npm run build`
- `docker compose config --quiet`
- `python3 scripts/checks.py --docs-check-config phases/8-smartcloset-account-stability/docs-checks.json --docs-check`

## 결정 완료 사항

- 이메일 발송: MVP8은 `ConsoleEmailSender`, 실제 SMTP/SES는 MVP9
- 이메일 인증 gate: 미인증 password 계정 login 차단
- 계정 삭제: 즉시 하드 삭제
- Access token 저장: 프론트 memory state
- Refresh token 전달: HttpOnly cookie
- 이메일 저장: 프론트에서 이메일 주소 문자열만 저장
- AWS: MVP8 구현 제외, MVP9 adapter 교체 가능성만 준비

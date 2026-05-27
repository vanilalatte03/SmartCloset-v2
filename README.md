# SmartCloset

현재 문서 기준은 **MVP8: 계정 안정성 MVP**입니다. MVP8은 MVP7 위치/날씨 신뢰도 완료 상태 위에 refresh token, 이메일 인증, 비밀번호 재설정, Google 로그인, 세션 만료 UX, 계정 삭제/데이터 삭제를 추가하는 단계입니다.

현재 코드 출발점은 MVP7 위치/날씨 신뢰도 구현 완료 상태입니다. MVP8 구현 source of truth는 루트 `README.md`와 `docs/` 아래 문서, 그리고 ADR-013입니다.

## 현재 Baseline

- Spring Boot 4.0.6, Java 21, MySQL, React+Vite+TypeScript SPA를 사용한다.
- Spring Security + JWT Bearer access token 인증을 유지한다.
- MVP8은 DB-backed refresh session과 HttpOnly refresh cookie를 추가한다.
- Access token은 JSON 응답의 bearer token으로 유지하되, 프론트는 memory state에 보관하고 refresh cookie로 세션을 복구한다.
- 공개 HTTP API는 `userId` query parameter를 받지 않는다.
- 현재 사용자 전용 response DTO는 `userId`를 노출하지 않는다.
- 사용자별 옷장, 위치, 선호도, 추천 이력, 착용 이력, 추천 피드백을 분리한다.
- 추천 생성은 `POST /api/recommendations`만 사용한다.
- 추천 이력은 `GET /api/recommendations?limit={limit}`이며 기본 20, 최소 1, 최대 50, 최신순이다.
- 현재 날씨 요약은 `GET /api/weather/current` 보호 API로 조회한다.
- MVP5 이미지 업로드/교체/조회/삭제, MVP6 피드백/개인화, MVP7 위치/날씨 source snapshot 계약을 유지한다.
- Docker Compose local 공유 방식을 유지한다.

## MVP8 목표

사용자가 로그인 세션을 안정적으로 유지하고, 계정을 회복하고, 본인 데이터를 삭제할 수 있게 만든다.

### 포함 범위

- DB-backed refresh token session
- Refresh token hash 저장과 rotation/revoke
- Refresh token HttpOnly cookie
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- 이메일 인증 요청/확인
- 비밀번호 재설정 요청/확인
- 개발용 `EmailSender` 인터페이스와 `ConsoleEmailSender`
- Google social login
- OAuth provider status API
- 세션 만료 UX 개선: 앱 시작 시 refresh, 401 retry-once, 최종 만료 안내
- 로그인 화면 이메일 저장 체크박스
- 계정 삭제와 사용자 데이터 즉시 하드 삭제
- MVP9 AWS 배포에서 교체할 Email/Image/Cookie/CORS/OAuth URL 어댑터 경계
- MVP8 phase 문서와 docs-check 규칙 작성

### 제외 범위

- AWS 배포 구현
- S3 구현체
- SES/SMTP 실제 발송 구현체
- Secrets Manager
- CD 자동화
- Redis
- admin 계정 관리
- soft delete/복구 정책
- production DB migration 도구 전환
- native mobile app 또는 PWA 배포
- 추천 점수/규칙 변경
- AI/GPT 추천
- AI 자동 태깅

## API 요약

공개 API:

- `POST /api/auth/signup`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `POST /api/auth/email-verification/request`
- `POST /api/auth/email-verification/confirm`
- `POST /api/auth/password-reset/request`
- `POST /api/auth/password-reset/confirm`
- `GET /api/auth/oauth2/providers`
- `GET /api/auth/oauth2/google`
- `GET /api/auth/oauth2/callback/google`

보호 API:

- `GET /api/users/me`
- `DELETE /api/users/me`
- `GET /api/locations?keyword={keyword}`
- `POST /api/locations/resolve`
- `GET /api/users/me/location`
- `PUT /api/users/me/location`
- `GET /api/users/me/preferences`
- `PUT /api/users/me/preferences`
- `GET /api/weather/current`
- `POST /api/clothes`
- `GET /api/clothes`
- `GET /api/clothes/{clothingId}`
- `PUT /api/clothes/{clothingId}`
- `PATCH /api/clothes/{clothingId}/archive`
- `PUT /api/clothes/{clothingId}/image`
- `GET /api/clothes/{clothingId}/image`
- `DELETE /api/clothes/{clothingId}/image`
- `POST /api/recommendations`
- `GET /api/recommendations?limit={limit}`
- `PATCH /api/recommendations/{recommendationId}/worn`
- `PUT /api/recommendations/{recommendationId}/feedback`

MVP8 API 변경:

- `POST /api/auth/signup`는 password 계정을 만들고 이메일 인증 필요 상태를 반환한다. 가입 직후 access token은 발급하지 않는다.
- `POST /api/auth/login`은 이메일 인증된 password 계정만 access token과 refresh cookie를 발급한다.
- `POST /api/auth/refresh`는 refresh cookie를 검증하고 rotation 후 새 access token과 새 refresh cookie를 발급한다.
- `POST /api/auth/logout`은 현재 refresh session을 revoke하고 refresh cookie를 만료한다. 이미 로그아웃된 상태도 성공으로 처리한다.
- 이메일 인증과 비밀번호 재설정 token 원문은 DB에 저장하지 않고 hash만 저장한다.
- `CurrentUserResponse`는 `emailVerified`, `passwordLoginEnabled`, `authProviders`를 포함한다.
- `AuthResponse`는 refresh token 원문을 응답 JSON에 포함하지 않는다.
- `DELETE /api/users/me`는 본인 계정과 소유 데이터를 즉시 삭제한다.

## 계정 안정성 정책

- Refresh token은 서버가 생성한 충분히 긴 random token을 사용한다.
- DB에는 refresh token hash, user, issuedAt, expiresAt, revokedAt, replacedBy hash metadata만 저장한다.
- Refresh token cookie는 HttpOnly이고, cookie name/max age/Secure/SameSite/domain/path는 properties/env로 설정한다.
- Access token은 bearer JSON 응답으로 유지하되 프론트 저장 위치는 memory state다.
- 새로고침 또는 앱 시작 시 `POST /api/auth/refresh`로 세션을 복구한다.
- 보호 API가 401을 반환하면 프론트는 refresh를 한 번 시도하고 원 요청을 한 번만 재시도한다.
- refresh까지 실패하면 access token을 제거하고 로그인 화면에 세션 만료 안내를 표시한다.
- 이메일 저장 체크박스는 이메일 주소 문자열만 저장하며 비밀번호와 token 저장 용도로 사용하지 않는다.
- 미인증 password 계정은 로그인할 수 없다.
- Google 계정은 Google이 verified email을 반환한 경우 이메일 인증 완료로 취급한다.
- 비밀번호 재설정 요청은 계정 존재 여부를 노출하지 않는다.
- 계정 삭제는 soft delete가 아니라 즉시 하드 삭제다.

## AWS-Ready 경계

MVP8은 AWS를 구현하지 않는다. 대신 MVP9 AWS 배포에서 코드를 크게 다시 쓰지 않도록 아래 경계를 문서화하고 구현 단계에서 지킨다.

- 이메일 발송은 `EmailSender` 인터페이스 뒤에 둔다. MVP8 구현체는 `ConsoleEmailSender`이며 MVP9에서 SES/SMTP 구현체를 추가할 수 있어야 한다.
- 이미지 파일 삭제는 `ClothingImageStorage` 인터페이스만 통해 수행한다. MVP9에서 S3 storage 구현체를 추가해도 계정 삭제 로직은 바꾸지 않는다.
- refresh cookie, CORS allowed origins, CORS credentials, OAuth redirect/base URL은 properties/env로 분리한다.
- local profile과 Docker Compose 실행 흐름은 MVP8 이후에도 유지한다.
- AWS, S3, SES, Secrets Manager, RDS 운영 migration, CD automation은 MVP9 범위로 남긴다.

## 추천 규칙

추천은 계속 AI/GPT가 아닌 설명 가능하고 테스트 가능한 규칙 기반 추천이다.

- 총점은 100점이며 기존 score field를 유지한다.
- `weatherScore=35`, `colorScore=25`, `wearHistoryScore=20`, `recommendationHistoryScore=10`, `preferenceScore=10`이다.
- MVP6의 상황, styleTags, 최근 피드백 기반 `preferenceScore` 계약은 유지한다.
- MVP7의 위치/source snapshot과 `forecastPeriod` 계약은 유지한다.
- MVP8 계정 기능은 추천 점수, 후보 필터링, 추천 이유를 변경하지 않는다.
- 이미지 존재 여부는 계속 추천 점수, 후보 필터링, 추천 이유에 영향을 주지 않는다.

상세 기준은 `docs/RECOMMENDATION_RULES.md`를 따른다.

## 실행

로컬 백엔드:

```bash
./gradlew bootRun
```

로컬 프론트엔드:

```bash
cd frontend
npm run dev
```

Docker Compose:

```bash
test -f .env || cp .env.example .env
docker compose down -v
docker compose up --build
```

## 검증 명령

문서 전환 검증:

```bash
git diff --check
python3 scripts/checks.py --docs-check-config phases/8-smartcloset-account-stability/docs-checks.json --docs-check
```

MVP8 구현 phase 최종 검증:

```bash
git diff --check
./gradlew test
./gradlew build
(cd frontend && npm run build)
docker compose config --quiet
python3 scripts/checks.py --docs-check-config phases/8-smartcloset-account-stability/docs-checks.json --docs-check
```

Docker Compose smoke:

```bash
docker compose down -v
test -f .env || cp .env.example .env
docker compose up --build -d
curl -fsS http://localhost:8080/v3/api-docs >/dev/null
curl -fsS http://localhost:5173 >/dev/null
docker compose down
```

## 문서 기준

| 영역 | 문서 |
| --- | --- |
| 제품 목표와 MVP 범위 | `docs/PRD.md` |
| HTTP API와 DTO | `docs/API.md` |
| 추천 규칙과 점수 | `docs/RECOMMENDATION_RULES.md` |
| 백엔드 구조와 트랜잭션 | `docs/ARCHITECTURE.md` |
| DB schema | `docs/ERD.md` |
| 프론트 타입과 UX | `docs/FRONTEND.md` |
| 데모 시나리오 | `docs/DEMO_SCENARIO.md` |
| Docker Compose 공유 | `docs/SHARING_GUIDE.md` |
| 명령 모음 | `docs/COMMANDS.md` |
| MVP 변경 체크리스트 | `docs/MVP_CHANGE_CHECKLIST.md` |
| 결정 기록 | `docs/ADR.md`, `docs/adr/` |

## Archive

완료된 MVP 문맥은 `archive/` 아래의 최소 요약으로만 유지합니다. 구현 기준은 현재 `README.md`와 `docs/` 아래 문서입니다.

- MVP6 archive: `archive/mvp-6/README.md`
- MVP6 phase 기록: `phases/6-smartcloset-feedback-personalization/README.md`
- MVP7 archive: `archive/mvp-7/README.md`
- MVP7 phase 기록: `phases/7-smartcloset-location-weather-trust/README.md`

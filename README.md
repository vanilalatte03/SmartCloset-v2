# SmartCloset

현재 문서 기준은 **MVP9: 프론트 UI/UX 리디자인 MVP**입니다. MVP9는 MVP8 계정 안정성 완료 상태 위에서 `tmp/design-preview`와 `docs/design/mvp9/`의 화면 시안을 강하게 참고해 Auth, 추천, 옷장, 내 취향, 위치, 기록, 계정 설정 화면의 완성도를 높이는 단계입니다.

원래 MVP9 후보였던 AWS 배포는 후속 MVP로 연기합니다. 현재 구현 source of truth는 루트 `README.md`와 `docs/` 아래 문서, 그리고 ADR-014입니다.

## 현재 Baseline

- Spring Boot 4.0.6, Java 21, MySQL, React+Vite+TypeScript SPA를 사용한다.
- Spring Security + JWT Bearer access token 인증을 유지한다.
- MVP8 계정 안정성 기능은 완료 baseline으로 유지한다.
- Access token은 JSON 응답의 bearer token으로 유지하되, 프론트는 memory state에 보관하고 refresh cookie로 세션을 복구한다.
- Refresh token은 HttpOnly cookie로만 전달하고 JSON 응답에는 포함하지 않는다.
- Password signup은 이메일 인증 필요 상태를 반환하고 access token을 발급하지 않는다.
- 미인증 password 계정은 login할 수 없다.
- Google provider 상태, Google login, 비밀번호 재설정, 계정 삭제 UX를 유지한다.
- 공개 HTTP API는 `userId` query parameter를 받지 않는다.
- 현재 사용자 전용 response DTO는 `userId`를 노출하지 않는다.
- 사용자별 옷장, 위치, 선호도, 추천 이력, 착용 이력, 추천 피드백을 분리한다.
- 추천 생성은 `POST /api/recommendations`만 사용한다.
- 추천 이력은 `GET /api/recommendations?limit={limit}`이며 기본 20, 최소 1, 최대 50, 최신순이다.
- 현재 날씨 요약은 `GET /api/weather/current` 보호 API로 조회한다.
- MVP5 이미지 업로드/교체/조회/삭제, MVP6 피드백/개인화, MVP7 위치/날씨 source snapshot 계약을 유지한다.
- 백엔드 HTTP API, DTO, DB schema, 추천 점수/필터/tie-break는 MVP9에서 변경하지 않는다.
- Docker Compose local 공유 방식을 유지한다.

## MVP9 목표

사용자가 기능 완성도뿐 아니라 화면 완성도에서도 SmartCloset을 실제 서비스처럼 느낄 수 있게 만든다.

### 포함 범위

- MVP8 계정 안정성 archive 작성
- MVP9 docs/ADR/agent baseline 전환
- `tmp/design-preview` 기반 디자인 reference를 `docs/design/mvp9/`에 보관
- Auth 화면 리디자인
- 추천 dashboard 리디자인
- 옷장 목록과 옷 등록/수정 UX 리디자인
- 내 취향 화면 swatch/chip/toggle 중심 리디자인
- 위치 검색과 현재 위치 후보 찾기 UX 리디자인
- 기록 calendar/timeline UX 리디자인
- Profile pill/menu 기반 계정 설정 진입
- 계정 설정 화면 리디자인
- 데스크톱 1440px, 모바일 390px 기준 반응형 QA
- `frontend` build와 phase docs-check 검증

### 제외 범위

- AWS 배포 구현
- S3 구현체
- SES/SMTP 실제 발송 구현체
- Secrets Manager
- CD 자동화
- Redis
- 백엔드 API/DTO 변경
- DB schema 변경
- 추천 점수/필터/tie-break 변경
- AI/GPT 추천
- AI 자동 태깅
- native mobile app 또는 PWA 배포

## 프론트 UX 기준

- 로그인 후 기본 view는 `추천`이다.
- 데스크톱 primary navigation은 상단 탭이다.
- 모바일 primary navigation은 하단 탭이다.
- primary nav는 `추천`, `옷장`, `내 취향`, `위치`, `기록`으로 고정한다.
- `계정 설정`은 주 navigation tab이 아니라 우측 상단 profile pill/menu에서 진입한다.
- 추천 화면은 날씨, 위치, 상황, 예보 시간대, 옷장 준비 상태, 최근 이력을 한 화면에서 스캔할 수 있어야 한다.
- 추천 결과는 점수표보다 옷 조합과 "오늘 입기 좋은 이유"를 먼저 보여준다.
- 옷 이미지가 있으면 추천, 옷장, 기록에서 우선 표시하고, 없으면 기존 fallback visual을 사용한다.
- 색상은 swatch, 소재와 style tag는 chip/toggle 중심 control로 표시한다.
- 위치 화면은 외부 지도 없이 내부 KMA catalog 검색과 브라우저 좌표 resolve 후보 선택만 제공한다.
- 카드 radius는 8px 이하로 유지하고, 카드 안에 카드가 중첩되는 느낌을 피한다.
- 390px 모바일 폭에서 버튼, 카드, 입력 텍스트가 겹치거나 잘리지 않아야 한다.

상세 기준은 `docs/FRONTEND.md`와 `docs/design/mvp9/README.md`를 따른다.

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
- `PATCH /api/users/me`
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

MVP9는 API surface를 변경하지 않는다. 세부 계약은 `docs/API.md`를 따른다.

## 세션과 계정 정책

- Refresh token은 서버가 생성한 random token이며 DB에는 hash만 저장한다.
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

## 추천 규칙

추천은 계속 AI/GPT가 아닌 설명 가능하고 테스트 가능한 규칙 기반 추천이다.

- 총점은 100점이며 기존 score field를 유지한다.
- `weatherScore=35`, `colorScore=25`, `wearHistoryScore=20`, `recommendationHistoryScore=10`, `preferenceScore=10`이다.
- MVP6의 상황, styleTags, 최근 피드백 기반 `preferenceScore` 계약은 유지한다.
- MVP7의 위치/source snapshot과 `forecastPeriod` 계약은 유지한다.
- MVP8 계정 기능과 MVP9 UI/UX 변경은 추천 점수, 후보 필터링, 추천 이유를 변경하지 않는다.
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
python3 scripts/checks.py --docs-check-config phases/9-smartcloset-ui-ux-redesign/docs-checks.json --docs-check
```

MVP9 구현 phase 검증:

```bash
git diff --check
(cd frontend && npm run build)
python3 scripts/checks.py --docs-check-config phases/9-smartcloset-ui-ux-redesign/docs-checks.json --docs-check
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
| 디자인 reference | `docs/design/mvp9/README.md` |
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
- MVP8 archive: `archive/mvp-8/README.md`
- MVP8 phase 기록: `phases/8-smartcloset-account-stability/README.md`

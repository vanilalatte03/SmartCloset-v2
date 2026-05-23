---
name: smartcloset-backend
description: SmartCloset Spring Boot 4.0.6 백엔드 구현 또는 리뷰 시 사용한다. 인증 사용자 API, 규칙 기반 옷차림 추천, KMA 날씨 provider/fallback weather, 사용자 위치 API, 선호도, 테스트, Docker Compose 공유, 문서 동기화를 포함한다.
---

# SmartCloset Backend Skill

## Purpose
SmartCloset 현재 baseline 백엔드/프론트엔드 연동 작업을 구현하거나 리뷰하기 전에 반드시 따르는 실행 규칙으로 이 스킬을 사용한다.

이 스킬은 기획 기록이 아니다. Codex가 따라야 하는 현재 구현 기준을 정의한다. 이 스킬이 과거 MVP 메모, archive 문서, 오래된 ADR 표현, seed/test-user 흐름과 충돌하면 `Current Execution Baseline`과 현재 루트 `README.md` 및 `docs/`를 우선한다.

## Reading Policy
이 스킬은 모든 작업에서 전체를 동일한 깊이로 읽는 문서가 아니다. 기존 규칙은 그대로 유지하되, 작업 성격에 따라 어떤 섹션을 깊게 확인할지 라우팅하기 위해 이 정책을 사용한다.

모든 작업에서는 먼저 다음 섹션을 확인한다.

- Purpose
- Reading Policy
- Current Execution Baseline
- Harness Step PR Rules
- Strict Out of Scope
- Implementation Attitude

작업 성격에 따라 필요한 섹션만 추가로 확인한다.

- API 변경: API Rules
- 인증/보안 변경: Auth Rules, API Rules, Test Rules
- 추천 변경: Recommendation Rules, Weather Rules, Preference Rules, Test Rules
- 위치 변경: Location Rules, Weather Rules, API Rules
- 프론트 변경: Frontend Rules, API Rules
- DB/entity 변경: Entity and JPA Rules, 관련 API/Domain Rules
- 테스트 변경: Test Rules와 변경 대상 도메인 섹션
- 문서 변경: Documentation Sync Rules와 변경 대상 문서 관련 섹션

Historical Context는 현재 기준이 헷갈릴 때만 참고한다. Historical Context는 어떤 경우에도 Current Execution Baseline을 override하지 않는다.

`archive/`, 완료된 phase, 현재 step과 무관한 docs를 기본 컨텍스트로 끌어오지 않는다.

phase/step 작업에서는 현재 phase README와 현재 step 문서가 step-local 판단 기준이다.

## Current Execution Baseline
SmartCloset의 현재 기준은 MVP-3 완료 baseline Spring Boot 4.0.6 서비스다. 구현 기준은 seed/test-user 기반 동작이 아니라 인증 사용자 기반 동작이다. MVP4 변경은 `docs/PRD.md`와 ADR에서 승인된 뒤 적용한다.

다음 현재 요구사항을 기준으로 구현하고 리뷰한다.

- Spring Security + JWT Bearer access token 인증을 사용한다.
- 공개 API는 `POST /api/auth/signup`, `POST /api/auth/login`만 허용한다.
- 그 외 모든 API는 보호 API이며 `Authorization: Bearer {accessToken}`이 필요하다.
- Controller는 인증 principal에서 현재 사용자를 식별한다.
- 공개 HTTP API는 `userId` query parameter를 받지 않는다.
- 현재 사용자 전용 response DTO는 `userId`를 노출하지 않는다.
- Application service와 repository는 소유권 확인과 query를 위해 내부적으로 `Long userId` 값을 사용할 수 있다.
- 회원가입은 role `USER`, 기본 위치 `SEOUL`, 빈 선호도 배열을 가진 사용자를 생성한다.
- 사용자 소유 옷장 데이터, 위치, 선호도, 추천 이력, 착용 이력은 인증 사용자별로 분리한다.
- 추천 생성은 `POST /api/recommendations`를 사용한다.
- 추천 이력은 `GET /api/recommendations?limit={limit}`를 사용하며 기본값 `20`, 최소 `1`, 최대 `50`, 최신순으로 정렬한다.
- React frontend는 access token을 `sessionStorage`에 저장한다.
- Docker Compose는 필수 공유 흐름으로 유지한다.
- MVP-3 완료 baseline 로컬 Docker Compose 전환 시 `docker compose up --build` 전에 `docker compose down -v` 실행을 권장한다.

## Historical Context
Historical context는 코드베이스가 현재 형태가 된 배경을 설명할 뿐이다. 어떤 경우에도 `Current Execution Baseline`을 override하지 않는다.

- 1차 MVP는 seed/test-user 흐름으로 규칙 기반 추천 백엔드를 검증했다.
- 1.5차 MVP는 KMA `getVilageFcst` JSON weather provider와 fallback 동작을 도입했다.
- 2차 MVP는 사용자 위치 저장, 내장 KMA 위치 catalog, React frontend를 추가했다.

현재 baseline 또는 MVP4 작업을 구현할 때 과거 seed/test-user API 계약, 과거 공개 `userId` query parameter, 과거 점수 필드를 되살리지 않는다.

## Harness Step PR Rules
완료된 MVP-3 phase의 최종 기준과 Harness 중간 step PR 기준을 구분한다.

- 최종 `Current Execution Baseline`은 phase 전체 완료 기준이다.
- 중간 step PR을 구현하거나 리뷰할 때는 `phases/{phase}/README.md`와 해당 `stepN.md`의 작업, 인수 기준, 금지사항을 우선한다.
- 미래 step에 명시된 기능이 현재 step에 없다는 이유만으로 blocker로 판단하지 않는다.
- 현재 step이 미래 step 범위를 선행 구현하면 blocker로 판단한다.
- 리뷰 실패 수정은 현재 step 범위 안에서 해결해야 하며, 미래 step 기능을 끌어와 해결하지 않는다.
- Step 1 같은 중간 auth step에서는 해당 step이 허용한 auth endpoint와 current-user endpoint만 평가하고, preferences API, recommendation history, `preferenceScore`, frontend session flow 같은 후속 step 산출물을 요구하지 않는다.

## Strict Out of Scope
아래 항목은 현재 baseline에서 구현하지 않았고, MVP4 문서/ADR 승인 전에는 현재 작업의 일부로 제안하지 않고 현재 문서/API/프론트엔드 범위에 추가하지 않는다.

- refresh token
- social login
- email verification
- password reset
- admin features
- external address/map APIs
- browser/current-location auto detection
- latitude/longitude to KMA grid conversion APIs
- KMA `getVilageFcst` 외 weather APIs
- weather source DB persistence
- Redis
- AWS deployment
- CD automation
- AI/GPT recommendations
- image upload
- shopping recommendations
- preference normalization tables
- styleTags scoring
- styleTags recommendation reasons

## API Rules
- 공개 API와 보호 API 표/계약을 분리해서 유지한다.
- 공개 API는 `POST /api/auth/signup`, `POST /api/auth/login`만 허용한다.
- 보호 API는 `Authorization: Bearer {accessToken}`이 필요하다.
- 공개 `userId` request parameter를 추가하지 않는다.
- 현재 사용자 전용 response DTO에 `userId`를 노출하지 않는다.
- 성공 응답은 `{ "data": ... }` 형태를 유지한다.
- 실패 응답은 `{ "code": "...", "message": "...", "details": [] }` 형태를 유지한다.
- `details`는 항상 배열이다.
- 추천 생성은 `POST /api/recommendations`다.
- today recommendation GET endpoint를 추가하거나 문서화하지 않는다.
- 추천 이력은 `GET /api/recommendations?limit={limit}`다.
- 추천 이력 `limit`는 기본값 `20`, 허용 범위 `1..50`, 최신순 정렬이며 잘못된 값은 `400 INVALID_REQUEST`로 실패한다.
- 위치 catalog 조회는 `GET /api/locations?keyword={keyword}`이며 보호 API다.
- 현재 사용자 위치 API는 `GET /api/users/me/location`, `PUT /api/users/me/location`이다.
- 현재 사용자 선호도 API는 `GET /api/users/me/preferences`, `PUT /api/users/me/preferences`이다.
- 옷 API는 현재 사용자 전용 API다: `GET/POST /api/clothes`, `GET/PUT /api/clothes/{clothingId}`, `PATCH /api/clothes/{clothingId}/archive`.
- 착용 완료는 `PATCH /api/recommendations/{recommendationId}/worn`이며 idempotent해야 한다.
- 옷 archive도 idempotent해야 한다.
- 추천 business failure는 HTTP `422 Unprocessable Entity`를 사용한다.

## Auth Rules
- Spring Security를 사용한다.
- JWT Bearer access token 인증을 사용한다.
- refresh token은 구현하지 않는다.
- JWT access token은 `HS256`으로 서명하고 `JWT_SECRET`을 사용한다.
- JWT subject는 현재 사용자 id 문자열이며 claims는 `email`, `role`만 둔다.
- JWT access token 만료 시간은 2시간으로 고정한다.
- password는 BCrypt hash로 저장한다.
- 기본 role은 `USER`다.
- 회원가입은 기본 위치와 빈 선호도를 초기화해야 한다.
- 로그인은 access token과 현재 사용자 정보를 반환한다.
- Frontend는 access token을 `sessionStorage`에 저장한다.
- 로그아웃은 frontend token과 user state 제거로 처리한다.
- token 없음, 잘못된 token, 만료된 token은 보호 API에서 authentication failure로 실패해야 한다.

## Domain Rules
- business rule을 controller에 두지 않는다.
- Controller는 HTTP, validation, principal extraction, DTO mapping을 담당한다.
- Application service는 use case와 transaction을 조율한다.
- Repository는 persistence만 담당한다.
- Repository에는 recommendation score calculation, outfit generation, KMA mapping, location catalog search policy를 두지 않는다.
- Recommendation calculation은 domain service에 둔다:
  - `WeatherSuitabilityFilter`
  - `OutfitCandidateGenerator`
  - `RecommendationScorer`
  - `RecommendationReasonGenerator`
- `OutfitCandidate`는 calculation model/value object이며 DB entity가 아니다.
- 모든 사용자 소유 read/write는 인증 사용자로 제한해야 한다.

## Weather Rules
- `WeatherProvider` interface에 의존한다.
- KMA `getVilageFcst` JSON만 유일한 외부 weather API로 유지한다.
- `KmaVilageForecastWeatherProvider`를 primary provider로 유지한다.
- `StaticWeatherProvider`를 fallback/test provider로 유지한다.
- Fallback weather는 `temperature=12`, `weatherType=CLOUDY`, `rainy=false`, `windy=false`다.
- KMA `TMP`, `SKY`, `PTY`, `PCP`, `WSD`를 내부 `WeatherCondition`으로 mapping한다.
- Recommendation domain은 KMA DTO가 아니라 내부 `WeatherCondition`에만 의존해야 한다.
- KMA request `nx`, `ny`는 인증 사용자의 저장 위치에서 온다.
- `KMA_NX`, `KMA_NY`는 compatibility/default helper일 뿐 recommendation source of truth가 아니다.
- 사용자에게 location snapshot이 없으면 현재 문서 기준에 따라 Seoul `SEOUL`, `60`, `127`로 backfill하거나 사용한다.
- `WEATHER_FALLBACK_ENABLED=false`는 strict KMA mode이며 조용히 fallback하면 안 된다.

## Location Rules
- 내장 KMA 대표 격자 catalog를 사용한다.
- 외부 address/map API를 추가하지 않는다.
- browser/current-location detection을 추가하지 않는다.
- latitude/longitude conversion API를 추가하지 않는다.
- 최소 catalog는 `SEOUL`, `BUSAN`, `DAEGU`, `INCHEON`, `GWANGJU`, `DAEJEON`, `ULSAN`, `SEJONG`, `JEJU`를 포함한다.
- `GET /api/locations`는 보호 API이며 로그인 후에만 사용한다.
- 회원가입은 location catalog를 호출하지 않는다.
- 신규 사용자는 Seoul `SEOUL`, `nx=60`, `ny=127`로 시작한다.
- 잘못된 location code는 `LOCATION_NOT_FOUND`로 실패한다.
- 기존 사용자에게 location data가 없어 backfill할 때는 write transaction이 필요하다.

## Preference Rules
- 선호도는 `users`의 JSON string column에 저장한다:
  - `preferred_colors_json`
  - `preferred_materials_json`
  - `style_tags_json`
- API DTO는 배열을 사용한다:
  - `preferredColors`
  - `preferredMaterials`
  - `styleTags`
- 신규 사용자는 다음 값으로 시작한다:
  - `preferredColors: []`
  - `preferredMaterials: []`
  - `styleTags: []`
- 현재 baseline에서는 선호도를 별도 table로 정규화하지 않는다.
- `preferredColors`와 `preferredMaterials`만 `preferenceScore`에 영향을 준다.
- `styleTags`는 저장, 반환, 표시만 한다.
- `styleTags`는 score, tie-breaker, candidate generation, filter, recommendation reason에 영향을 주면 안 된다.

## Frontend Rules
- `frontend/`는 React+Vite+TypeScript SPA로 유지한다.
- TypeScript `strict`를 사용한다.
- API request/response DTO를 명시적으로 유지한다.
- 로그인 전에 보호 API를 호출하지 않는다.
- 회원가입 또는 로그아웃 화면에서 `GET /api/locations`를 호출하지 않는다.
- Access token은 `sessionStorage`에 저장한다.
- 새로고침 후 저장된 token으로 `GET /api/users/me`를 호출해 로그인 상태를 복구한다.
- `GET /api/locations`의 `401`은 location search failure가 아니라 authentication expiration으로 처리한다.
- React state와 작은 hook을 사용한다. 현재 baseline에서는 큰 state-management library를 추가하지 않는다.
- Frontend 동작은 `docs/FRONTEND.md`를 따른다.

## Recommendation Rules
- `docs/RECOMMENDATION_RULES.md`를 따른다.
- Total score는 100점이다.
- `weatherScore` 최대값은 35점이다.
- `colorScore` 최대값은 25점이다.
- `wearHistoryScore` 최대값은 20점이다.
- `recommendationHistoryScore` 최대값은 10점이다.
- `preferenceScore` 최대값은 10점이다.
- `preferenceScore`는 candidate item 중 하나 이상이 `preferredColors`와 일치하면 5점이다.
- `preferenceScore`는 candidate item 중 하나 이상이 `preferredMaterials`와 일치하면 5점이다.
- 선호 색상 배열과 선호 소재 배열이 모두 비어 있으면 `preferenceScore=0`이다.
- scoring 또는 recommendation reason에 styleTags를 사용하지 않는다.
- Recommendation reason은 template 기반이며 AI-generated가 아니다.
- Recommendation reason은 3개에서 5개를 생성한다.
- Tie-break rule은 deterministic해야 한다.
- 같은 input은 같은 recommendation result를 만들어야 한다.

## Entity and JPA Rules
- `docs/ERD.md`를 따른다.
- 모든 entity는 `BaseTimeEntity`를 사용한다.
- Enum은 `VARCHAR`로 저장한다.
- `recommendation_results.reasons_json`은 DB JSON과 entity `String reasonsJson`으로 저장한다.
- Preference JSON string column은 entity `String` field로 저장한다.
- Entity에는 Lombok `@Data`를 사용하지 않는다.
- Entity setter를 남용하지 않는다.
- `@Getter`, protected no-args constructor, 의도를 드러내는 method를 선호한다.
- Entity mutation method는 의도를 명확히 표현해야 한다. 예:
  - `updateDetails`
  - `archive`
  - `markWorn`
  - `updateLocation`
  - `updatePreferences`
- User email은 unique해야 한다.
- User password storage는 plaintext가 아니라 `password_hash`를 사용해야 한다.

## Test Rules
- 동작 변경 범위에 비례해 test를 추가하거나 수정한다.
- Auth 작업에는 authentication success/failure test가 필요하다.
- Security 작업에는 보호 API `401`/`403` test가 필요하다.
- 사용자 소유 resource를 건드릴 때는 user data isolation test가 필요하다.
- 현재 사용자 전용 response DTO test는 `userId`가 노출되지 않음을 검증해야 한다.
- Scoring을 건드릴 때는 recommendation score unit test가 필요하다.
- `preferenceScore` test는 empty preferences, color match, material match, both matches를 포함해야 한다.
- Test는 styleTags가 score 또는 reason에 영향을 주지 않음을 증명해야 한다.
- Weather suitability를 건드릴 때는 weather filtering test가 필요하다.
- Recommendation rule을 건드릴 때는 color, material, temperature rule test가 필요하다.
- Candidate generation 또는 filtering을 건드릴 때는 recommendation failure code test가 필요하다.
- Recommendation history limit test는 default, min, max, invalid limit를 포함해야 한다.
- Deterministic recommendation test는 유지해야 한다.
- P0 API는 integration, controller, service test 중 하나로 cover해야 한다.

## Documentation Sync Rules
- API behavior가 바뀌면 `docs/API.md`, `README.md`, `docs/DEMO_SCENARIO.md`를 확인한다.
- Frontend behavior가 바뀌면 `docs/FRONTEND.md`, `README.md`, `docs/SHARING_GUIDE.md`를 확인한다.
- Recommendation scoring이 바뀌면 `docs/RECOMMENDATION_RULES.md`, `docs/API.md`, `docs/ERD.md`를 확인한다.
- 현재 API 계약에서 공개 `userId` query parameter가 노출되면 제거한다.
- 현재 사용자 전용 response DTO 예시와 frontend type에서 `userId`를 제거한다.
- Recommendation creation은 `POST /api/recommendations`로만 문서화한다.
- Today recommendation GET path가 현재 API 계약으로 나타나면 제거한다.
- Preference storage는 `users` JSON string column으로 문서화한다.
- `styleTags`는 storage/display only로 문서화한다.
- `GET /api/locations`는 보호 API이며 로그인 후 API로 문서화한다.
- MVP-3 완료 baseline 전환용 Docker Compose DB reset 안내를 sharing/demo/command 문서에 유지한다.
- Out-of-scope feature를 현재 문서에 추가하지 않는다.
- 실제 API key, token, password, private key, production secret을 커밋하지 않는다.

## Implementation Attitude
- `Current Execution Baseline`을 현재 활성 rule set으로 취급한다.
- `Historical Context`는 배경 정보로만 취급한다.
- Archive와 오래된 ADR 표현보다 현재 docs를 우선한다.
- Seed/test-user behavior를 구현 shortcut으로 다시 도입하지 않는다.
- 현재 docs가 명시적으로 요구하지 않는 한 과거 public API shape를 compatibility 명목으로 유지하지 않는다.
- 변경은 요청된 behavior 범위로 제한한다.
- 기존 package boundary와 local pattern을 우선한다.
- 불필요한 abstraction을 피한다.
- P1 polish보다 P0 behavior를 먼저 구현한다.
- 구현 후 관련 test를 실행하고 README/demo scenario를 확인한다.

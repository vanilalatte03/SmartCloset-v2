---
name: smartcloset-backend
description: SmartCloset Spring Boot 4.0.6 백엔드 구현 또는 리뷰 시 사용한다. 인증 사용자 API, 규칙 기반 옷차림 추천, KMA 날씨 provider/fallback weather, 사용자 위치 API, 선호도, 옷 이미지 업로드, 추천 피드백/개인화, 테스트, Docker Compose 공유, 문서 동기화를 포함한다.
---

# SmartCloset Backend Skill

## Purpose

SmartCloset 현재 baseline 백엔드/프론트엔드 연동 작업을 구현하거나 리뷰하기 전에 반드시 따르는 실행 규칙이다.

이 스킬은 기획 기록이 아니다. Codex가 따라야 하는 현재 구현 기준을 정의한다. 이 스킬이 과거 MVP 메모, 완료된 phase 문서, archive 문서, 오래된 ADR 표현, seed/test-user 흐름과 충돌하면 `Current Execution Baseline`과 현재 루트 `README.md` 및 `docs/`를 우선한다.

## Reading Policy

모든 작업에서는 먼저 다음 섹션을 확인한다.

- Purpose
- Current Execution Baseline
- Harness Step PR Rules
- Strict Out of Scope
- Implementation Attitude

작업 성격에 따라 필요한 섹션을 추가로 확인한다.

- API 변경: API Rules
- 인증/보안 변경: Auth Rules, API Rules, Test Rules
- 추천 변경: Recommendation Rules, Weather Rules, Preference Rules, Test Rules
- 위치 변경: Location Rules, Weather Rules, API Rules
- 옷 이미지 변경: Clothing Image Rules, API Rules, Frontend Rules, Test Rules
- 프론트 변경: Frontend Rules, API Rules
- DB/entity 변경: Entity and JPA Rules, 관련 API/Domain Rules
- 테스트 변경: Test Rules와 변경 대상 도메인 섹션
- 문서 변경: Documentation Sync Rules와 변경 대상 문서 관련 섹션

Historical Context는 현재 기준이 헷갈릴 때만 참고한다. Historical Context는 어떤 경우에도 Current Execution Baseline을 override하지 않는다.

## Current Execution Baseline

SmartCloset의 현재 기준은 MVP7 위치/날씨 신뢰도 문서 전환 baseline Spring Boot 4.0.6 서비스다. MVP7 계약은 `docs/PRD.md`와 ADR-012를 따른다. 현재 코드 출발점은 MVP6 추천 피드백/개인화 완료 상태이며, MVP7 구현은 `phases/7-smartcloset-location-weather-trust` step 문서를 따른다.

다음 현재 요구사항을 기준으로 구현하고 리뷰한다.

- Spring Security + JWT Bearer access token 인증을 사용한다.
- 공개 API는 `POST /api/auth/signup`, `POST /api/auth/login`만 허용한다.
- 그 외 모든 API는 보호 API이며 `Authorization: Bearer {accessToken}`이 필요하다.
- Controller는 인증 principal에서 현재 사용자를 식별한다.
- 공개 HTTP API는 `userId` query parameter를 받지 않는다.
- 현재 사용자 전용 response DTO는 `userId`를 노출하지 않는다.
- 회원가입은 role `USER`, 기본 위치 `SEOUL`, 빈 선호도 배열을 가진 사용자를 생성한다.
- 사용자 소유 옷장 데이터, 위치, 선호도, 추천 이력, 착용 이력, 추천 피드백은 인증 사용자별로 분리한다.
- 추천 생성은 `POST /api/recommendations`를 사용하며 선택 body `{ "situation": "WORK", "forecastPeriod": "AFTERNOON" }`를 받을 수 있다.
- 추천 생성 body가 없거나 `situation`이 누락되면 `CASUAL`을 사용한다.
- 추천 생성 body가 없거나 `forecastPeriod`가 누락되면 `CURRENT`를 사용한다.
- 추천 이력은 `GET /api/recommendations?limit={limit}`를 사용하며 기본값 `20`, 최소 `1`, 최대 `50`, 최신순으로 정렬한다.
- 추천 피드백 저장/clear는 `PUT /api/recommendations/{recommendationId}/feedback`를 사용한다.
- 추천 피드백 PUT은 전체 교체이며 누락 필드는 `null`로 간주한다.
- 현재 날씨 요약은 `GET /api/weather/current` 보호 API로 조회한다.
- 현재 날씨 요약은 인증 사용자 위치와 `CURRENT` 기준 `WeatherResponse`를 반환하며 추천 결과를 생성하거나 저장하지 않는다.
- React frontend는 access token을 `sessionStorage`에 저장한다.
- MVP5부터 옷 1개당 이미지 1장 업로드를 지원한다.
- 기존 옷 등록/수정 JSON API는 유지한다.
- MVP6에서는 옷 등록/수정/응답에 `styleTags` 배열을 포함한다.
- MVP7에서는 KMA 행정구역 catalog 검색, 브라우저 좌표 resolve, 위치 source, 예보 시간대, 위치/날씨 source snapshot을 추가한다.
- 이미지 업로드/교체/조회/삭제 API는 모두 보호 API다.
- 이미지 존재 여부는 추천 점수, 후보 필터링, 추천 이유에 영향을 주지 않는다.
- Docker Compose는 필수 공유 흐름으로 유지한다.
- 로컬 Docker Compose baseline 전환 시 `docker compose up --build` 전에 `docker compose down -v` 실행을 권장한다.

## Historical Context

- 1차 MVP는 seed/test-user 흐름으로 규칙 기반 추천 백엔드를 검증했다.
- 1.5차 MVP는 KMA `getVilageFcst` JSON weather provider와 fallback 동작을 도입했다.
- 2차 MVP는 사용자 위치 저장, 내장 KMA 위치 catalog, React frontend를 추가했다.
- MVP3는 Spring Security, JWT Bearer access token, 인증 사용자 API, 선호도, `preferenceScore`를 추가했다.
- MVP4는 반응형 실사용 UX와 `GET /api/weather/current`를 추가했다.
- MVP5는 옷 이미지 업로드/교체/조회/삭제, 기본 옷 프리셋 이미지, 추천/이력 썸네일, Docker Compose 이미지 volume을 추가했다.
- MVP6는 추천 상황, 옷별 styleTags, 추천 피드백 snapshot, 최근 피드백 기반 `preferenceScore`, 추천 이력의 착용/피드백 표시를 추가한다.
- MVP7은 KMA 행정구역 catalog, 브라우저 좌표 resolve, forecastPeriod, 위치/날씨 source snapshot, KMA/fallback/base/forecast 표시를 추가한다.

현재 작업에서 과거 seed/test-user API 계약, 공개 `userId` query parameter, 과거 점수 필드를 되살리지 않는다.

## Harness Step PR Rules

- 최종 `Current Execution Baseline`은 phase 전체 완료 기준이다.
- 중간 step PR을 구현하거나 리뷰할 때는 `phases/{phase}/README.md`와 해당 `stepN.md`의 작업, 인수 기준, 금지사항을 우선한다.
- 미래 step에 명시된 기능이 현재 step에 없다는 이유만으로 blocker로 판단하지 않는다.
- 현재 step이 미래 step 범위를 선행 구현하면 blocker로 판단한다.
- 리뷰 실패 수정은 현재 step 범위 안에서 해결해야 하며, 미래 step 기능을 끌어와 해결하지 않는다.

## Strict Out of Scope

아래 항목은 MVP7 범위에 추가하지 않는다.

- refresh token
- social login
- email verification
- password reset
- admin features
- external address/map APIs
- KMA `getVilageFcst` 외 weather APIs
- raw KMA response DB persistence
- browser GPS coordinate DB persistence
- Redis
- AWS deployment
- CD automation
- AI/GPT recommendations
- AI automatic tagging
- feedback event log analytics
- multiple images per clothing item
- image editing/cropping/resizing pipeline
- image compression pipeline
- image EXIF analysis
- image moderation
- S3/CDN/external image hosting
- image-based recommendation scoring
- image-based recommendation reasons
- shopping recommendations
- preference normalization tables

## API Rules

- 공개 API와 보호 API 표/계약을 분리해서 유지한다.
- 공개 API는 `POST /api/auth/signup`, `POST /api/auth/login`만 허용한다.
- 보호 API는 `Authorization: Bearer {accessToken}`이 필요하다.
- 공개 `userId` request parameter를 추가하지 않는다.
- 현재 사용자 전용 response DTO에 `userId`를 노출하지 않는다.
- 성공 JSON 응답은 `{ "data": ... }` 형태를 유지한다.
- 실패 응답은 `{ "code": "...", "message": "...", "details": [] }` 형태를 유지한다.
- `details`는 항상 배열이다.
- 추천 생성은 `POST /api/recommendations`다.
- 추천 생성 request body는 선택이며 `situation`과 `forecastPeriod`를 받을 수 있다.
- 추천 생성 body가 없거나 `situation`이 누락되면 `CASUAL`이다.
- 추천 생성 body가 없거나 `forecastPeriod`가 누락되면 `CURRENT`다.
- today recommendation GET endpoint를 추가하거나 문서화하지 않는다.
- 추천 이력은 `GET /api/recommendations?limit={limit}`다.
- 현재 날씨 요약은 `GET /api/weather/current`다.
- 위치 검색은 `GET /api/locations?keyword={keyword}`다.
- 브라우저 좌표 resolve는 `POST /api/locations/resolve`다.
- 위치 저장은 `PUT /api/users/me/location`이며 optional `source`를 받을 수 있다.
- 옷 API는 현재 사용자 전용 API다: `GET/POST /api/clothes`, `GET/PUT /api/clothes/{clothingId}`, `PATCH /api/clothes/{clothingId}/archive`.
- 옷 등록/수정/응답 DTO는 `styleTags` 배열을 포함한다.
- 옷 이미지 API는 `PUT /api/clothes/{clothingId}/image`, `GET /api/clothes/{clothingId}/image`, `DELETE /api/clothes/{clothingId}/image`다.
- 이미지 업로드는 multipart part name `image`를 사용한다.
- 이미지 bytes 조회는 인증과 소유권 확인 후 image content type과 bytes를 반환한다.
- 착용 완료는 `PATCH /api/recommendations/{recommendationId}/worn`이며 idempotent해야 한다.
- 추천 피드백은 `PUT /api/recommendations/{recommendationId}/feedback`이며 현재 사용자 소유 추천만 수정한다.
- 추천 피드백 PUT은 전체 교체이고 누락 필드는 `null`로 간주한다.
- `sentiment`와 `thermal`이 모두 `null`이면 피드백 clear다.
- 옷 archive와 이미지 삭제는 idempotent해야 한다.
- 추천 business failure는 HTTP `422 Unprocessable Entity`를 사용한다.

## Auth Rules

- Spring Security를 사용한다.
- JWT Bearer access token 인증을 사용한다.
- refresh token은 구현하지 않는다.
- JWT access token은 `HS256`으로 서명하고 `JWT_SECRET`을 사용한다.
- JWT subject는 현재 사용자 id 문자열이며 claims는 `email`, `role`만 둔다.
- JWT access token 만료 시간은 2시간으로 고정한다.
- password는 BCrypt hash로 저장한다.
- Frontend는 access token을 `sessionStorage`에 저장한다.
- token 없음, 잘못된 token, 만료된 token은 보호 API에서 authentication failure로 실패해야 한다.

## Clothing Image Rules

- 옷 1개당 이미지는 최대 1장이다.
- 기존 옷 등록/수정 JSON API를 multipart로 대체하지 않는다.
- 이미지 업로드/교체는 `PUT /api/clothes/{clothingId}/image`로 처리한다.
- 이미지 조회는 `GET /api/clothes/{clothingId}/image`로 처리한다.
- 이미지 삭제는 `DELETE /api/clothes/{clothingId}/image`로 처리한다.
- 이미지 API는 현재 인증 사용자 소유 옷만 접근할 수 있다.
- 다른 사용자 옷 또는 존재하지 않는 옷은 `CLOTHING_NOT_FOUND`로 실패한다.
- 내 옷이지만 이미지가 없으면 `CLOTHING_IMAGE_NOT_FOUND`로 실패한다.
- 삭제는 이미지가 없어도 성공해야 한다.
- 파일 bytes는 DB가 아니라 로컬 파일 시스템 또는 Docker Compose volume에 저장한다.
- DB에는 `clothing_items` 이미지 메타데이터 컬럼만 둔다.
- 허용 파일은 5MB 이하 jpg/jpeg/png/webp다.
- MIME type은 `image/jpeg`, `image/png`, `image/webp`만 허용한다.
- Spring multipart limit은 앱 validator 기준인 `CLOTHING_IMAGE_MAX_SIZE_BYTES`보다 작게 설정하지 않는다.
- 파일 크기 초과와 multipart size 초과는 `400 INVALID_REQUEST`와 `details` 배열로 실패해야 한다.
- 원본 파일명을 저장 경로에 사용하지 않는다.
- 서버 생성 UUID 기반 저장 파일명을 사용한다.
- AI 자동 태깅은 구현하지 않는다.

## Domain Rules

- business rule을 controller에 두지 않는다.
- Controller는 HTTP, validation, principal extraction, DTO mapping을 담당한다.
- Application service는 use case와 transaction을 조율한다.
- Repository는 persistence만 담당한다.
- Repository에는 recommendation score calculation, outfit generation, KMA mapping, location catalog search policy를 두지 않는다.
- Recommendation calculation은 domain service에 둔다.
- `OutfitCandidate`는 calculation model/value object이며 DB entity가 아니다.
- 모든 사용자 소유 read/write는 인증 사용자로 제한해야 한다.

## Weather Rules

- `WeatherProvider` interface에 의존한다.
- KMA `getVilageFcst` JSON만 유일한 외부 weather API로 유지한다.
- `KmaVilageForecastWeatherProvider`를 primary provider로 유지한다.
- `StaticWeatherProvider`를 fallback/test provider로 유지한다.
- Fallback weather는 `temperature=12`, `weatherType=CLOUDY`, `rainy=false`, `windy=false`다.
- KMA request `nx`, `ny`는 인증 사용자의 저장 위치에서 온다.
- MVP7 weather application 계약은 weather condition과 location/source metadata를 함께 다룬다.
- Weather source metadata는 provider, KMA 사용 여부, fallback 여부, base date/time, forecast date/time을 포함한다.
- raw KMA 응답 JSON은 저장하거나 response DTO로 노출하지 않는다.
- `GET /api/weather/current`는 추천 결과, 추천 이력, 착용 이력을 생성하지 않는다.

## Location Rules

- KMA 행정구역 격자 catalog를 사용한다.
- 외부 address/map API를 추가하지 않는다.
- 브라우저 current-location은 프론트 Geolocation API와 서버 `POST /api/locations/resolve`로 후보를 찾는 데만 사용한다.
- 브라우저 GPS 원문 좌표를 DB에 저장하지 않는다.
- 위경도 -> KMA grid 변환은 서버 내부 로직으로 구현한다.
- 위치 저장 source는 `MANUAL_SEARCH`, `BROWSER_GEOLOCATION`이다.
- `GET /api/locations`는 보호 API이며 로그인 후에만 사용한다.
- 신규 사용자는 Seoul `SEOUL`, `nx=60`, `ny=127`로 시작한다.

## Preference Rules

- 선호도는 `users`의 JSON string column에 저장한다.
- API DTO는 `preferredColors`, `preferredMaterials`, `styleTags` 배열을 사용한다.
- 신규 사용자는 모두 빈 배열로 시작한다.
- `preferredColors`, `preferredMaterials`, `styleTags`는 MVP6 `preferenceScore`에 영향을 준다.
- 사용자 선호 `styleTags`와 옷별 `styleTags`는 trim 후 비교하고 ASCII는 case-insensitive로 비교한다.
- blank style tag는 저장하지 않는다.

## Frontend Rules

- `frontend/`는 React+Vite+TypeScript SPA로 유지한다.
- TypeScript `strict`를 사용한다.
- API request/response DTO를 명시적으로 유지한다.
- 로그인 전에 보호 API를 호출하지 않는다.
- Access token은 `sessionStorage`에 저장한다.
- React state와 작은 hook을 사용한다. 큰 state-management library를 추가하지 않는다.
- 이미지 조회는 Authorization header가 필요하므로 blob fetch와 object URL을 사용한다.
- 일반 public `<img src>`로 보호 이미지를 직접 참조하지 않는다.
- object URL은 cleanup해야 한다.
- Frontend 동작은 `docs/FRONTEND.md`를 따른다.

## Recommendation Rules

- `docs/RECOMMENDATION_RULES.md`를 따른다.
- Total score는 100점이다.
- `weatherScore` 최대값은 35점이다.
- `colorScore` 최대값은 25점이다.
- `wearHistoryScore` 최대값은 20점이다.
- `recommendationHistoryScore` 최대값은 10점이다.
- `preferenceScore` 최대값은 10점이다.
- MVP6 `preferenceScore`는 색상 0/2, 소재 0/2, styleTags 0..3, 최근 피드백 -3..3 보정을 clamp해 계산한다.
- 최근 피드백 window는 14일이다.
- 추천 상황은 `WORK`, `CASUAL`, `WORKOUT`, `DATE`, `FORMAL`이다.
- 예보 시간대는 `CURRENT`, `MORNING`, `AFTERNOON`, `EVENING`이다.
- forecastPeriod는 weather input 선택에만 관여하며 score field를 새로 만들지 않는다.
- scoring, filtering, tie-break, recommendation reason에 image metadata를 사용하지 않는다.
- Recommendation reason은 template 기반이며 AI-generated가 아니다.
- Tie-break rule은 deterministic해야 한다.

## Entity and JPA Rules

- `docs/ERD.md`를 따른다.
- 모든 entity는 `BaseTimeEntity`를 사용한다.
- Enum은 `VARCHAR`로 저장한다.
- Entity에는 Lombok `@Data`를 사용하지 않는다.
- Entity setter를 남용하지 않는다.
- Entity mutation method는 의도를 명확히 표현해야 한다.
- Clothing image metadata mutation method는 업로드/삭제 의도를 드러내야 한다.

## Test Rules

- 동작 변경 범위에 비례해 test를 추가하거나 수정한다.
- Auth 작업에는 authentication success/failure test가 필요하다.
- 사용자 소유 resource를 건드릴 때는 user data isolation test가 필요하다.
- 이미지 API에는 인증 누락, 타 사용자 접근, 이미지 없음, 업로드 성공, 교체, 삭제 idempotency test가 필요하다.
- 파일 검증 test는 빈 파일, 크기 초과, 잘못된 확장자, 잘못된 MIME type, signature 불일치를 포함해야 한다.
- 추천 DTO에 image metadata를 추가해도 추천 점수와 reason이 변하지 않음을 테스트해야 한다.
- 추천 피드백 API에는 전체 교체, 누락 필드 null 처리, clear, 타 사용자 접근 차단 test가 필요하다.
- styleTags와 최근 피드백이 `preferenceScore`와 추천 이유에 반영됨을 테스트해야 한다.
- 위치 검색 test에는 `일산동` 같은 동명이인 후보 반환이 필요하다.
- 좌표 resolve test에는 정상 좌표와 invalid latitude/longitude 실패가 필요하다.
- weather source test에는 KMA 성공과 fallback metadata가 필요하다.
- 추천 snapshot test에는 사용자 위치 변경 후 과거 추천 location/source snapshot 불변 검증이 필요하다.
- P0 API는 integration, controller, service test 중 하나로 cover해야 한다.

## Documentation Sync Rules

- MVP 또는 phase 범위가 바뀌면 `docs/MVP_CHANGE_CHECKLIST.md`를 먼저 확인한다.
- API behavior가 바뀌면 `docs/API.md`, `README.md`, `docs/DEMO_SCENARIO.md`를 확인한다.
- Frontend behavior가 바뀌면 `docs/FRONTEND.md`, `README.md`, `docs/SHARING_GUIDE.md`를 확인한다.
- DB/entity가 바뀌면 `docs/ERD.md`, `docs/ARCHITECTURE.md`를 확인한다.
- Recommendation scoring이 바뀌면 `docs/RECOMMENDATION_RULES.md`, `docs/API.md`, `docs/ERD.md`를 확인한다.
- 현재 API 계약에서 공개 `userId` query parameter가 노출되면 제거한다.
- 현재 사용자 전용 response DTO 예시와 frontend type에서 `userId`를 제거한다.
- Recommendation creation은 `POST /api/recommendations`로만 문서화한다.
- Today recommendation GET path가 현재 API 계약으로 나타나면 제거한다.
- MVP7 위치 검색은 내부 KMA catalog 기준으로 문서화하고 외부 지도/주소 API를 사용하지 않는다.
- MVP7 브라우저 현재 위치는 좌표 resolve 후보 찾기로만 문서화하고 GPS 원문 DB 저장을 추가하지 않는다.
- MVP7 weather source snapshot은 raw KMA 응답 JSON 없이 provider, KMA/fallback 여부, base/forecast 시각만 문서화한다.
- Image upload는 MVP5 승인 범위로 유지하지만 AI 자동 태깅, 다중 이미지, S3/CDN, 이미지 기반 추천 점수/이유, EXIF 분석, image moderation은 제외 범위로 문서화한다.
- MVP별 자동 문서 검증 규칙은 `scripts/checks.py`가 아니라 `phases/{phase}/docs-checks.json`에 둔다.
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

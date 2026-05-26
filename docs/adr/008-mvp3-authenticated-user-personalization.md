# Use Authenticated User APIs and Preference Score

## Status
Accepted

## Context
SmartCloset 2차 MVP는 사용자별 위치와 React 프론트엔드를 제공했지만 공개 API 계약은 테스트용 `userId` request parameter에 의존했다. 이 구조는 사용자별 데이터 분리는 가능하지만 실제 사용자 서비스처럼 보이지 않고, 프론트도 고정 `userId=1` 흐름에서 벗어나기 어렵다.

MVP-3은 회원가입/로그인과 인증 사용자 기준 API로 전환해 사용자별 옷장, 위치, 추천 이력, 착용 이력, 선호도를 분리해야 한다.

또한 추천 점수의 10점 보정 항목을 반복 추천 감소용 기존 다양성 점수에서 개인화 기반 `preferenceScore`로 전환한다.

## Decision
MVP-3은 Spring Security와 JWT Bearer access token을 사용한다.

- 공개 API는 `POST /api/auth/signup`, `POST /api/auth/login`만 둔다.
- 그 외 API는 보호 API로 두고 `Authorization: Bearer {accessToken}` header를 요구한다.
- JWT는 access token 단일 구조로 시작한다.
- refresh token은 MVP-3 범위에서 제외한다.
- 프론트 access token 저장 위치는 `sessionStorage`로 고정한다.
- JWT access token은 `HS256`으로 서명하고 `JWT_SECRET`을 사용한다.
- JWT subject는 현재 사용자 id 문자열이며 claims는 `email`, `role`만 둔다.
- JWT access token 만료 시간은 2시간으로 고정한다.
- 비밀번호는 BCrypt hash로 저장한다.
- 기본 role은 `USER`다.

`userId` 제거는 두 기준으로 분리한다.

- Query parameter 제거: HTTP 공개 계약에서 `?userId=`를 제거하고 인증 principal에서 현재 사용자를 식별한다.
- Response DTO 제거: 현재 사용자 전용 응답에서 `userId` 필드를 제거한다.

`GET /api/locations`는 보호 API로 고정한다. 회원가입 화면에서는 위치 catalog를 호출하지 않고, 신규 사용자는 기본 위치 `SEOUL`, `nx=60`, `ny=127`로 생성한다.

선호도는 `users` 테이블의 JSON 문자열 컬럼으로 저장한다.

- `preferred_colors_json`
- `preferred_materials_json`
- `style_tags_json`

API 계약에서는 `preferredColors`, `preferredMaterials`, `styleTags` 배열로 주고받는다. 신규 사용자의 기본값은 모두 빈 배열이다.

추천 점수는 기존 100점 체계를 유지하되 기존 다양성 점수 10점을 `preferenceScore` 10점으로 교체한다.

- 선호 색상/소재가 모두 비어 있으면 `preferenceScore=0`
- 후보 옷 중 선호 색상이 하나 이상 있으면 5점
- 후보 옷 중 선호 소재가 하나 이상 있으면 5점
- `styleTags`는 MVP-3 당시 저장/조회/표시 전용이었다. MVP6 이후 추천 점수와 추천 이유 반영 기준은 ADR-011을 따른다.

추천 이력 조회 API는 `GET /api/recommendations?limit={limit}`로 둔다.

- 기본값 `20`
- 최소 `1`
- 최대 `50`
- 최신순 정렬
- invalid limit은 `400 INVALID_REQUEST`

MVP-3 전환 시 로컬 Docker Compose DB는 기존 2차 schema/seed data와 충돌할 수 있으므로 `docker compose down -v` 후 재실행을 권장한다.

## Consequences
- 프론트는 로그인 전/후 화면과 token lifecycle을 관리해야 한다.
- 기존 `userId=1` demo 흐름은 데모 계정 또는 회원가입 기반 흐름으로 대체된다.
- 보호 API 테스트는 인증 token 발급과 header 설정을 포함해야 한다.
- 현재 사용자 전용 DTO에서 `userId`가 사라지므로 프론트 타입과 API 문서를 함께 갱신해야 한다.
- 추천 점수 DTO와 DB snapshot 컬럼은 `preferenceScore`를 사용해야 한다.
- 기존 다양성 점수 기반 테스트와 문서는 갱신해야 한다.
- `styleTags`를 저장하더라도 MVP-3 추천 결과에는 영향을 주지 않았다. MVP6 이후 기준은 ADR-011을 따른다.
- 선호도 정규화가 필요해지면 4차 이후 별도 테이블 도입을 검토한다.

## Out of Scope
- Refresh token
- Social login
- Email verification
- Password reset
- Admin role workflow
- External address/map API
- Browser geolocation
- Preference normalization tables
- styleTags scoring
- AI/GPT recommendation
- Image upload
- Redis
- AWS deployment

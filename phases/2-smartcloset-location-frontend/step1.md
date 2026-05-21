# 단계 1: location-api

범위: Must-have / 2차 P0

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/API.md`
- `docs/ARCHITECTURE.md`
- `docs/ERD.md`
- `docs/COMMANDS.md`
- `phases/2-smartcloset-location-frontend/step0.md`
- step 0에서 변경된 `src/main/java/com/smartcloset/user/**`
- step 0에서 변경된 `src/main/java/com/smartcloset/location/**`
- `src/main/java/com/smartcloset/common/**`

이전 단계에서 만들어진 코드를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업
내장 위치 catalog 조회 API와 사용자 위치 조회/선택 API를 구현한다. API는 기존 프로젝트의 controller, service, DTO, error response 패턴을 따른다.

## 변경 예상 파일
- `src/main/java/com/smartcloset/location/presentation/**`
- `src/main/java/com/smartcloset/location/application/**`
- `src/main/java/com/smartcloset/location/dto/**`
- `src/main/java/com/smartcloset/user/presentation/**`
- `src/main/java/com/smartcloset/user/application/**`
- `src/main/java/com/smartcloset/user/dto/**`
- `src/main/java/com/smartcloset/common/exception/**`
- `src/test/java/com/smartcloset/location/**`
- `src/test/java/com/smartcloset/user/**`

## 구현 메모
- 추가 API:
  - `GET /api/locations?keyword={keyword}`
  - `GET /api/users/location?userId={userId}`
  - `PUT /api/users/location?userId={userId}`
- DTO:
  - `LocationOptionResponse`: `code`, `name`, `nx`, `ny`
  - `UserLocationResponse`: `userId`, `code`, `name`, `nx`, `ny`, `updatedAt`
  - `UpdateUserLocationRequest`: `locationCode`
- 성공 응답은 기존 공통 wrapper인 `{ "data": ... }` 형태를 따른다.
- 실패 응답은 `{ "code", "message", "details" }` 형태를 따른다.
- `details` 원소는 `{ "field": "...", "message": "..." }` 형태다.
- 존재하지 않는 `locationCode`는 `LOCATION_NOT_FOUND`, HTTP `404`로 응답한다.
- 기존 사용자 row에 위치가 없으면 위치 조회 API에서 서울 기본값으로 backfill하고 응답한다.
- 위치 조회 API에서 backfill 저장이 필요한 경우에는 read-only transaction을 쓰지 않는다. 위치가 이미 있는 단순 조회는 read-only transaction을 사용할 수 있다.
- `userId`는 기존 API와 동일하게 request parameter로 받는다.

## 검증 절차
```bash
git diff --check
! rg -n 'GET /api/recommendations/(today)' . --glob '!archive/**'
./gradlew test
```

## 인수 기준
- `GET /api/locations`가 9개 대표 catalog를 반환한다.
- `GET /api/locations?keyword=서울`이 `SEOUL` 항목을 반환한다.
- `GET /api/locations?keyword=SEO`가 `SEOUL` 항목을 반환한다.
- `GET /api/users/location?userId=1`이 서울 기본 위치를 반환한다.
- 위치가 비어 있는 기존 사용자 row는 위치 조회 전에 서울 기본값으로 보정된다.
- 위치 조회에서 backfill이 발생한 경우 보정된 위치가 DB에 저장된다.
- `PUT /api/users/location?userId=1`에 `{ "locationCode": "BUSAN" }`을 보내면 부산 위치가 저장되고 응답된다.
- 알 수 없는 `locationCode`는 `LOCATION_NOT_FOUND`와 `details[0].field = "locationCode"`로 실패한다.
- `USER_NOT_FOUND` 등 기존 오류 응답 shape가 깨지지 않는다.

## 금지사항
- 로그인, 세션, Spring Security를 추가하지 마라. 이유: 2차도 seed/test user 기반이다.
- 위치 catalog를 DB에 저장하지 마라. 이유: 2차는 서버 내장 대표 격자 catalog로 결정됐다.
- 추천 생성 API 계약을 바꾸지 마라. 이유: `POST /api/recommendations?userId={userId}`는 유지 계약이다.
- today 추천 GET 경로를 추가하지 마라. 이유: 문서와 skill에서 금지한 API 계약이다.
- 프론트엔드 파일을 이 단계에서 만들지 마라. 이유: 프론트 스캐폴드와 Compose 추가는 별도 step에서 함께 처리한다.

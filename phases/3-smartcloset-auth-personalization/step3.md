# 단계 3: location-current-user-api

범위: Must-have / 3차 P0

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/API.md`
- `docs/ARCHITECTURE.md`
- `docs/ERD.md`
- `docs/COMMANDS.md`
- `docs/adr/007-mvp2-user-location-and-react-frontend.md`
- `docs/adr/008-mvp3-authenticated-user-personalization.md`
- `src/main/java/com/smartcloset/location/**`
- `src/main/java/com/smartcloset/user/**`
- `src/test/java/com/smartcloset/location/**`

이전 단계에서 만들어진 코드를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업
위치 catalog와 현재 사용자 위치 API를 인증 사용자 기준으로 전환한다. `GET /api/locations`는 보호 API로 만들고, 기존 `userId` query 기반 위치 API를 `GET/PUT /api/users/me/location` 계약으로 바꾼다.

이 단계에서 보호 API로 추가 잠그는 범위는 위치 catalog와 현재 사용자 위치 API다. 추천 API는 Step 6 전환 전까지 임시 허용을 유지한다.

## 변경 예상 파일
- `src/main/java/com/smartcloset/location/presentation/**`
- `src/main/java/com/smartcloset/location/application/**`
- `src/main/java/com/smartcloset/location/dto/**`
- `src/main/java/com/smartcloset/user/domain/User.java`
- `src/test/java/com/smartcloset/location/**`

## 구현 메모
- 대상 API:
  - `GET /api/locations?keyword={keyword}`
  - `GET /api/users/me/location`
  - `PUT /api/users/me/location`
- `GET /api/locations`는 보호 API다. token 없이 호출하면 401이다.
- 회원가입 흐름은 위치 catalog를 호출하지 않는다. 신규 사용자 기본 위치는 서버에서 `SEOUL`로 설정한다.
- 위치 catalog는 DB 테이블이 아니라 서버 내장 대표 격자 catalog를 유지한다.
- 최소 catalog는 `SEOUL`, `BUSAN`, `DAEGU`, `INCHEON`, `GWANGJU`, `DAEJEON`, `ULSAN`, `SEJONG`, `JEJU`를 포함한다.
- 현재 사용자 위치 조회 시 위치 snapshot이 없으면 서울 기본값으로 backfill한다. 이 경우 write transaction이 필요하다.
- 존재하지 않는 `locationCode`는 `LOCATION_NOT_FOUND`로 실패한다.
- 추천 생성에서 현재 사용자 위치 `nx`, `ny`를 KMA 요청에 연결하는 작업은 `recommendation-current-user-api` step에서 다룬다.
- `SecurityConfig`는 `GET /api/locations`, `GET /api/users/me/location`, `PUT /api/users/me/location`을 Bearer token 필수로 만들고, 아직 전환하지 않은 API의 임시 허용은 Step 7 제거 대상으로 유지한다.

## 검증 절차
```bash
git diff --check
! rg -n -F -e '/api/users/location?userId' src/main/java/com/smartcloset/user src/test/java/com/smartcloset/user
! rg -n -e 'GET /api/users/location' -e 'PUT /api/users/location' -e '@RequestParam.*userId' -e 'RequestParam Long userId' -e '\\.param\\("userId"' src/main/java/com/smartcloset/user src/test/java/com/smartcloset/user
! rg -n '/api/recommendations.*401' src/test/java
./gradlew test
```

## 인수 기준
- token 없이 `GET /api/locations`를 호출하면 401로 실패한다.
- 유효 token으로 위치 catalog 조회와 검색이 동작한다.
- `GET /api/users/me/location`은 현재 인증 사용자의 위치를 반환하며 응답에 `userId`가 없다.
- 위치가 비어 있는 기존 사용자는 서울 기본값으로 backfill된다.
- `PUT /api/users/me/location`은 현재 인증 사용자 위치만 변경한다.
- 잘못된 location code는 `LOCATION_NOT_FOUND`로 실패한다.
- 위치 API 전환 이후에도 내장 catalog의 9개 대표 지역 조회와 검색이 유지된다.
- 아직 전환하지 않은 추천 API를 Step 3에서 새로 401 회귀 테스트 대상으로 만들지 않는다.

## 금지사항
- `GET /api/locations`를 공개 API로 열지 마라. 이유: 3차에서는 로그인 후 위치 선택 흐름에서만 사용한다.
- 외부 주소/지도 API를 추가하지 마라. 이유: 3차 제외 범위다.
- 브라우저 geolocation 또는 위경도-KMA 변환 API를 추가하지 마라. 이유: 3차 제외 범위다.
- 위치 catalog를 DB 테이블로 만들지 마라. 이유: 서버 내장 catalog로 결정됐다.
- 위치 응답에 `userId`를 넣지 마라. 이유: 현재 사용자 전용 response DTO에서는 `userId`를 제거한다.
- KMA provider 요청 파라미터 변경을 이 단계에 섞지 마라. 이유: 위치 API 전환과 추천/weather 연동을 분리해 리뷰 가능하게 유지한다.
- `/api/**` 전체 인증 정책을 이 단계에 적용하지 마라. 이유: 추천 API 전환이 아직 남아 있다.

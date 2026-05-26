# PRD: SmartCloset MVP7 위치/날씨 신뢰도

## 문서 목적

이 문서는 SmartCloset MVP7의 확정 범위를 정의한다. MVP7은 MVP6 추천 피드백/개인화 완료 baseline 위에 KMA 격자 기반 위치 정밀화, 브라우저 현재 위치 후보 찾기, 예보 시간대 선택, 추천 결과의 위치/날씨 source snapshot 저장을 추가한다.

현재 코드 출발점은 MVP6 구현 완료 상태다. MVP7 구현 기준은 이 문서와 `docs/` 아래 현재 문서, ADR-012다.

## 문서 책임

| 계약 영역 | Source of truth |
| --- | --- |
| HTTP endpoint, request/response DTO, 인증/에러 계약 | `docs/API.md` |
| 추천 후보, 점수, 추천 이유, 예보 시간대 입력 | `docs/RECOMMENDATION_RULES.md` |
| 백엔드 구조, transaction, location/weather provider 정책 | `docs/ARCHITECTURE.md` |
| DB schema와 JPA/entity 기준 | `docs/ERD.md` |
| 프론트 API client, 타입, UX, 반응형 기준 | `docs/FRONTEND.md` |
| 데모/공유 검증 | `docs/DEMO_SCENARIO.md`, `docs/SHARING_GUIDE.md` |
| 결정 배경 | `docs/ADR.md`, `docs/adr/012-mvp7-location-weather-trust.md` |

## MVP7 한 줄 정의

SmartCloset이 추천에 사용한 동네 위치, KMA/fallback 여부, 예보 기준 시각을 저장하고 표시해 날씨 기반 추천의 신뢰도를 높인다.

## 목표

- 대표 도시 중심 위치 catalog를 읍/면/동 단위 KMA 행정구역 catalog로 확장한다.
- 사용자가 지역명을 검색하거나 브라우저 현재 위치로 가까운 후보를 찾아 위치를 저장할 수 있게 한다.
- 추천 생성 시 현재/오전/오후/저녁 예보 기준을 선택할 수 있게 한다.
- 추천 결과와 이력에서 사용된 위치와 날씨 source를 확인하게 한다.
- KMA 사용 여부, fallback 여부, KMA base date/time, forecast date/time을 표시한다.

## 현재 Baseline

- 공개 API는 `POST /api/auth/signup`, `POST /api/auth/login`뿐이다.
- 그 외 API는 `Authorization: Bearer {accessToken}` header를 요구한다.
- 공개 HTTP API는 `userId` query parameter를 받지 않는다.
- 현재 사용자 전용 응답 DTO는 `userId`를 노출하지 않는다.
- 사용자 소유 옷장, 위치, 선호도, 추천 이력, 착용 이력, 추천 피드백은 인증 사용자별로 분리한다.
- 추천 생성 API는 `POST /api/recommendations`다.
- 추천 이력 조회 API는 `GET /api/recommendations?limit={limit}`이며 기본 20, 최소 1, 최대 50, 최신순이다.
- 현재 날씨 요약 API는 `GET /api/weather/current`이며 보호 API다.
- MVP6의 추천 상황, 옷별 `styleTags`, 추천 피드백 snapshot, 최근 피드백 기반 `preferenceScore`는 유지한다.
- 프론트 access token 저장 위치는 `sessionStorage`다.
- 옷 이미지 업로드/교체/조회/삭제, 추천/이력 썸네일, Docker Compose image volume은 MVP5 계약을 유지한다.

## 해결하려는 문제

- 대표 도시 중심 catalog만으로는 사용자가 실제 생활권의 날씨 기준을 고르기 어렵다.
- 같은 추천 화면 안에서 날씨 요약과 추천 snapshot의 출처가 불분명하면 사용자가 추천을 신뢰하기 어렵다.
- KMA 실패로 fallback 날씨가 쓰였는지 알 수 없다.
- 추천 이력에서 과거 추천이 어떤 위치와 예보 시각을 기준으로 만들어졌는지 확인할 수 없다.
- 오전/오후/저녁 외출처럼 오늘 안의 시간대가 다른 상황을 현재 시각 날씨 하나로만 처리한다.

## 핵심 사용자 시나리오

1. 사용자가 Location view에서 `일산동`을 검색한다.
2. 앱은 KMA 행정구역 catalog 후보를 여러 개 보여준다.
3. 사용자가 후보를 직접 선택하거나 브라우저 현재 위치 권한을 허용해 가까운 후보를 찾는다.
4. 사용자는 후보를 확인한 뒤 내 위치로 저장한다.
5. Today view에서 출근/캐주얼 같은 상황과 오전/오후/저녁 예보 기준을 선택한다.
6. 추천 결과에는 옷 조합, 이유, 점수와 함께 사용 위치, KMA/fallback 여부, 예보 기준 시각이 표시된다.
7. History view에서 과거 추천의 위치/날씨 source snapshot을 다시 확인한다.

## MVP7 우선순위

### P0: KMA 위치 catalog 확장

- KMA 단기예보 격자 위경도 엑셀을 프로젝트 내부 catalog로 가져온다.
- 검색 단위는 행정구역 `1단계`, `2단계`, `3단계`다.
- `GET /api/locations?keyword={keyword}`는 읍/면/동 후보를 반환한다.
- 동명이인은 여러 후보로 반환하고 프론트에서 선택하게 한다.
- `LocationOptionResponse`는 code, name, fullName, region1, region2, region3, nx, ny, latitude, longitude를 포함한다.

### P0: 브라우저 현재 위치 후보 찾기

- 프론트는 브라우저 Geolocation API로 좌표를 얻는다.
- 서버는 `POST /api/locations/resolve`로 좌표를 받아 KMA grid와 가까운 catalog 후보를 반환한다.
- 브라우저 좌표 원문은 DB에 저장하지 않는다.
- 사용자가 후보를 확인하고 선택해야 사용자 위치가 저장된다.
- 위치 저장 source는 `MANUAL_SEARCH` 또는 `BROWSER_GEOLOCATION`이다.

### P0: 예보 시간대 선택

- 추천 생성 request는 optional `forecastPeriod`를 받는다.
- enum은 `CURRENT`, `MORNING`, `AFTERNOON`, `EVENING`이다.
- 누락된 `forecastPeriod`는 `CURRENT`다.
- KMA provider는 요청한 시간대에 맞는 forecast target time을 선택한다.
- 실제 사용한 forecast date/time은 응답과 추천 snapshot에 남긴다.

### P0: 위치/날씨 source snapshot

- `WeatherResponse`는 기존 날씨 값에 location snapshot과 source metadata를 포함한다.
- 추천 결과 row에는 추천 생성 당시 위치와 weather source를 snapshot으로 저장한다.
- 저장 대상은 사람이 신뢰 판단에 쓰는 필드만 포함한다.
- raw KMA 응답 JSON은 저장하지 않는다.
- 사용자 위치가 나중에 바뀌어도 과거 추천 이력의 snapshot은 바뀌지 않는다.

### P0: Trust UX

- Today view와 History view에 KMA 사용 여부, fallback 여부, 예보 기준 시각을 표시한다.
- Location view는 동명이인 후보를 구분 가능한 fullName과 grid 정보로 보여준다.
- 브라우저 위치 권한 거부 시 수동 검색으로 자연스럽게 돌아간다.
- 모바일 375px에서 위치 후보, source 표시, 시간대 선택 control이 겹치지 않는다.

## 포함 범위

- KMA 행정구역 catalog data/resource
- `POST /api/locations/resolve`
- `LocationSource` enum: `MANUAL_SEARCH`, `BROWSER_GEOLOCATION`
- `ForecastPeriod` enum: `CURRENT`, `MORNING`, `AFTERNOON`, `EVENING`
- `LocationOptionResponse` 확장
- `UpdateUserLocationRequest.source`
- `RecommendationRequest.forecastPeriod`
- `WeatherResponse.location`
- `WeatherResponse.source`
- `RecommendationResponse.weather.location`
- `RecommendationResponse.weather.source`
- `recommendation_results` 위치/날씨 source snapshot 컬럼
- 현재 weather 조회와 추천 생성에 사용되는 weather source metadata
- 프론트 위치 검색/현재 위치 후보/시간대 선택/source 표시 UX
- MVP7 phase 문서와 docs-check 규칙

## 제외 범위

- 외부 주소/지도 검색 API
- 도로명/건물명 full address geocoding
- GPS 좌표 원문 DB 저장
- raw KMA 응답 JSON 저장
- KMA `getVilageFcst` 외 weather API
- 기상청 초단기실황/초단기예보 API
- AI/GPT 추천
- AI 자동 태깅
- 피드백 이벤트 로그 테이블과 analytics
- 쇼핑 추천
- refresh token
- social login
- email verification
- password reset
- Redis
- AWS 배포와 CD 자동화
- S3/CDN 이미지 hosting
- 다중 이미지 업로드

## API 변경 기준

- 새 공개 API는 추가하지 않는다.
- 모든 신규/변경 API는 보호 API다.
- `GET /api/locations`는 내부 KMA catalog 검색이며 외부 주소 검색 proxy로 사용하지 않는다.
- 브라우저 좌표는 `POST /api/locations/resolve` 요청에만 사용하고 저장하지 않는다.
- `PUT /api/users/me/location`은 기존 `locationCode`를 유지해 기존 호출을 깨뜨리지 않는다.
- `POST /api/recommendations`는 body 없이도 기존 호출이 성공해야 한다.
- 추천 `situation` 누락은 `CASUAL`, `forecastPeriod` 누락은 `CURRENT`다.
- 현재 사용자 전용 DTO에 `userId`를 되살리지 않고 공개 `userId` query parameter를 추가하지 않는다.

## 데이터/ERD 기준

- 사용자 row에는 선택된 catalog 위치만 저장한다.
- 사용자 위치 저장 source는 `users.location_source`에 저장한다.
- 추천 결과 row에는 위치/날씨 source snapshot을 저장한다.
- 추천 snapshot은 raw KMA JSON이나 브라우저 GPS 원문 좌표를 포함하지 않는다.
- 운영 DB migration 전략은 기존과 같이 Hibernate `ddl-auto=update`와 로컬 Docker Compose reset 기준으로 검증한다.

## 프론트엔드 기준

- Location view에는 동네 검색과 현재 위치로 찾기 control을 둔다.
- 브라우저 위치 권한 요청은 사용자 버튼 클릭 뒤에만 수행한다.
- 좌표 resolve 결과는 후보 선택 UI로 보여주고 자동 저장하지 않는다.
- Today view에는 `CURRENT`, `MORNING`, `AFTERNOON`, `EVENING` 선택 control을 둔다.
- 추천 결과와 History card에는 위치/날씨 source snapshot을 표시한다.
- 큰 state-management library를 추가하지 않는다.

## 추천 규칙 기준

상세 추천 계약은 `docs/RECOMMENDATION_RULES.md`를 따른다.

- 점수 총합과 세부 점수 최대값은 MVP6와 동일하다.
- `forecastPeriod`는 weather input 선택에만 관여한다.
- 위치/source snapshot은 점수 항목을 새로 만들지 않는다.
- `WeatherCondition`과 source metadata는 분리해 추천 도메인이 외부 API 원본에 직접 의존하지 않게 한다.
- 이미지 존재 여부는 추천 점수, 후보 필터링, 추천 이유에 영향을 주지 않는다.

## 완료 기준

- `일산동` 같은 동네 검색이 복수 후보를 반환한다.
- 브라우저 좌표 resolve API가 KMA grid와 가까운 위치 후보를 반환한다.
- 잘못된 좌표는 `400 INVALID_REQUEST`로 실패한다.
- 위치 저장 시 `MANUAL_SEARCH` 또는 `BROWSER_GEOLOCATION` source가 저장된다.
- `GET /api/weather/current`가 location/source metadata를 반환한다.
- `POST /api/recommendations`가 `forecastPeriod`를 받아 추천을 생성한다.
- 추천 결과와 추천 이력에 위치/source snapshot이 포함된다.
- KMA 사용 여부, fallback 여부, base date/time, forecast date/time을 확인할 수 있다.
- 사용자 위치가 바뀌어도 과거 추천 snapshot은 바뀌지 않는다.
- MVP6 피드백/개인화와 MVP5 이미지 흐름은 유지된다.

## 테스트/검증 기준

- `git diff --check`
- `./gradlew test`
- `./gradlew build`
- `cd frontend && npm run build`
- `docker compose config --quiet`
- `python3 scripts/checks.py --docs-check-config phases/7-smartcloset-location-weather-trust/docs-checks.json --docs-check`

## 결정 완료 사항

- 위치 검색: 내부 KMA 행정구역 catalog를 사용하고 외부 지도 API는 사용하지 않는다
- 현재 위치: 브라우저 Geolocation 좌표를 서버 resolve에만 사용하고 후보 선택 후 저장
- 예보 선택: 추천 요청의 `forecastPeriod`
- 기본 예보 선택: `CURRENT`
- source snapshot: 사람이 신뢰 판단에 쓰는 필드만 저장
- raw KMA JSON: 저장하지 않음

# PRD: SmartCloset 2차 MVP

## 한 줄 정의
SmartCloset 2차 MVP는 기상청 단기예보 기반 규칙 추천 백엔드에 사용자별 위치 저장과 React+Vite+TypeScript 프론트엔드 앱을 추가해, 사용자가 자신의 지역 날씨 기준으로 옷장을 관리하고 코디를 추천받는 서비스다.

## 배경
1차 MVP는 고정 날씨로 추천 도메인과 API 계약을 검증했다. 1.5차 MVP는 기상청 단기예보 조회서비스 `getVilageFcst` JSON 연동과 fallback 정책을 추가해 실제 날씨 기반 추천을 가능하게 했다.

2차 MVP는 위치가 앱 전역 환경변수로 고정되어 있던 한계를 줄인다. 사용자는 내장 대표 격자 catalog에서 자신의 위치를 선택하고, 추천 생성은 사용자별 위치의 `nx`, `ny`를 사용해 KMA 예보를 조회한다. 또한 Swagger/Demo UI 중심 확인 흐름을 React+Vite+TypeScript 앱으로 전환한다.

## 해결하려는 문제
- 사용자마다 사는 지역이 다르지만 1.5차까지는 모든 사용자가 같은 KMA 격자를 사용했다.
- API는 동작하지만 실제 사용 흐름은 Swagger 또는 Spring static Demo UI에 의존해 제품 사용감을 확인하기 어려웠다.
- 프론트엔드 계약과 백엔드 API 계약이 분리되어 있지 않아 2차 이후 화면 개발 기준이 모호했다.

## 핵심 사용자 시나리오
1. 사용자는 React 앱에 접속해 seed user 또는 테스트용 `userId=1`로 서비스를 사용한다.
2. 사용자는 현재 저장된 위치를 확인한다. seed user의 기본 위치는 서울특별시 `nx=60`, `ny=127`이다.
3. 사용자는 내장 대표 격자 catalog에서 지역을 검색하고 선택한다.
4. 서비스는 선택한 위치를 사용자별 위치로 저장한다.
5. 사용자는 옷을 등록하고 목록을 확인한다.
6. 추천 생성 시 서비스는 사용자 위치의 `nx`, `ny`로 기상청 `getVilageFcst` JSON을 조회한다.
7. 서비스키 미설정, 외부 API 실패, `NODATA`, 필수 category 누락 시 fallback 날씨로 추천 흐름을 유지한다.
8. 서비스는 날씨상 입기 어려운 옷을 제외하고 TOP/BOTTOM 또는 TOP/BOTTOM/OUTER 조합을 만든다.
9. 사용자는 추천 결과에서 현재 위치, weather snapshot, 총점, 세부 점수, 추천 이유를 확인한다.
10. 사용자가 추천 결과를 착용 완료 처리하면 이후 추천에 이력이 반영된다.
11. 공유 대상자는 Docker Compose로 백엔드, MySQL, 프론트엔드 앱을 실행해 같은 흐름을 확인한다.

## 2차 MVP 우선순위

### P0: 사용자 위치와 정식 프론트 흐름
- 사용자별 위치 저장
- 내장 KMA 대표 격자 catalog 조회와 검색
- 위치 선택 API
- 추천 생성 시 사용자별 `nx`, `ny` 사용
- seed user 기본 위치를 서울특별시로 설정
- React+Vite+TypeScript SPA 제공
- 프론트에서 위치 선택, 옷 목록/등록, 추천 생성, 착용 완료 처리 제공
- Docker Compose 공유 흐름을 프론트 포함 기준으로 갱신
- README, API, ERD, 아키텍처, 데모, 공유 문서 동기화

### P1: 프론트 사용성 보강
- 옷 상세/수정/보관 화면
- 추천 실패 코드별 안내 상태
- 로딩, 빈 목록, API 실패 상태 정리
- 프론트 build/type check를 GitHub Actions에 포함
- 프론트와 백엔드 API base URL 환경변수 정리

### P2: 2차 이후 후보
- 외부 주소/지도 검색 API
- 사용자 위치 좌표 변환
- weather source, `nx`, `ny` snapshot DB 저장
- 최근 성공 날씨 캐싱
- 옷 이미지 업로드
- 선호도/스타일 태그 기반 개인화
- 로그인/회원가입

## 2차 포함 범위
- Java 21 기반 Spring Boot 4.0.6 백엔드 유지
- 기존 옷 관리 API 유지
- 기존 추천 생성/착용 완료 API 유지
- 기존 추천 점수 체계와 실패 코드 유지
- 기상청 단기예보 `getVilageFcst` JSON 연동 유지
- `StaticWeatherProvider` fallback 유지
- 사용자별 위치 컬럼 추가
- 내장 대표 격자 catalog 기반 위치 조회/선택 API
- React+Vite+TypeScript SPA
- typed API client와 명시적 DTO 타입
- Docker Compose 공유 방식 유지
- 1.5차 결과의 최소 archive 정리

## 2차 제외 범위
- 로그인/회원가입
- Spring Security
- 외부 주소/지도 검색 API
- 사용자 현재 위치 자동 감지
- 위경도-KMA 격자 변환 API
- Weather source DB 저장
- 날씨 응답 Redis 캐싱
- 최근 성공 날씨 DB 캐시
- AI/GPT 추천
- 옷 이미지 업로드
- 이미지 자동 분석/태깅
- 캘린더 연동
- 쇼핑몰 추천
- 관리자 기능
- AWS 배포
- CD 자동화

## 위치 정책
2차 위치 선택은 외부 위치 API 없이 서버 내장 대표 격자 catalog를 사용한다.

최소 catalog는 아래 지역을 포함한다. 실제 구현 시 `nx`, `ny`는 기상청 격자 위경도 공식 자료 기준으로 고정한다.

| Code | Name | nx | ny |
| --- | --- | ---: | ---: |
| `SEOUL` | 서울특별시 | 60 | 127 |
| `BUSAN` | 부산광역시 | 98 | 76 |
| `DAEGU` | 대구광역시 | 89 | 90 |
| `INCHEON` | 인천광역시 | 55 | 124 |
| `GWANGJU` | 광주광역시 | 58 | 74 |
| `DAEJEON` | 대전광역시 | 67 | 100 |
| `ULSAN` | 울산광역시 | 102 | 84 |
| `SEJONG` | 세종특별자치시 | 66 | 103 |
| `JEJU` | 제주특별자치도 | 52 | 38 |

seed user는 기본 위치로 `SEOUL`을 가진다. 위치가 없는 기존 사용자 데이터는 위치 조회 또는 추천 생성 전에 애플리케이션에서 서울 기본값으로 backfill한다. 후속 migration 도구 도입 전까지 위치 컬럼에 DB non-null 제약을 강제하지 않는다.

## Weather 정책
추천 로직은 외부 API 응답 모델에 직접 의존하지 않는다. 모든 추천 도메인 로직은 내부 `WeatherCondition`만 사용한다.

`WeatherProvider` 인터페이스는 유지한다. 2차에서 `WeatherProvider#getCurrentWeather(Long userId)`는 `userId`로 사용자 위치를 조회하고, 해당 위치의 `locationNx`, `locationNy`를 KMA 요청에 사용한다. 2차 추천 경로의 KMA grid source of truth는 `KMA_NX`, `KMA_NY` 환경변수가 아니라 사용자 위치다.

- 기본 구현: `KmaVilageForecastWeatherProvider`
- fallback/test 구현: `StaticWeatherProvider`
- 외부 API endpoint: `GET {KMA_BASE_URL}/getVilageFcst`
- KMA category: `TMP`, `SKY`, `PTY`, `PCP`, `WSD`

`WEATHER_FALLBACK_ENABLED`의 기본값은 `true`다. `true`이면 서비스키 미설정, KMA 호출 실패, `resultCode != 00`, `NODATA`, 필수 category 누락, 파싱 실패 시 fallback 값을 사용한다. `false`이면 strict KMA mode로 동작하며 같은 상황에서 fallback하지 않고 `INTERNAL_SERVER_ERROR`로 실패한다.

fallback 값은 유지한다.

| Field | Value |
| --- | --- |
| `temperature` | `12` |
| `weatherType` | `CLOUDY` |
| `rainy` | `false` |
| `windy` | `false` |

## 사용자 모델
2차에서도 회원가입과 로그인을 구현하지 않는다. API는 테스트용 `userId`를 request parameter로 전달받는다.

`users`는 위치 선택을 위해 아래 값을 가진다.

- `locationCode`
- `locationName`
- `locationNx`
- `locationNy`

위치 변경은 인증 없는 테스트 사용자 기준으로만 제공한다.

## 주요 API
- `POST /api/clothes?userId={userId}`: 옷 등록
- `GET /api/clothes?userId={userId}`: 옷 목록 조회
- `GET /api/clothes/{clothingId}?userId={userId}`: 옷 상세 조회
- `PUT /api/clothes/{clothingId}?userId={userId}`: 옷 수정
- `PATCH /api/clothes/{clothingId}/archive?userId={userId}`: 옷 보관 처리
- `GET /api/locations?keyword={keyword}`: 내장 위치 catalog 조회
- `GET /api/users/location?userId={userId}`: 사용자 위치 조회
- `PUT /api/users/location?userId={userId}`: 사용자 위치 선택
- `POST /api/recommendations?userId={userId}`: 추천 생성
- `PATCH /api/recommendations/{recommendationId}/worn?userId={userId}`: 추천 결과 착용 완료 처리

today 추천 GET 경로는 API 계약으로 사용하지 않는다.

## 프론트엔드 정책
2차 프론트엔드는 `frontend/` 아래 React+Vite+TypeScript SPA로 둔다.

`frontend/` 스캐폴드와 Docker Compose `frontend` 서비스는 함께 유지한다.

- TypeScript `strict` 기준을 사용한다.
- API 요청/응답 DTO는 명시적 타입으로 관리한다.
- 대형 상태 관리 라이브러리 없이 React state와 작은 API client로 구현한다.
- 프론트 상세 기준은 `docs/FRONTEND.md`를 따른다.

## 완료 기준
- 1.5차 KMA 연동 결과가 `archive/mvp-1-5/`에 최소 요약으로 정리된다.
- seed user 기본 위치가 서울특별시로 문서화된다.
- 위치 catalog 조회와 사용자 위치 선택 API 계약이 문서화된다.
- 추천 생성은 사용자별 위치의 `nx`, `ny`를 사용한다.
- 추천 생성 API 계약은 `POST /api/recommendations?userId={userId}`로 유지된다.
- 추천 점수 100점 배점과 추천 실패 코드 5종이 변경되지 않는다.
- React+Vite+TypeScript 앱의 화면, 타입, API client, 검증 기준이 문서화된다.
- README만 보고 백엔드와 프론트엔드를 함께 실행하고 데모 흐름을 이해할 수 있다.

## 테스트/검증 기준
- `GET /api/locations`가 내장 catalog를 반환한다.
- `GET /api/locations?keyword=서울`이 서울 항목을 반환한다.
- `GET /api/users/location?userId=1`이 seed user 기본 위치를 반환한다.
- `PUT /api/users/location?userId=1`이 유효한 `locationCode`를 저장한다.
- 잘못된 `locationCode`는 `LOCATION_NOT_FOUND`로 실패한다.
- 위치 변경 후 추천 생성이 사용자 위치 `nx`, `ny`로 KMA를 호출한다.
- 서비스키 미설정 상태에서도 fallback 추천이 성공한다.
- TypeScript type check와 프론트 build가 통과한다.
- Docker Compose로 백엔드, MySQL, 프론트엔드를 실행할 수 있다.

## 결정된 사항
- Spring Boot 버전은 `4.0.6`으로 고정한다.
- 추천 생성 API는 `POST /api/recommendations?userId={userId}`를 유지한다.
- 외부 Weather API는 기상청 단기예보 `getVilageFcst` JSON만 사용한다.
- 위치 선택은 외부 지도/주소 API 없이 서버 내장 대표 격자 catalog로 구현한다.
- 프론트엔드는 React+Vite+TypeScript SPA로 구현한다.
- Docker Compose 공유 방식을 유지한다.

# 아키텍처: SmartCloset MVP4

## 전체 아키텍처 개요
SmartCloset MVP4는 Spring Boot 4.0.6 백엔드와 React+Vite+TypeScript 프론트엔드 앱으로 구성한다. 백엔드는 MVP-3 인증 사용자 baseline, 추천 도메인, KMA weather provider, JWT Bearer 인증, 사용자 선호도, 추천 이력 조회를 유지한다.

MVP4의 아키텍처 변경 지점은 프론트엔드 앱 셸과 화면 구성이다. Today 화면의 현재 날씨 요약을 위해 보호 API `GET /api/weather/current`를 추가하지만, 새 외부 provider, persistence 변경, 인증 구조 변경은 MVP4 범위가 아니다.

전체 백엔드 요청 흐름은 아래 계층 구조를 유지한다.

```text
Controller -> Application Service -> Domain Service -> Repository / Provider
```

보호 API는 Spring Security filter chain에서 JWT를 검증하고 인증 principal을 만든다. Controller는 principal에서 현재 사용자 id를 얻어 application service에 전달한다. HTTP 계약과 프론트 타입에는 `userId` query parameter를 노출하지 않는다.

프론트엔드는 `frontend/` 아래 Vite React TypeScript SPA로 두고, 로그인 후 access token을 `sessionStorage`에 저장한다. 보호 API 호출 시 `Authorization: Bearer {accessToken}` header를 붙인다. MVP4에서는 로그인 후 기본 화면을 Today view로 두고, 현재 위치와 `GET /api/weather/current` 결과를 함께 보여준다.

## 권장 패키지 구조

```text
com.smartcloset
├── SmartClosetApplication
├── auth
│   ├── application
│   ├── domain
│   ├── dto
│   ├── infrastructure
│   └── presentation
├── common
│   ├── exception
│   ├── response
│   └── config
├── security
│   ├── JwtAuthenticationFilter
│   ├── JwtTokenProvider
│   ├── CurrentUserPrincipal
│   └── SecurityConfig
├── user
│   ├── domain
│   ├── repository
│   ├── application
│   ├── presentation
│   └── dto
├── location
│   ├── domain
│   ├── application
│   ├── presentation
│   └── dto
├── clothing
│   ├── domain
│   ├── repository
│   ├── application
│   ├── presentation
│   └── dto
├── weather
│   ├── domain
│   ├── application
│   ├── presentation
│   ├── dto
│   └── infrastructure
│       ├── kma
│       └── fallback
└── recommendation
    ├── domain
    ├── application
    ├── repository
    ├── presentation
    └── dto
```

프론트엔드:

```text
frontend
├── package.json
├── tsconfig.json
├── vite.config.ts
└── src
    ├── api
    ├── components
    ├── features
    │   ├── auth
    │   ├── clothes
    │   ├── location
    │   ├── preferences
    │   └── recommendation
    ├── types
    └── main.tsx
```

MVP4 view 구조:

```text
AppShell
├── DesktopSidebar
├── MobileTopBar
├── MobileBottomNav
└── active view
    ├── Today
    ├── Closet
    ├── Preferences
    ├── Location
    └── History
```

Spring Boot static Demo UI는 현재 주 제품 화면이 아니다. 유지하더라도 API smoke 확인용 보조 화면으로만 취급한다.

## 계층별 책임

### security
- 공개 API와 보호 API 분리
- JWT access token 검증
- 인증 principal 생성
- 401/403 처리
- 비밀번호 hash 검증은 인증 application service와 협력

### presentation/controller
- HTTP 요청/응답 처리
- request validation
- 인증 principal에서 현재 사용자 id 추출
- Application Service 호출
- DTO 변환
- 비즈니스 규칙 직접 처리 금지

### application/service
- 유스케이스 조합
- 트랜잭션 경계 관리
- Repository 호출
- `WeatherProvider` 호출
- Domain Service 호출
- 회원가입/로그인 orchestration
- 위치 catalog 조회와 사용자 위치 변경 orchestration
- 선호도 조회/저장 orchestration
- 추천 이력 조회 limit 검증

### domain
- Entity, Enum, Value Object
- 추천 후보 생성 규칙
- 점수 계산 규칙
- 추천 실패 판단
- 추천 이유 생성 규칙
- 위치 catalog 항목 value object
- 선호도 value object 또는 domain helper
- 가능한 한 순수 Java 로직으로 유지

### repository
- JPA 기반 데이터 접근
- Entity 저장/조회
- 소유자 조건을 포함한 조회
- 추천 점수 계산 로직 금지
- 후보 조합 생성 로직 금지
- KMA category 매핑 금지

## 인증 구조
현재 인증은 Spring Security + JWT Bearer access token 단일 구조다. refresh token은 MVP4 범위가 아니다.

```text
AuthController
  -> AuthService
      -> PasswordEncoder
      -> UserRepository
      -> JwtTokenProvider

ProtectedController
  <- JwtAuthenticationFilter
      <- JwtTokenProvider
      <- UserRepository
```

회원가입:

1. email 중복을 확인한다.
2. password를 BCrypt로 hash한다.
3. 기본 role `USER`를 설정한다.
4. 기본 위치 `SEOUL`을 설정한다.
5. 선호도 JSON 문자열 컬럼을 모두 `[]`로 설정한다.
6. 사용자를 저장한다.

로그인:

1. email로 사용자를 조회한다.
2. BCrypt password match를 확인한다.
3. `HS256`과 `JWT_SECRET`으로 access token을 발급한다.
4. token과 현재 사용자 정보를 반환한다.

프론트는 access token을 `sessionStorage`에 저장한다.

JWT access token payload 기준:

| Item | Value |
| --- | --- |
| `sub` | 현재 사용자 id 문자열 |
| `email` | 현재 사용자 email |
| `role` | `USER` |
| `iat` | 발급 시각 |
| `exp` | 발급 후 2시간 |

만료된 token, 서명이 잘못된 token, 지원하지 않는 token은 보호 API에서 `401`로 처리한다.

## API 인증 경계

공개 API:

- `POST /api/auth/signup`
- `POST /api/auth/login`

보호 API:

- `GET /api/users/me`
- `GET /api/locations?keyword={keyword}`
- `GET /api/users/me/location`
- `PUT /api/users/me/location`
- `GET /api/users/me/preferences`
- `PUT /api/users/me/preferences`
- `GET /api/weather/current`
- `GET /api/clothes`
- `POST /api/clothes`
- `GET /api/clothes/{clothingId}`
- `PUT /api/clothes/{clothingId}`
- `PATCH /api/clothes/{clothingId}/archive`
- `POST /api/recommendations`
- `GET /api/recommendations?limit={limit}`
- `PATCH /api/recommendations/{recommendationId}/worn`

`GET /api/locations`는 민감정보를 반환하지 않지만 MVP4에서도 보호 API로 고정한다. 회원가입 화면은 위치 catalog를 호출하지 않고, 로그인 후 위치 선택 화면에서 호출한다.

## userId 제거 구조
HTTP query parameter의 `userId`는 제거한다. Controller는 인증 principal에서 현재 사용자 id를 얻는다.

```text
JwtAuthenticationFilter
  -> CurrentUserPrincipal(userId, email, role)
      -> Controller method argument
          -> service.method(userId, ...)
```

현재 사용자 전용 response DTO에서도 `userId`를 제거한다. 내부 Entity, Repository, Service에서는 소유자 검증과 조회 조건을 위해 `Long userId`를 유지할 수 있다.

## Location 구조
현재 위치 선택은 외부 위치 API 없이 내장 대표 격자 catalog를 사용한다.

```text
LocationController
  -> LocationService
      -> LocationCatalog

UserLocationController
  -> UserLocationService
      -> UserRepository
      -> LocationCatalog
```

책임:

- `LocationOption`: code, name, nx, ny를 가진 내장 catalog 항목이다.
- `LocationCatalog`: 전체 위치 목록, keyword 검색, code 조회를 담당한다.
- `UserLocationService`: 현재 인증 사용자 위치 조회와 선택을 담당한다.
- `User`: 현재 선택된 위치 snapshot을 저장한다.

내장 catalog code가 존재하지 않으면 `LOCATION_NOT_FOUND`로 실패한다.

## Preference 구조
선호도는 `users` 테이블의 JSON 문자열 컬럼으로 시작한다.

```text
UserPreferencesController
  -> UserPreferencesService
      -> UserRepository
      -> PreferenceJsonMapper
```

컬럼:

- `preferred_colors_json`
- `preferred_materials_json`
- `style_tags_json`

API DTO는 배열을 사용한다.

- `preferredColors`
- `preferredMaterials`
- `styleTags`

`preferredColors`와 `preferredMaterials`만 `preferenceScore`에 반영한다. `styleTags`는 저장/조회/표시만 하며 추천 점수와 추천 이유에는 반영하지 않는다.

## Weather Provider 구조
`RecommendationService`는 계속 `WeatherProvider` 인터페이스에만 의존한다.

```text
RecommendationService
  -> WeatherProvider#getCurrentWeather(userId)
      -> KmaVilageForecastWeatherProvider
          -> UserLocationReader
          -> short in-memory weather snapshot cache
          -> KmaVilageForecastClient
          -> KmaForecastBaseTimeCalculator
          -> KmaWeatherConditionMapper
          -> StaticWeatherProvider fallback
```

현재 기준:

- KMA 요청의 `nx`, `ny`는 현재 인증 사용자 위치에서 온다.
- 기존 `KMA_NX`, `KMA_NY`는 기존 구현/로컬 기본값 호환용이다.
- 사용자 위치가 비어 있으면 서울특별시 `SEOUL`, `60`, `127`로 보정한다.
- `KmaVilageForecastWeatherProvider`는 기본 `WeatherProvider` bean이며 `@Primary`로 둔다.
- `StaticWeatherProvider`는 fallback/test 구현체로 유지한다.
- `KmaVilageForecastWeatherProvider`는 프로세스 메모리에서 2분 TTL weather snapshot cache를 사용한다.
- cache key는 현재 사용자 id, 위치 code/nx/ny, 요청 시점에 계산한 KMA base date/time, 서비스키 설정 여부, fallback enabled 여부로 구성한다.
- cache는 성공적으로 얻은 KMA 또는 fallback `WeatherCondition`만 저장하며 strict mode 실패는 저장하지 않는다.

## 현재 날씨 요약 흐름
`GET /api/weather/current` 요청 흐름:

1. 인증 principal에서 현재 사용자 id를 얻는다.
2. `WeatherProvider#getCurrentWeather(userId)`로 현재 사용자 위치 기준 `WeatherCondition`을 조회한다.
3. 요청 시점의 KMA base date/time과 사용자 위치 기준으로 cache hit가 있으면 cached `WeatherCondition`을 반환한다.
4. KMA 설정이 유효하면 사용자 위치의 `nx`, `ny`로 `getVilageFcst` JSON을 호출한다.
5. KMA 호출 또는 매핑이 실패하면 `WEATHER_FALLBACK_ENABLED=true`에서는 fallback `WeatherCondition`을 사용하고, `false`에서는 `INTERNAL_SERVER_ERROR`로 실패한다.
6. 성공적으로 얻은 `WeatherCondition`을 2분 TTL로 cache하고 `temperature`, `weatherType`, `rainy`, `windy`만 포함한 현재 사용자 전용 response DTO를 반환한다.

이 흐름은 추천 결과를 생성하거나 저장하지 않으며 추천 이력, 착용 이력, 점수 계산에 영향을 주지 않는다.

## 추천 유스케이스 흐름
`POST /api/recommendations` 요청 흐름:

1. 인증 principal에서 현재 사용자 id를 얻는다.
2. 사용자 위치 snapshot을 조회한다. 없으면 서울 기본값으로 backfill하고 저장한 뒤 사용한다.
3. 사용자 선호 색상/소재를 조회한다.
4. `WeatherProvider#getCurrentWeather(userId)`로 `WeatherCondition`을 조회한다.
5. 같은 cache key의 2분 TTL snapshot이 있으면 현재 날씨 요약 API와 같은 `WeatherCondition`을 재사용한다.
6. cache miss이면 KMA 설정과 fallback 규칙에 따라 새 `WeatherCondition`을 얻는다.
7. 현재 사용자 `archived=false`인 옷 목록을 조회한다.
8. 날씨 조건에 맞지 않는 옷을 필터링한다.
9. TOP/BOTTOM 또는 TOP/BOTTOM/OUTER 조합을 생성한다.
10. 후보 조합별 점수를 계산한다.
11. 최근 착용 이력, 최근 추천 이력, 선호 색상/소재를 반영한다.
12. 최고 점수 후보를 선택한다.
13. 추천 이유를 생성한다.
14. `RecommendationResult`를 생성하고 저장한다.
15. 현재 사용자 전용 response DTO를 반환한다.

기존 다양성 점수는 인증 사용자 baseline에서 제거하고 `preferenceScore`로 교체했다.

## 추천 이력 조회 흐름
`GET /api/recommendations?limit={limit}`는 현재 인증 사용자 추천 결과를 최신순으로 조회한다.

Limit 정책:

- 기본값 `20`
- 최소 `1`
- 최대 `50`
- 범위 밖 또는 숫자가 아닌 값은 `400 INVALID_REQUEST`

## 트랜잭션 경계
- 회원가입: write transaction
- 로그인: readOnly transaction
- 현재 사용자 조회: readOnly transaction
- 옷 등록/수정/보관 처리: write transaction
- 옷 목록/상세 조회: readOnly transaction
- 위치 catalog 조회: readOnly 또는 in-memory 조회
- 사용자 위치 조회: 위치 snapshot이 있으면 readOnly transaction, 서울 기본값 backfill이 필요하면 write transaction
- 사용자 위치 선택: write transaction
- 사용자 선호도 조회: readOnly transaction
- 사용자 선호도 저장: write transaction
- 현재 날씨 요약: 위치 조회/backfill과 KMA/fallback 조회를 수행하되 추천 결과를 저장하지 않는다.
- 추천 생성: 위치 조회/backfill, KMA 호출, 추천 저장을 분리한다. 최종 `RecommendationResult` 저장은 write transaction이다.
- 추천 이력 조회: readOnly transaction
- 착용 완료 처리: `RecommendationResult` 상태 변경과 `WearHistory` 저장이 필요하므로 write transaction

외부 KMA 호출은 DB transaction을 길게 잡지 않는다.

## Docker Compose 공유 기준
현재 공유 기준은 아래 3개 서비스다.

```text
mysql
app
frontend
```

MVP4 데모 전 로컬 Docker Compose DB는 기존 schema/seed data와 충돌할 수 있으므로 초기화를 권장한다.

```bash
docker compose down -v
docker compose up --build
```

운영 DB migration은 MVP4 문서 범위에서 다루지 않는다. 로컬 공유/데모 기준은 volume 초기화로 정리한다.

## MVP4 아키텍처 비변경 사항
- 새 공개 API를 추가하지 않는다. 현재 날씨 요약은 보호 API로만 제공한다.
- `userId` query parameter를 되살리지 않는다.
- DB schema를 변경하지 않는다.
- 추천 scoring, tie-break, failure code를 변경하지 않는다.
- 외부 지도/주소 API, browser geolocation, AI/GPT provider, image storage provider를 추가하지 않는다.

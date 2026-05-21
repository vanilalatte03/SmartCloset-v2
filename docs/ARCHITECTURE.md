# 아키텍처: SmartCloset 2차 MVP

## 전체 아키텍처 개요
SmartCloset 2차 MVP는 Spring Boot 4.0.6 백엔드와 React+Vite+TypeScript 프론트엔드 앱으로 구성한다. 백엔드는 기존 추천 도메인과 KMA weather provider를 유지하면서 사용자별 위치 저장과 위치 catalog API를 추가한다.

전체 백엔드 요청 흐름은 기존 계층 구조를 유지한다.

```text
Controller -> Application Service -> Domain Service -> Repository / Provider
```

Controller는 HTTP 요청과 응답만 처리한다. Application Service는 유스케이스와 트랜잭션 경계를 관리한다. 추천 후보 생성, 점수 계산, 추천 실패 판단, 추천 이유 생성은 Domain Service에서 처리한다. Repository는 JPA 기반 데이터 접근만 담당한다.

프론트엔드는 `frontend/` 아래 Vite React TypeScript SPA로 두고, 백엔드 REST API를 호출한다. 현재 문서 전환 시점에는 `frontend/`가 아직 없으므로 첫 frontend 구현 step에서 스캐폴드와 Docker Compose `frontend` 서비스를 함께 추가한다. 프론트 상세 기준은 `docs/FRONTEND.md`를 따른다.

## 권장 패키지 구조

```text
com.smartcloset
├── SmartClosetApplication
├── common
│   ├── exception
│   ├── response
│   └── config
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
    │   ├── clothes
    │   ├── location
    │   └── recommendation
    ├── types
    └── main.tsx
```

Spring Boot static Demo UI는 2차의 주 제품 화면이 아니다. 유지하더라도 API smoke 확인용 보조 화면으로만 취급한다.

## 계층별 책임

### presentation/controller
- HTTP 요청/응답 처리
- request validation
- Application Service 호출
- DTO 변환
- 비즈니스 규칙 직접 처리 금지

### application/service
- 유스케이스 조합
- 트랜잭션 경계 관리
- Repository 호출
- `WeatherProvider` 호출
- Domain Service 호출
- 위치 catalog 조회와 사용자 위치 변경 orchestration
- 응답 DTO 구성

### domain
- Entity, Enum, Value Object
- 추천 후보 생성 규칙
- 점수 계산 규칙
- 추천 실패 판단
- 추천 이유 생성 규칙
- 위치 catalog 항목 value object
- 가능한 한 순수 Java 로직으로 유지

### repository
- JPA 기반 데이터 접근
- Entity 저장/조회
- 추천 점수 계산 로직 금지
- 후보 조합 생성 로직 금지
- KMA category 매핑 금지

### infrastructure/provider
- `WeatherProvider#getCurrentWeather(Long userId)` 구현
- 사용자 위치의 `nx`, `ny`를 사용한 KMA API 호출
- JSON 응답 파싱, category 매핑
- 외부 API 오류를 fallback 또는 strict mode 실패로 변환
- 추천 도메인이 KMA 응답 DTO에 의존하지 않도록 내부 `WeatherCondition`으로 변환

## Location 구조
2차 위치 선택은 외부 위치 API 없이 내장 대표 격자 catalog를 사용한다.

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
- `UserLocationService`: 사용자 위치 조회와 선택을 담당한다.
- `User`: 현재 선택된 위치 snapshot을 저장한다.

내장 catalog code가 존재하지 않으면 `LOCATION_NOT_FOUND`로 실패한다.

## Weather Provider 구조
`RecommendationService`는 계속 `WeatherProvider` 인터페이스에만 의존한다.

```text
RecommendationService
  -> WeatherProvider#getCurrentWeather(userId)
      -> KmaVilageForecastWeatherProvider
          -> UserLocationReader
          -> KmaVilageForecastClient
          -> KmaForecastBaseTimeCalculator
          -> KmaWeatherConditionMapper
          -> StaticWeatherProvider fallback
```

2차 변경점:

- KMA 요청의 `nx`, `ny`는 환경변수 기본값이 아니라 사용자 위치에서 온다.
- 기존 `KMA_NX`, `KMA_NY`는 기존 구현/로컬 기본값 호환용이며 2차 사용자별 추천의 source of truth가 아니다.
- 사용자 위치가 비어 있으면 서울특별시 `SEOUL`, `60`, `127`로 보정한다.

내부 인터페이스 기준:

```java
public record KmaGrid(int nx, int ny) {
}

public interface KmaForecastClient {
    List<KmaForecastItem> getVilageForecast(KmaForecastBaseTime baseTime, KmaGrid grid);
}
```

`KmaVilageForecastClient`는 URI 생성 시 `KmaWeatherProperties#nx`, `KmaWeatherProperties#ny`가 아니라 전달받은 `KmaGrid`를 사용한다.

유지되는 규칙:

- `KmaVilageForecastWeatherProvider`는 기본 `WeatherProvider` bean이며 `@Primary`로 둔다.
- `StaticWeatherProvider`는 fallback/test 구현체로 유지한다.
- KMA 응답 DTO는 `weather.infrastructure` 밖으로 노출하지 않는다.
- 추천 도메인은 내부 `WeatherCondition`만 사용한다.

## 추천 유스케이스 흐름
`POST /api/recommendations?userId={userId}` 요청 흐름:

1. `userId`로 seed/test user를 조회한다.
2. 사용자 위치 snapshot을 조회한다. 없으면 애플리케이션에서 서울 기본값으로 backfill하고 저장한 뒤 사용한다.
3. `WeatherProvider#getCurrentWeather(Long userId)`로 `WeatherCondition`을 조회한다.
4. KMA 설정이 유효하면 사용자 위치의 `nx`, `ny`로 `getVilageFcst` JSON을 호출한다.
5. KMA 응답에서 `TMP`, `SKY`, `PTY`, `PCP`, `WSD`를 매핑한다.
6. KMA 호출 또는 매핑이 실패하면 `WEATHER_FALLBACK_ENABLED=true`에서는 fallback `WeatherCondition`을 사용하고, `false`에서는 `INTERNAL_SERVER_ERROR`로 실패한다.
7. `archived=false`인 옷 목록을 조회한다.
8. 날씨 조건에 맞지 않는 옷을 필터링한다.
9. TOP/BOTTOM 또는 TOP/BOTTOM/OUTER 조합을 생성한다.
10. 후보 조합별 점수를 계산한다.
11. 최근 착용 이력과 최근 추천 이력을 반영한다.
12. 최고 점수 후보를 선택한다.
13. 추천 이유를 생성한다.
14. `RecommendationResult`를 생성하고 저장한다.
15. 응답 DTO를 반환한다.

## KMA 요청 구성
외부 호출은 아래 endpoint로 제한한다.

```text
GET {KMA_BASE_URL}/getVilageFcst
```

요청 parameter:

| Parameter | Source |
| --- | --- |
| `serviceKey` | `KMA_SERVICE_KEY` |
| `pageNo` | fixed `1` |
| `numOfRows` | fixed `1000` |
| `dataType` | fixed `JSON` |
| `base_date` | `KmaForecastBaseTimeCalculator` |
| `base_time` | `KmaForecastBaseTimeCalculator` |
| `nx` | 사용자 위치 `locationNx` |
| `ny` | 사용자 위치 `locationNy` |

단기예보 발표시각:

```text
0200, 0500, 0800, 1100, 1400, 1700, 2000, 2300
```

각 발표시각 10분 이후부터 API에서 사용할 수 있다고 보고, 현재 KST 기준 제공 가능한 최신 발표시각을 선택한다.

## WeatherCondition 매핑
KMA 응답 item은 같은 forecast time끼리 묶어서 사용한다.

forecast target time 선택 기준은 현재 KST 이후 가장 가까운 예보시각이다. 선택 group에 필수 category가 하나라도 누락되거나 값 파싱에 실패하면 다른 group으로 이동하지 않고 provider에 실패를 반환한다.

| Internal field | KMA category | Mapping |
| --- | --- | --- |
| `temperature` | `TMP` | `fcstValue`를 정수 섭씨로 변환 |
| `weatherType` | `PTY`, `SKY` | `PTY` 우선, `PTY=0`이면 `SKY` 사용 |
| `rainy` | `PTY`, `PCP` | `PTY != 0` 또는 유효 강수량이면 true |
| `windy` | `WSD` | `WSD >= 4.0`이면 true |

Weather type:

| KMA value | WeatherType |
| --- | --- |
| `PTY=1`, `PTY=2`, `PTY=4` | `RAINY` |
| `PTY=3` | `SNOWY` |
| `PTY=0`, `SKY=1` | `SUNNY` |
| `PTY=0`, `SKY=3` 또는 `SKY=4` | `CLOUDY` |

`PCP`가 `-`, `null`, `0`, `강수없음`이면 강수 없음으로 본다.

## 트랜잭션 경계
- 옷 등록/수정/보관 처리: write transaction
- 옷 목록/상세 조회: readOnly transaction
- 위치 catalog 조회: readOnly 또는 in-memory 조회
- 사용자 위치 조회: 위치 snapshot이 있으면 readOnly transaction, 서울 기본값 backfill이 필요하면 write transaction
- 사용자 위치 선택: write transaction
- 추천 생성: 위치 조회/backfill, KMA 호출, 추천 저장을 분리한다. 최종 `RecommendationResult` 저장은 write transaction이다.
- 착용 완료 처리: `RecommendationResult` 상태 변경과 `WearHistory` 저장이 필요하므로 write transaction

외부 KMA 호출은 DB transaction을 길게 잡지 않는다. 권장 흐름은 위치 snapshot 확보와 필요한 backfill 저장을 짧게 끝낸 뒤, KMA 호출을 transaction 밖에서 수행하고, 추천 결과 저장만 별도 write transaction으로 처리하는 것이다. 구현 단순성을 위해 하나의 application service에서 조합하더라도 외부 호출 전에 불필요한 DB lock이나 변경 감지를 오래 유지하지 않게 책임을 분리한다.

## Frontend 구조
프론트엔드는 React+Vite+TypeScript SPA다.

주요 책임:

- 사용자 위치 조회/검색/선택
- 옷 목록 조회와 등록
- 추천 생성
- 추천 결과 표시
- 착용 완료 처리
- API 에러와 추천 실패 코드 표시

프론트는 백엔드 도메인 규칙을 재구현하지 않는다. 추천 가능 여부, 점수 계산, KMA 매핑은 모두 백엔드가 담당한다.

## Docker Compose 구성
2차 frontend 구현 완료 후 공유 기준은 아래 3개 서비스다. 현재 문서 전환 직후에는 `frontend/`와 Compose `frontend` 서비스가 아직 없을 수 있다.

- `mysql`: MySQL DB
- `app`: Spring Boot API
- `frontend`: React+Vite+TypeScript 앱

기본 접속 경로:

- Frontend(2차 frontend step 완료 후): `http://localhost:5173`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## 의존 방향 규칙
- Controller는 Application Service에만 의존한다.
- Service는 Repository, `WeatherProvider`, Domain Service를 조합한다.
- Domain 로직은 Controller, JPA, HTTP, Swagger, KMA DTO, React DTO에 의존하지 않는다.
- `RecommendationService`는 KMA client를 직접 호출하지 않는다.
- Repository는 추천 점수 계산을 하지 않는다.
- 프론트엔드는 백엔드 API 계약에만 의존하고 DB 구조에 의존하지 않는다.

## 테스트 기준
- 위치 catalog 검색/조회 테스트
- 사용자 위치 조회/수정 API 테스트
- 잘못된 `locationCode`의 `LOCATION_NOT_FOUND` 테스트
- 사용자 위치 `nx`, `ny`가 KMA 요청에 반영되는 통합 테스트
- fallback/strict KMA mode 기존 테스트 유지
- 추천 스코어링, 날씨 필터링, 색상 점수, 최근 착용/추천 이력 테스트 유지
- 프론트 TypeScript type check와 build
- React 앱 핵심 흐름 smoke 테스트

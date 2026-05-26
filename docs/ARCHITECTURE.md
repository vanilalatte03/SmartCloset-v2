# 아키텍처: SmartCloset MVP7

## 전체 아키텍처 개요

SmartCloset MVP7은 Spring Boot 4.0.6 백엔드와 React+Vite+TypeScript 프론트엔드 앱으로 구성한다. MVP7의 변경 지점은 KMA 위치 catalog 확장, 브라우저 좌표 resolve, forecast period 기반 weather 조회, 위치/날씨 source snapshot 저장과 표시다.

기존 인증, 옷 이미지 저장, 추천 피드백/개인화, 추천 이력 구조는 유지한다.

```text
Controller -> Application Service -> Domain Service -> Repository / Provider
```

추천 점수 계산은 recommendation domain service에 둔다. Controller와 Repository에는 점수 계산 로직을 두지 않는다.

## 권장 패키지 구조

```text
com.smartcloset
├── auth
├── common
├── security
├── user
├── location
│   ├── domain
│   ├── application
│   ├── dto
│   └── presentation
├── weather
│   ├── domain
│   ├── application
│   └── infrastructure
├── clothing
└── recommendation
    ├── domain
    ├── repository
    ├── application
    ├── presentation
    └── dto
```

MVP7 location/weather에는 아래 개념을 둔다.

- `LocationSource`
- KMA 행정구역 `LocationCatalog`
- `KmaGridConverter`
- `LocationResolveService`
- `ForecastPeriod`
- `WeatherSnapshot`
- `WeatherSource`
- `WeatherLocationSnapshot`

프론트엔드:

```text
frontend/src
├── api
├── components
├── features
│   ├── auth
│   ├── clothes
│   ├── location
│   ├── preferences
│   ├── recommendation
│   └── history
├── types
└── main.tsx
```

## 인증 경계

공개 API:

- `POST /api/auth/signup`
- `POST /api/auth/login`

그 외 `/api/**` endpoint는 보호 API다.

MVP7 신규/변경 보호 API:

- `GET /api/locations?keyword={keyword}` 확장
- `POST /api/locations/resolve`
- `PUT /api/users/me/location` optional `source`
- `GET /api/weather/current` source metadata 포함
- `POST /api/recommendations` optional `forecastPeriod`

모든 사용자 소유 데이터는 인증 principal의 현재 사용자 id로 제한한다.

## 위치 catalog 흐름

```text
GET /api/locations?keyword=일산동
  -> LocationController
  -> LocationService.searchLocations(keyword)
      -> LocationCatalog.search(keyword)
      -> LocationOptionResponse
```

정책:

- KMA 단기예보 격자 위경도 자료를 application resource로 사용한다.
- DB table로 만들지 않고 read-only catalog로 시작한다.
- 검색은 code, fullName, region1, region2, region3 기준으로 수행한다.
- 동명이인은 모두 반환하고 client가 선택하게 한다.
- 외부 주소/지도 API를 호출하지 않는다.

## 브라우저 좌표 resolve 흐름

```text
POST /api/locations/resolve
  -> LocationController
  -> LocationResolveService.resolve(latitude, longitude)
      -> KmaGridConverter.toGrid(latitude, longitude)
      -> LocationCatalog.findNearest(grid, coordinate)
      -> LocationResolveResponse
```

정책:

- 브라우저 Geolocation API 호출은 프론트 사용자 클릭 뒤에만 수행한다.
- 서버는 좌표를 KMA grid로 변환하고 가까운 catalog 후보를 반환한다.
- 좌표 원문은 DB에 저장하지 않는다.
- 후보 선택 후 `PUT /api/users/me/location`을 호출해야 사용자 위치가 저장된다.

## 사용자 위치 저장 흐름

```text
PUT /api/users/me/location
  -> UserLocationController
  -> UserLocationService.updateUserLocation(userId, request)
      -> LocationCatalog.findByCode(locationCode)
      -> User.updateLocation(location, source)
      -> UserLocationResponse
```

정책:

- `source` 누락 시 `MANUAL_SEARCH`다.
- 저장 값은 location code/name/fullName/region/grid/source다.
- 브라우저 좌표 원문이나 catalog latitude/longitude는 사용자 row에 저장하지 않는다.
- 신규 사용자는 Seoul `SEOUL`, `nx=60`, `ny=127`, `MANUAL_SEARCH`로 시작한다.

## WeatherProvider 흐름

```text
WeatherProvider.getWeather(userId, forecastPeriod)
  -> UserLocationReader.getRequiredLocationSnapshot(userId)
  -> KmaForecastBaseTimeCalculator.calculate(clock)
  -> KmaVilageForecastClient.getVilageForecast(baseTime, grid)
  -> KmaWeatherConditionMapper.map(items, forecastPeriod, now)
  -> WeatherSnapshot(condition, location, source)
```

정책:

- `getVilageFcst` JSON만 외부 weather API로 사용한다.
- `WeatherSnapshot`은 내부 `WeatherCondition`, `WeatherLocationSnapshot`, `WeatherSource`를 포함한다.
- KMA 성공 시 `provider=KMA_VILAGE_FORECAST`, `kmaUsed=true`, `fallbackUsed=false`다.
- 서비스키 미설정, KMA 실패, mapping 실패는 fallback enabled일 때 `StaticWeatherProvider`로 전환한다.
- fallback 시 `provider=STATIC_FALLBACK`, `kmaUsed=false`, `fallbackUsed=true`다.
- raw KMA 응답 JSON은 domain, DB, API response에 전달하지 않는다.

## 추천 생성 흐름

```text
POST /api/recommendations
  -> RecommendationController
  -> RecommendationService.createRecommendation(userId, situation, forecastPeriod)
      -> WeatherProvider.getWeather(userId, forecastPeriod)
      -> UserRepository / ClothingItemRepository
      -> recent wear/recommendation/feedback snapshots
      -> WeatherSuitabilityFilter
      -> OutfitCandidateGenerator
      -> RecommendationScorer
      -> RecommendationReasonGenerator
      -> RecommendationResultRepository
      -> RecommendationResultItemRepository
```

정책:

- request body가 없거나 `situation`이 없으면 `CASUAL`을 사용한다.
- `forecastPeriod`가 없으면 `CURRENT`를 사용한다.
- 추천 상황과 forecastPeriod는 `recommendation_results`에 snapshot으로 저장한다.
- weather condition과 source metadata도 `recommendation_results`에 snapshot으로 저장한다.
- score snapshot은 기존 field를 유지한다.
- `preferenceScore` 내부에서 색상, 소재, style tag, 피드백 보정을 계산한다.
- 추천 생성은 피드백이나 착용 이력을 생성하지 않는다.

## 현재 날씨 조회 흐름

```text
GET /api/weather/current
  -> CurrentWeatherController
  -> CurrentWeatherService.getCurrentWeather(userId)
      -> WeatherProvider.getWeather(userId, CURRENT)
      -> WeatherResponse
```

정책:

- 현재 사용자 위치와 `CURRENT` 기준 source metadata를 반환한다.
- 추천 결과, 추천 이력, 착용 이력, 피드백을 생성하거나 변경하지 않는다.

## 추천 이력 조회 흐름

```text
GET /api/recommendations?limit=20
  -> RecommendationService.getRecommendationHistory
      -> recommendation result ids latest first
      -> result items with clothing item
      -> wear history by recommendation id
      -> RecommendationResponse
```

MVP7 이력 응답은 아래를 포함한다.

- `situation`
- `forecastPeriod`
- `weather.location`
- `weather.source`
- `worn`
- `wornAt`
- `feedback`
- outfit item image metadata
- outfit item styleTags

## 트랜잭션 경계

- 위치 검색: read-only, transaction 불필요 또는 readOnly
- 좌표 resolve: read-only, DB write 없음
- 사용자 위치 저장: write transaction
- 현재 날씨 조회: 추천 관련 write 없음
- 추천 생성: weather 조회 후 write transaction으로 추천 결과와 result items 저장
- 추천 이력 조회: readOnly transaction
- 추천 피드백 저장/clear: write transaction
- 착용 완료: write transaction, idempotent
- 옷 등록/수정: write transaction, styleTags 포함
- 옷 이미지 업로드/교체/조회/삭제: MVP5 transaction 및 파일 보상 처리 유지

## 프론트 구조

- `src/types/api.ts`에 MVP7 request/response type을 명시한다.
- `src/api/smartClosetApi.ts`에 `resolveLocation(accessToken, request)`, `updateUserLocation(accessToken, request)`, `createRecommendation(accessToken, request?)`를 둔다.
- Location view는 검색과 현재 위치 후보 찾기를 제공한다.
- Today view는 상황 선택, forecastPeriod 선택, 추천 결과 source 표시를 제공한다.
- History view는 각 추천의 위치/날씨 source snapshot을 표시한다.
- 보호 이미지 조회는 계속 Authorization header가 있는 blob fetch와 object URL을 사용한다.

## Storage 정책

옷 이미지 storage 정책은 MVP5를 유지한다.

```yaml
smartcloset:
  clothing:
    image:
      storage-dir: ${CLOTHING_IMAGE_STORAGE_DIR:./uploads/clothing-images}
      max-size-bytes: ${CLOTHING_IMAGE_MAX_SIZE_BYTES:5242880}
```

KMA location catalog는 application resource로 포함한다. Docker Compose에서 별도 외부 API key나 volume을 요구하지 않는다.

## 금지 사항

- 공개 API를 추가하지 않는다. 이유: 인증 사용자 데이터 경계를 유지해야 한다.
- 공개 `userId` query parameter를 추가하지 않는다. 이유: 현재 사용자 경계는 JWT principal이다.
- 외부 주소/지도 API를 추가하지 않는다. 이유: MVP7 P0는 KMA catalog 기반 위치 신뢰도 검증이다.
- 브라우저 GPS 원문 좌표를 DB에 저장하지 않는다. 이유: 위치 후보 선택에만 필요한 민감한 입력이다.
- raw KMA 응답 JSON을 저장하지 않는다. 이유: MVP7 source snapshot은 사람이 확인할 신뢰 필드만 다룬다.
- 기상청 `getVilageFcst` 외 weather API를 추가하지 않는다. 이유: weather provider 외부 의존성을 확장하지 않는다.
- 추천 점수 계산 로직을 Controller나 Repository에 두지 않는다. 이유: 추천 도메인 규칙을 테스트 가능하게 유지해야 한다.
- 이미지 metadata를 추천 점수나 이유에 사용하지 않는다. 이유: MVP7 신뢰도 입력은 위치/날씨 source 표시이며 이미지와 무관하다.
- AI/GPT 추천을 추가하지 않는다. 이유: MVP7은 설명 가능한 규칙 기반 추천이다.

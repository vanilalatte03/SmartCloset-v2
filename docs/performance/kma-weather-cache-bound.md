# KMA 날씨 캐시 TTL/크기 상한

## 배경

KMA `getVilageFcst` provider는 현재 날씨 조회와 추천 생성에서 같은 사용자 위치, 발표 시각, 예보 시간대가 반복될 때 외부 API 호출 지연을 줄이기 위해 process-local cache를 사용한다.

기존 cache는 `ConcurrentHashMap`에 2분 TTL entry를 저장했지만, 만료 entry는 같은 key가 다시 조회될 때만 제거됐다. 또한 cache key에 `userId`와 위치 표시 metadata가 포함되어 같은 KMA grid 날씨라도 사용자별로 중복 저장될 수 있었다.

## 문제

- 장시간 실행 시 만료된 entry가 재조회되지 않으면 map 안에 남아 메모리 상한을 예측하기 어렵다.
- 같은 `nx`, `ny`, KMA base time, forecast period여도 사용자별 key가 달라 중복 KMA 호출과 중복 cache entry가 발생할 수 있다.
- cache TTL은 코드 상수였고 entry 상한은 설정이나 문서로 드러나지 않았다.

## 변경

- `smartcloset.weather.kma.cache-ttl`을 추가했다. 기본값은 기존과 동일한 `2m`이다.
- `smartcloset.weather.kma.cache-max-size`를 추가했다. 기본값은 `256` entry다.
- cache 조회 전에 만료 entry를 제거한다. TTL과 같은 시각에 도달한 entry는 만료로 간주한다.
- 새 entry 저장 후 cache가 max size를 초과하면 가장 먼저 만료될 오래된 entry부터 제거한다.
- cache key에서 `userId`, 위치명, 위치 source를 제거하고 `nx`, `ny`, KMA base date/time, forecast period, service key configured 여부, fallback enabled 여부만 유지한다.
- cache value는 `WeatherCondition`과 `WeatherSource`만 저장한다. `WeatherLocationSnapshot`은 매 요청의 사용자 저장 위치에서 다시 합성한다.

## 성능 영향

같은 KMA grid를 쓰는 사용자가 늘어도 KMA 호출과 cache entry는 grid/base time/forecast period 기준으로 재사용된다. 예를 들어 부산 grid `98,76`에서 같은 base time의 `CURRENT` 날씨를 여러 사용자가 조회하면 첫 요청만 KMA를 호출하고 이후 요청은 TTL 안에서 cache hit가 된다.

메모리 사용량은 `KMA_CACHE_MAX_SIZE`로 상한이 정해진다. 기본 `256` entry는 `nx`, `ny`, forecast period, base time 조합을 짧은 TTL로 보관하는 용도이며, 운영 환경에서 위치 다양성이 커지면 env 값으로 조정할 수 있다.

## 계약 유지

- fallback enabled/disabled와 service key configured 여부는 cache key에 남겨 strict mode 전환 시 기존 fallback cache를 재사용하지 않는다.
- 추천 결과와 날씨 응답의 location snapshot은 현재 사용자 저장 위치 기준으로 유지된다.
- KMA raw JSON은 cache, API response, 추천 이력에 저장하지 않는다.
- 외부 cache, Redis, Caffeine 의존성은 추가하지 않았다.

## 검증

- `KmaVilageForecastWeatherProviderTest`
  - TTL 만료 후 KMA를 다시 호출한다.
  - TTL 만료 entry를 새 cache 저장 전에 제거한다.
  - max size 초과 시 오래된 entry를 제거한다.
  - 같은 grid를 쓰는 여러 사용자가 cache를 공유하면서 각자의 위치 snapshot을 받는다.
  - fallback 설정 변경 시 기존 fallback cache를 재사용하지 않는다.
- `KmaWeatherPropertiesTest`
  - 기본 `cacheTtl=2m`, `cacheMaxSize=256`과 설정 override를 검증한다.
- `KmaWeatherPropertiesApplicationContextTest`
  - `KMA_CACHE_TTL`, `KMA_CACHE_MAX_SIZE` env 값이 `application.yml`을 통해 바인딩되는지 검증한다.

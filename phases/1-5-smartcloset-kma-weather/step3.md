# 단계 3: kma-provider-fallback-wiring

범위: Must-have / 1.5 P0

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/ARCHITECTURE.md`
- `docs/API.md`
- `docs/COMMANDS.md`
- `docs/adr/001-static-weather-provider.md`
- `docs/adr/006-kma-vilage-forecast-weather-provider.md`
- `phases/1-5-smartcloset-kma-weather/step0.md`
- `phases/1-5-smartcloset-kma-weather/step1.md`
- `phases/1-5-smartcloset-kma-weather/step2.md`
- `src/main/java/com/smartcloset/weather/application/WeatherProvider.java`
- `src/main/java/com/smartcloset/weather/infrastructure/StaticWeatherProvider.java`
- `src/main/java/com/smartcloset/recommendation/application/RecommendationService.java`

이전 단계에서 만들어진 KMA 설정, 시간 계산, 매핑 core, HTTP client를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업
`KmaVilageForecastWeatherProvider`를 1.5차 기본 `WeatherProvider`로 연결하고 fallback/strict mode 정책을 구현한다.

## 변경 예상 파일
- `src/main/java/com/smartcloset/weather/infrastructure/kma/**`
- `src/main/java/com/smartcloset/weather/infrastructure/StaticWeatherProvider.java`
- `src/test/java/com/smartcloset/weather/infrastructure/kma/**`
- 필요 시 `src/test/resources/application-test.yml`

## 구현 메모
- `KmaVilageForecastWeatherProvider`는 `WeatherProvider`를 구현하고 `@Primary` bean으로 등록한다.
- `StaticWeatherProvider`는 fallback/test 구현체로 유지한다.
- KMA provider가 fallback을 사용할 때는 `StaticWeatherProvider` concrete type을 주입받는다. `WeatherProvider` 타입으로 fallback을 주입해 자기 자신 또는 다중 bean 충돌을 만들지 않는다.
- 기본 `WEATHER_FALLBACK_ENABLED=true`에서는 아래 상황에서 fallback 값을 반환한다.
  - `KMA_SERVICE_KEY`가 비어 있음
  - KMA HTTP 호출 실패
  - KMA `resultCode`가 `00`이 아님
  - `NODATA_ERROR`
  - `items.item` 비어 있음
  - 선택 forecast group에서 필수 category 누락
  - `TMP`, `PTY`, `SKY`, `PCP`, `WSD` 값 파싱 실패
- fallback 값은 기존과 동일하다.
  - `temperature=12`
  - `weatherType=CLOUDY`
  - `rainy=false`
  - `windy=false`
- `WEATHER_FALLBACK_ENABLED=false`는 strict KMA mode다.
  - 같은 오류에서 fallback하지 않는다.
  - `SmartClosetException(ErrorCode.INTERNAL_SERVER_ERROR)` 또는 전역 handler가 `INTERNAL_SERVER_ERROR`로 변환하는 예외를 발생시킨다.
  - 추천 실패 코드 5종으로 변환하지 않는다.
- 현재 KST를 주입 가능한 `Clock` 또는 테스트 가능한 시간 공급 방식으로 다루면 좋다.

## 검증 절차
```bash
git diff --check
! rg -n 'GET /api/recommendations/(today)' . --glob '!archive/**'
./gradlew test
```

## 인수 기준
- application context에서 `WeatherProvider` 단일 주입이 `KmaVilageForecastWeatherProvider`로 해결된다.
- `StaticWeatherProvider` bean은 fallback/test 용도로 유지된다.
- 서비스키 미설정 + fallback enabled에서 `StaticWeatherProvider` fallback 값이 반환된다.
- KMA client/mapper 실패 + fallback enabled에서 fallback 값이 반환된다.
- fallback disabled strict mode에서 같은 실패가 `INTERNAL_SERVER_ERROR` 경로로 이어진다.
- 기존 추천 API 테스트는 서비스키 없이도 fallback으로 계속 통과한다.

## 금지사항
- `StaticWeatherProvider`를 제거하지 마라. 이유: fallback과 테스트 구현체로 유지하기로 결정했다.
- KMA provider와 Static provider 사이에 `WeatherProvider` 타입 순환 의존을 만들지 마라. 이유: provider bean 충돌과 자기 주입 위험이 있다.
- strict mode 실패를 추천 실패 코드 5종으로 매핑하지 마라. 이유: 외부 KMA 실패는 추천 후보 부족과 의미가 다르다.
- KMA 원본 응답이나 fallback 여부를 DB/API 응답에 추가하지 마라. 이유: 1.5차 공개 API와 DB snapshot 구조는 변경하지 않는다.

# 단계 0: kma-configuration-contract

범위: Must-have / 1.5 P0

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/ARCHITECTURE.md`
- `docs/API.md`
- `docs/COMMANDS.md`
- `docs/adr/006-kma-vilage-forecast-weather-provider.md`
- `src/main/resources/application.yml`
- `.env.example`
- `docker-compose.yml`
- `src/main/java/com/smartcloset/weather/application/WeatherProvider.java`
- `src/main/java/com/smartcloset/weather/infrastructure/StaticWeatherProvider.java`

## 작업
KMA provider 구현에 필요한 설정 계약을 코드에 준비한다. 이 단계는 설정 바인딩과 테스트 가능한 configuration 골격까지만 다루고, 실제 KMA HTTP 호출과 `WeatherProvider` 교체는 이후 step에서 한다.

## 변경 예상 파일
- `src/main/resources/application.yml`
- `src/main/java/com/smartcloset/weather/infrastructure/kma/**`
- `src/test/java/com/smartcloset/weather/infrastructure/kma/**`

## 구현 메모
- 환경변수는 문서 기준으로 아래 값을 지원한다.
  - `KMA_SERVICE_KEY`
  - `KMA_NX`, 기본 `60`
  - `KMA_NY`, 기본 `127`
  - `KMA_BASE_URL`, 기본 `http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0`
  - `WEATHER_FALLBACK_ENABLED`, 기본 `true`
- Spring 설정은 `application.yml`에서 앱 내부 property로 바인딩한다. 예: `smartcloset.weather.kma.*`, `smartcloset.weather.fallback-enabled`.
- `@ConfigurationProperties`를 사용한다면 Spring Boot 4.0.6 기준으로 컴파일되는 annotation import와 등록 방식을 사용한다.
- 서비스키는 빈 문자열을 허용한다. 이유: 서비스키가 없어도 fallback 데모가 가능해야 한다.
- `KMA_NX`, `KMA_NY`는 앱 전역 기본 위치이며 사용자별 위치 저장이나 API를 만들지 않는다.
- 이 단계가 끝난 뒤에도 기본 `WeatherProvider`는 기존 `StaticWeatherProvider` 하나여야 한다.

## 검증 절차
```bash
git diff --check
! rg -n 'GET /api/recommendations/(today)' . --glob '!archive/**'
./gradlew test
```

## 인수 기준
- KMA 설정 값이 환경변수 기본값과 함께 Spring configuration으로 바인딩된다.
- 기본값이 문서와 일치한다: `nx=60`, `ny=127`, fallback enabled `true`, KMA base URL.
- `KMA_SERVICE_KEY`가 비어 있어도 application context가 뜬다.
- 실제 API key 값은 코드, 테스트, 문서에 추가되지 않는다.
- `StaticWeatherProvider`의 기존 테스트와 추천 API 테스트가 계속 통과한다.

## 금지사항
- 실제 KMA HTTP 호출을 구현하지 마라. 이유: 호출/응답 검증은 별도 client step에서 다룬다.
- `KmaVilageForecastWeatherProvider`를 아직 `WeatherProvider` bean으로 등록하지 마라. 이유: provider 교체와 bean 충돌 검증은 wiring step에서 다룬다.
- 사용자 위치 저장 테이블이나 위치 변경 API를 만들지 마라. 이유: 1.5차 위치는 `KMA_NX`, `KMA_NY` 환경변수로만 관리한다.
- 실제 공공데이터 API key를 커밋하지 마라. 이유: 서비스키는 민감정보다.

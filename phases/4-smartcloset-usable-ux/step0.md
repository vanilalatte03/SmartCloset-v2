# 단계 0: current-weather-api

범위: Must-have / MVP4 P0

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `README.md`
- `docs/PRD.md`
- `docs/ARCHITECTURE.md`
- `docs/API.md`
- `docs/RECOMMENDATION_RULES.md`
- `docs/COMMANDS.md`
- `docs/adr/009-mvp4-usable-ux.md`
- `src/main/java/com/smartcloset/weather/application/WeatherProvider.java`
- `src/main/java/com/smartcloset/recommendation/dto/WeatherResponse.java`
- `src/main/java/com/smartcloset/security/SecurityConfig.java`
- `src/main/java/com/smartcloset/recommendation/application/RecommendationService.java`
- `src/test/java/com/smartcloset/security/SecurityBoundaryRegressionTest.java`
- `src/test/java/com/smartcloset/weather/**`

## 작업
MVP4 Today 화면에서 현재 사용자 위치 기준 날씨를 추천 생성 전에 보여줄 수 있도록 보호 API `GET /api/weather/current`를 구현한다. 이 단계는 MVP4의 유일한 backend API 추가 단계다.

## 변경 예상 파일
- `src/main/java/com/smartcloset/weather/application/**`
- `src/main/java/com/smartcloset/weather/presentation/**`
- `src/main/java/com/smartcloset/recommendation/dto/WeatherResponse.java`
- `src/main/java/com/smartcloset/security/SecurityConfig.java`
- `src/test/java/com/smartcloset/weather/**`
- `src/test/java/com/smartcloset/security/**`

## 구현 메모
- `GET /api/weather/current`는 보호 API다. `Authorization: Bearer {accessToken}` 없이 호출하면 `401`로 실패해야 한다.
- Controller는 `CurrentUserPrincipal`에서 `userId`를 얻는다.
- Controller -> Application Service -> `WeatherProvider#getCurrentWeather(userId)` 흐름을 유지한다.
- 응답은 `{ "data": { "temperature", "weatherType", "rainy", "windy" } }` 형태다.
- `WeatherResponse.from(WeatherCondition)` 같은 기존 mapping을 재사용한다.
- `SecurityConfig`는 현재 `/api/**` 보호 정책으로 충분하면 endpoint별 permit/deny rule을 새로 늘리지 않는다.
- 이 API는 추천 결과, 추천 이력, 착용 이력, 점수 snapshot을 생성하거나 저장하지 않는다.
- `WeatherProvider`의 KMA/fallback/strict mode 동작은 기존 provider 규칙을 그대로 따른다.
- 새 공개 API를 추가하지 않는다.
- `SecurityBoundaryRegressionTest`의 보호 API 목록에 `GET /api/weather/current`를 추가한다.
- current weather controller/service test에는 fallback weather `temperature=12`, `weatherType=CLOUDY`, `rainy=false`, `windy=false` 응답을 포함한다.
- current weather 응답에 `userId`가 없는지 검증한다.
- API 호출 전후 `RecommendationResult`와 `WearHistory`가 증가하지 않는지 검증한다.
- strict KMA mode에서 provider 실패가 기존 규칙대로 `INTERNAL_SERVER_ERROR`로 전파되는지 검증한다.

## 검증 절차
```bash
git diff --check
rg -n 'GET /api/weather/current|weather/current' README.md docs src/main/java src/test/java
rg -n 'weather/current' src/test/java/com/smartcloset/security/SecurityBoundaryRegressionTest.java src/test/java/com/smartcloset/weather
! rg -n 'weather/current.*permitAll|requestMatchers\\(.*weather/current.*permitAll' src/main/java
./gradlew test
./gradlew build
```

## 인수 기준
- `GET /api/weather/current`가 인증 사용자 기준 현재 위치 날씨를 반환한다.
- 응답 DTO에 `userId`가 없다.
- token 없음/만료/잘못된 token은 보호 API 기준으로 실패한다.
- API 호출은 `RecommendationResult` 또는 `WearHistory`를 생성하지 않는다.
- fallback enabled 환경에서 KMA key가 없어도 fallback weather를 반환할 수 있다.
- strict KMA mode의 실패 정책은 기존 weather provider 규칙을 따른다.

## 금지사항
- 새 공개 API를 만들지 마라. 이유: MVP4 공개 API는 auth 2종뿐이다.
- `GET /api/weather/current`에서 추천 결과를 생성하거나 저장하지 마라. 이유: 이 API는 날씨 요약 조회 전용이다.
- DB table 또는 column을 추가하지 마라. 이유: MVP4 DB schema 변경은 없다.
- KMA 외 weather provider를 추가하지 마라. 이유: 외부 Weather API는 `getVilageFcst` JSON만 허용한다.
- `userId` query parameter를 받지 마라. 이유: 현재 사용자 식별은 인증 principal 기준이다.
- today 추천 GET endpoint를 만들지 마라. 이유: 추천 생성 API는 `POST /api/recommendations`만 사용한다.

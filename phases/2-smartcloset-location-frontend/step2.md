# 단계 2: kma-user-grid-integration

범위: Must-have / 2차 P0

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/API.md`
- `docs/ARCHITECTURE.md`
- `docs/RECOMMENDATION_RULES.md`
- `docs/COMMANDS.md`
- `docs/adr/006-kma-vilage-forecast-weather-provider.md`
- `docs/adr/007-mvp2-user-location-and-react-frontend.md`
- `phases/2-smartcloset-location-frontend/step0.md`
- `phases/2-smartcloset-location-frontend/step1.md`
- `src/main/java/com/smartcloset/weather/application/WeatherProvider.java`
- `src/main/java/com/smartcloset/weather/infrastructure/kma/**`
- `src/main/java/com/smartcloset/recommendation/application/RecommendationService.java`
- step 1에서 변경된 위치 API와 사용자 위치 서비스 코드

이전 단계에서 만들어진 코드를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업
추천 생성 경로가 사용자별 위치의 `locationNx`, `locationNy`를 사용해 KMA `getVilageFcst`를 호출하도록 weather provider와 추천 서비스 연결을 변경한다.

## 변경 예상 파일
- `src/main/java/com/smartcloset/weather/application/WeatherProvider.java`
- `src/main/java/com/smartcloset/weather/infrastructure/kma/**`
- `src/main/java/com/smartcloset/weather/infrastructure/StaticWeatherProvider.java`
- `src/main/java/com/smartcloset/recommendation/application/RecommendationService.java`
- `src/main/java/com/smartcloset/user/application/**`
- `src/test/java/com/smartcloset/weather/**`
- `src/test/java/com/smartcloset/recommendation/**`

## 구현 메모
- `RecommendationService`는 계속 `WeatherProvider` 인터페이스에만 의존한다.
- 2차 기준 weather 호출은 `userId`를 포함해야 한다.
  - 예: `WeatherProvider#getCurrentWeather(Long userId)`
- KMA client 내부 인터페이스는 명시적으로 grid를 받는다.

```java
public record KmaGrid(int nx, int ny) {
}

public interface KmaForecastClient {
    List<KmaForecastItem> getVilageForecast(KmaForecastBaseTime baseTime, KmaGrid grid);
}
```

- `KmaVilageForecastClient`는 URI 생성 시 `KmaWeatherProperties#nx`, `KmaWeatherProperties#ny`가 아니라 전달받은 `KmaGrid`를 사용한다.
- 사용자 위치가 비어 있으면 추천 생성 전에 서울 기본값으로 backfill하고 그 값을 KMA 요청에 사용한다.
- `KMA_NX`, `KMA_NY`는 기존 구현/로컬 기본값 호환용으로 남길 수 있지만 2차 추천 경로의 source of truth로 사용하지 않는다.
- 추천 응답 `weather`에는 KMA 원본 응답, source, `nx`, `ny`를 추가하지 않는다.
- `WEATHER_FALLBACK_ENABLED=true`와 strict mode 정책은 1.5차와 동일하게 유지한다.
- 추천 생성에서는 위치 snapshot 확보와 필요한 backfill 저장을 짧은 DB 작업으로 끝내고, KMA 외부 호출을 긴 DB transaction 안에 두지 않는다. 추천 결과 저장은 KMA 호출 이후 별도 write transaction으로 처리하는 방향을 우선한다.

## 검증 절차
```bash
git diff --check
! rg -n 'GET /api/recommendations/(today)' . --glob '!archive/**'
./gradlew test
./gradlew build
```

## 인수 기준
- 위치를 부산으로 변경한 뒤 추천 생성 시 KMA 요청의 `nx`, `ny`가 `98`, `76`이다.
- 위치가 비어 있는 사용자로 추천 생성 시 서울 기본값이 backfill되고 KMA 요청의 `nx`, `ny`가 `60`, `127`이다.
- 위치 backfill이 필요한 추천 생성에서도 KMA 외부 호출 동안 불필요하게 DB transaction을 길게 유지하지 않는다.
- `KMA_SERVICE_KEY`가 비어 있고 fallback이 켜져 있으면 추천 생성이 fallback 날씨로 성공한다.
- strict KMA mode에서 KMA 실패 시 기존 정책대로 `INTERNAL_SERVER_ERROR`가 되며 `RecommendationResult`를 저장하지 않는다.
- 기존 추천 실패 코드 5종과 점수 계산 테스트가 계속 통과한다.
- `POST /api/recommendations?userId={userId}` 공개 계약은 변경되지 않는다.

## 금지사항
- `RecommendationService`에서 KMA client를 직접 호출하지 마라. 이유: 추천 서비스는 `WeatherProvider` 추상화에만 의존해야 한다.
- 추천 도메인 모델이 KMA 응답 DTO에 의존하게 만들지 마라. 이유: 외부 API 모델은 infrastructure 밖으로 새면 안 된다.
- 추천 결과 DB와 API 응답에 위치 code, `nx`, `ny` snapshot을 추가하지 마라. 이유: 2차 제외 범위다.
- `KMA_NX`, `KMA_NY`를 사용자별 추천의 source of truth로 유지하지 마라. 이유: 2차 핵심은 사용자 위치 기반 KMA 요청이다.
- KMA 외부 호출을 열린 DB transaction 안에서 오래 수행하지 마라. 이유: 외부 API 지연이 DB transaction과 lock 유지 시간으로 전파될 수 있다.
- fallback 실패를 추천 실패 코드 5종과 섞지 마라. 이유: 추천 실패와 외부 provider 실패의 의미가 다르다.

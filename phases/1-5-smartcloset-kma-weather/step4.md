# 단계 4: recommendation-api-kma-integration

범위: Must-have / 1.5 P0

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/ARCHITECTURE.md`
- `docs/API.md`
- `docs/RECOMMENDATION_RULES.md`
- `docs/ERD.md`
- `docs/COMMANDS.md`
- `docs/adr/006-kma-vilage-forecast-weather-provider.md`
- `phases/1-5-smartcloset-kma-weather/step0.md`
- `phases/1-5-smartcloset-kma-weather/step1.md`
- `phases/1-5-smartcloset-kma-weather/step2.md`
- `phases/1-5-smartcloset-kma-weather/step3.md`
- `src/main/java/com/smartcloset/recommendation/application/RecommendationService.java`
- `src/main/java/com/smartcloset/recommendation/presentation/RecommendationController.java`
- `src/test/java/com/smartcloset/recommendation/RecommendationControllerTest.java`

이전 단계에서 만들어진 provider wiring을 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업
추천 생성 API가 KMA 기반 weather snapshot과 fallback/strict mode를 문서 계약대로 처리하는지 통합 테스트로 잠근다. 필요한 경우 테스트 가능성을 높이는 작은 조정만 한다.

## 변경 예상 파일
- `src/test/java/com/smartcloset/recommendation/**`
- `src/test/java/com/smartcloset/weather/infrastructure/kma/**`
- 필요 시 `src/main/java/com/smartcloset/weather/infrastructure/kma/**`
- 필요 시 `src/main/java/com/smartcloset/recommendation/application/RecommendationService.java`

## 구현 메모
- 공개 추천 생성 API는 그대로 유지한다.
  - `POST /api/recommendations?userId={userId}`
  - 성공 시 `201 Created`
  - 응답은 `{ "data": ... }`
- 성공 응답의 `weather`는 KMA 기반 또는 fallback 기반 내부 snapshot이다.
- KMA 정상 응답 통합 테스트:
  - fake client 또는 mock server로 `TMP`, `SKY`, `PTY`, `PCP`, `WSD`를 포함한 JSON/item을 제공한다.
  - 추천 응답 `weather.temperature`, `weather.weatherType`, `weather.rainy`, `weather.windy`가 KMA 매핑 결과를 반영하는지 확인한다.
  - 저장된 `RecommendationResult`의 weather snapshot도 같은 값을 갖는지 확인한다.
- fallback 통합 테스트:
  - 서비스키 미설정 또는 KMA 실패에서 `WEATHER_FALLBACK_ENABLED=true`이면 추천 생성이 성공하고 fallback weather snapshot이 저장된다.
- strict mode 통합 테스트:
  - `WEATHER_FALLBACK_ENABLED=false`에서 KMA 실패가 `500 INTERNAL_SERVER_ERROR`로 응답하는지 확인한다.
  - strict mode 실패 시 `RecommendationResult`가 새로 저장되지 않는지 확인한다.
- 기존 추천 실패 코드 5종 테스트는 유지한다.
- 기존 착용 완료 API와 idempotency 테스트는 유지한다.

## 검증 절차
```bash
git diff --check
! rg -n 'GET /api/recommendations/(today)' . --glob '!archive/**'
./gradlew test
./gradlew build
```

## 인수 기준
- KMA 정상 응답이 추천 API의 weather snapshot과 저장 결과에 반영된다.
- fallback enabled에서 KMA 실패 또는 서비스키 미설정이 추천 성공으로 이어진다.
- strict mode에서 KMA 실패가 `INTERNAL_SERVER_ERROR`와 추천 결과 미저장으로 이어진다.
- 추천 도메인의 100점 점수 구조와 실패 코드 5종은 변경되지 않는다.
- today 추천 GET route나 문서상 API 계약이 생기지 않는다.

## 금지사항
- 추천 생성 API 경로 또는 HTTP method를 바꾸지 마라. 이유: 1.5차는 공개 SmartCloset API를 추가하지 않는다.
- KMA DTO를 recommendation domain package로 넘기지 마라. 이유: 추천 도메인은 내부 `WeatherCondition`만 받아야 한다.
- strict mode 실패 후 `RecommendationResult`를 저장하지 마라. 이유: 날씨 조회 실패는 추천 결과가 생성되지 않은 상태다.
- 기존 5종 추천 실패 코드를 변경하지 마라. 이유: 후보 부족 실패 계약은 1차 MVP와 동일하게 유지한다.

# 단계 4: recommendation-weather-snapshot

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/API.md`
- `docs/RECOMMENDATION_RULES.md`
- `docs/ARCHITECTURE.md`
- `docs/ERD.md`
- `docs/COMMANDS.md`
- `src/main/java/com/smartcloset/recommendation/**`
- `src/main/java/com/smartcloset/weather/**`
- Step 3에서 만든 `WeatherSnapshot`

## 작업

- `RecommendationRequest`에 optional `forecastPeriod`를 추가한다.
- body 없음 또는 `forecastPeriod` 누락 시 `CURRENT`를 사용한다.
- `RecommendationResult`에 `forecastPeriod`와 위치/날씨 source snapshot 컬럼을 추가한다.
- 추천 생성 시 Step 3의 `WeatherSnapshot`을 사용해 추천 점수와 snapshot 저장을 함께 처리한다.
- `RecommendationResponse`와 추천 이력 응답에 `forecastPeriod`, `weather.location`, `weather.source`를 포함한다.
- 사용자 위치 변경 후에도 과거 추천 snapshot이 바뀌지 않는 테스트를 추가한다.
- MVP6 피드백/착용/styleTags/personalization 계약을 유지한다.

## 인수 기준

```bash
./gradlew test --tests '*Recommendation*'
./gradlew test
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. body 없는 추천 생성이 `CASUAL`, `CURRENT`로 성공하는지 확인한다.
3. 추천 이력의 weather snapshot이 현재 사용자 위치가 아니라 추천 row snapshot에서 나오는지 확인한다.
4. 결과에 따라 `phases/7-smartcloset-location-weather-trust/index.json`의 해당 단계를 업데이트한다.

## 금지사항

- today 추천 GET endpoint를 추가하지 마라. 이유: 추천 생성은 계속 `POST /api/recommendations`다.
- 위치/source snapshot을 추천 점수 새 항목으로 만들지 마라. 이유: 총점 100점 세부 구조를 유지한다.
- 피드백 이벤트 로그 테이블을 만들지 마라. 이유: MVP6 snapshot 정책을 유지한다.

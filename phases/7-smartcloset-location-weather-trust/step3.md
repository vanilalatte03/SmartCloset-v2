# 단계 3: weather-source-snapshot

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/API.md`
- `docs/RECOMMENDATION_RULES.md`
- `docs/ARCHITECTURE.md`
- `docs/COMMANDS.md`
- `src/main/java/com/smartcloset/weather/**`
- `src/main/java/com/smartcloset/user/application/UserLocationReader.java`
- Step 2에서 수정한 위치 source 파일

## 작업

- `ForecastPeriod` enum을 추가한다: `CURRENT`, `MORNING`, `AFTERNOON`, `EVENING`.
- weather application 계약을 `WeatherCondition` 단독 반환에서 `WeatherSnapshot` 반환으로 확장한다.
- `WeatherSnapshot`은 condition, location snapshot, source metadata를 포함한다.
- `KmaWeatherConditionMapper`가 `forecastPeriod`에 맞는 forecast group을 선택하게 한다.
- KMA 성공/fallback 시 `provider`, `kmaUsed`, `fallbackUsed`, `baseDate`, `baseTime`, `forecastDate`, `forecastTime`을 채운다.
- `GET /api/weather/current`가 MVP7 `WeatherResponse`를 반환하도록 갱신한다.
- raw KMA 응답 JSON은 DTO나 DB 모델로 전달하지 않는다.

## 인수 기준

```bash
./gradlew test --tests '*Weather*'
./gradlew test
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. KMA 성공과 fallback 테스트에서 source metadata가 기대값인지 확인한다.
3. `GET /api/weather/current`가 추천 결과를 생성하지 않는지 확인한다.
4. 결과에 따라 `phases/7-smartcloset-location-weather-trust/index.json`의 해당 단계를 업데이트한다.

## 금지사항

- KMA `getVilageFcst` 외 weather API를 추가하지 마라. 이유: MVP7 외부 weather 의존성은 확장하지 않는다.
- raw KMA 응답 JSON을 저장하거나 응답하지 마라. 이유: source snapshot은 신뢰 필드만 포함한다.
- 추천 결과 DB 저장을 이 단계에서 추가하지 마라. 이유: Step 4 범위다.

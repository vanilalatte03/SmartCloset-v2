# 단계 4: recommendation-domain

범위: Must-have / P0

## 읽어야 할 파일
- `docs/RECOMMENDATION_RULES.md`
- `docs/ARCHITECTURE.md`
- `docs/API.md`
- `docs/ERD.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `phases/1-smartcloset-mvp/step2.md`

## 작업
추천 후보 필터링, 조합 생성, 점수 계산, 추천 이유 생성을 순수 도메인 서비스로 구현하고 단위 테스트를 작성한다.

## 변경 예상 파일
- `src/main/java/com/smartcloset/weather/domain/**`
- `src/main/java/com/smartcloset/weather/application/**`
- `src/main/java/com/smartcloset/weather/infrastructure/**`
- `src/main/java/com/smartcloset/recommendation/domain/**`
- `src/test/java/com/smartcloset/recommendation/domain/**`

## 구현 메모
- `WeatherProvider#getCurrentWeather(Long userId)` 인터페이스를 사용한다.
- 구현체는 `StaticWeatherProvider` 하나이며 기본값은 `temperature=12`, `weatherType=CLOUDY`, `rainy=false`, `windy=false`다.
- 최소 도메인 서비스는 `WeatherSuitabilityFilter`, `OutfitCandidateGenerator`, `RecommendationScorer`, `RecommendationReasonGenerator`다.
- `OutfitCandidate`는 DB Entity가 아니라 계산용 도메인 모델 또는 value object다.
- 점수는 `weatherScore=35`, `colorScore=25`, `wearHistoryScore=20`, `recommendationHistoryScore=10`, `diversityScore=10` 기준으로 구현한다.
- 추천 이유는 3개 이상 5개 이하로 생성한다.
- 실패 코드 5종과 tie-break는 `docs/RECOMMENDATION_RULES.md` 기준을 따른다.

## 검증 절차
```bash
./gradlew test --tests "*Recommendation*"
./gradlew test
```

## 인수 기준
- 추천 점수 계산 단위 테스트가 있다.
- 날씨 필터링 테스트가 있다.
- 색상, material, 온도 규칙 테스트가 있다.
- 실패 코드 5종 테스트가 있다.
- 동일 입력에서 동일 추천 결과를 반환하는 테스트가 있다.

## 금지사항
- 추천 계산을 Controller나 Repository에 넣지 마라. 이유: 도메인 로직은 순수 Java로 테스트 가능해야 한다.
- 외부 Weather API를 호출하지 마라. 이유: 1차 MVP는 StaticWeatherProvider만 사용한다.

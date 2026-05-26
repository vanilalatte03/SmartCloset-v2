# 단계 4: personalized-scoring-reasons

## 읽어야 할 파일

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/RECOMMENDATION_RULES.md`
- `docs/API.md`
- `docs/ERD.md`
- `docs/ARCHITECTURE.md`
- `src/main/java/com/smartcloset/recommendation/domain/RecommendationScorer.java`
- `src/main/java/com/smartcloset/recommendation/domain/RecommendationReasonGenerator.java`
- `src/main/java/com/smartcloset/recommendation/domain/RecommendationHistorySnapshot.java`
- `src/main/java/com/smartcloset/recommendation/application/RecommendationService.java`

## 작업

MVP6 개인화 점수와 추천 이유를 구현한다.

- `preferenceScore = clamp(color 0/2 + material 0/2 + styleTag 0..3 + feedbackAdjustment -3..3, 0, 10)`로 계산한다.
- 사용자 선호 `styleTags`와 옷별 `styleTags` 교집합이면 styleTagScore 2점을 부여한다.
- 선택 상황의 matching styleTags와 옷별 `styleTags` 교집합이면 styleTagScore 1점을 부여한다.
- 최근 14일 feedback snapshot만 점수에 반영한다.
- `LIKED`, `DISLIKED`, `TOO_COLD`, `TOO_HOT` 보정 기준은 `docs/RECOMMENDATION_RULES.md`를 그대로 따른다.
- 긍정/부정 signal 충돌 시 부정 signal을 우선한다.
- 여러 부정 signal은 가장 강한 감점을 사용한다.
- 추천 이유에 상황/styleTags/최근 피드백 반영 문구를 추가한다.
- 기존 `changingOnlyStyleTagsDoesNotChangeRecommendationScoreOrReasons` 테스트는 MVP6 기준에 맞게 styleTags가 점수와 이유에 반영됨을 검증하도록 이름과 기대값을 갱신한다.
- domain test를 집중 추가한다.

## 인수 기준

```bash
./gradlew test --tests '*RecommendationScorerTest'
./gradlew test --tests '*RecommendationReasonGeneratorTest'
./gradlew test --tests '*Recommendation*'
git diff --check
```

## 검증 절차

1. 색상/소재 점수 최대값이 각각 2점인지 확인한다.
2. 사용자 styleTags와 상황 styleTags 점수가 각각 반영되는지 확인한다.
3. 최근 14일 밖의 피드백은 무시되는지 확인한다.
4. `LIKED`, `DISLIKED`, `TOO_COLD`, `TOO_HOT` 보정 기준을 확인한다.
5. 부정 signal 우선순위와 clamp를 확인한다.
6. 이미지 metadata가 점수와 이유에 영향을 주지 않는지 확인한다.

## 금지사항

- AI/GPT 추천 문장을 생성하지 마라. 이유: 추천 이유는 template 기반이다.
- 점수 계산 로직을 Controller나 Repository로 옮기지 마라. 이유: 추천 도메인 테스트 가능성을 유지해야 한다.
- weather provider 동작을 바꾸지 마라. 이유: MVP6 범위는 개인화 점수다.

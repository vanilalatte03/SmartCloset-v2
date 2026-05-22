# 단계 5: preference-score-rules

범위: Must-have / 3차 P0

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/API.md`
- `docs/ARCHITECTURE.md`
- `docs/ERD.md`
- `docs/RECOMMENDATION_RULES.md`
- `docs/COMMANDS.md`
- `src/main/java/com/smartcloset/recommendation/domain/**`
- `src/main/java/com/smartcloset/recommendation/application/**`
- `src/main/java/com/smartcloset/recommendation/dto/**`
- `src/main/java/com/smartcloset/user/**`
- `src/test/java/com/smartcloset/recommendation/**`

이전 단계에서 만들어진 코드를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업
기존 다양성 점수를 `preferenceScore`로 교체하고 추천 점수, 저장 snapshot, 응답 DTO, 추천 이유, deterministic tie-break를 3차 추천 규칙에 맞춘다. 추천 API의 인증 사용자 전환은 다음 step에서 수행하므로, 이 단계는 추천 규칙과 score model만 다룬다.

## 변경 예상 파일
- `src/main/java/com/smartcloset/recommendation/domain/RecommendationScorer.java`
- `src/main/java/com/smartcloset/recommendation/domain/RecommendationReasonGenerator.java`
- `src/main/java/com/smartcloset/recommendation/domain/OutfitCandidate.java`
- `src/main/java/com/smartcloset/recommendation/domain/RecommendationResult.java`
- `src/main/java/com/smartcloset/recommendation/dto/**`
- `src/main/java/com/smartcloset/recommendation/application/**`
- `src/main/java/com/smartcloset/user/**`
- `src/test/java/com/smartcloset/recommendation/**`

## 구현 메모
- 총점은 100점이다.
- 점수 항목:
  - `weatherScore` 최대 35
  - `colorScore` 최대 25
  - `wearHistoryScore` 최대 20
  - `recommendationHistoryScore` 최대 10
  - `preferenceScore` 최대 10
- `preferenceScore` 계산:
  - 선호 색상/소재가 모두 비어 있으면 0점
  - 후보 옷 중 하나 이상이 `preferredColors`와 일치하면 5점
  - 후보 옷 중 하나 이상이 `preferredMaterials`와 일치하면 5점
  - 둘 다 일치하면 10점
- `styleTags`는 score, tie-breaker, candidate generation, filter, recommendation reason에 영향을 주면 안 된다.
- 추천 이유는 template 기반이며 3개에서 5개를 생성한다.
- 선호 색상 또는 선호 소재와 맞는 옷이 포함된 경우에만 선호도 관련 이유를 생성한다.
- 기존 다양성 점수 필드명, DTO field, DB snapshot field, 테스트 기대값을 제거하거나 `preferenceScore`로 교체한다.
- tie-break 순서는 문서를 따른다.
  1. `weatherScore`
  2. `preferenceScore`
  3. `colorScore`
  4. `wearHistoryScore`
  5. `recommendationHistoryScore`
  6. TOP id
  7. BOTTOM id
  8. OUTER id 또는 날씨 정책상 자연스러운 OUTER 여부

## 검증 절차
```bash
git diff --check
rg -n 'preferenceScore' src/main/java src/test/java
! rg -n 'diversityScore|diversity_score|diversity score|다양성 점수' src/main/java src/test/java
./gradlew test
```

## 인수 기준
- 선호 색상/소재가 모두 비어 있으면 `preferenceScore=0`이다.
- 선호 색상만 일치하면 `preferenceScore=5`다.
- 선호 소재만 일치하면 `preferenceScore=5`다.
- 선호 색상과 소재가 모두 일치하면 `preferenceScore=10`이다.
- `styleTags`만 바꿔도 추천 점수와 추천 이유가 바뀌지 않는 테스트가 있다.
- 추천 응답 score에는 `preferenceScore`가 있고 기존 다양성 점수 필드는 없다.
- 추천 결과 저장 snapshot에는 `preference_score` 또는 이에 대응하는 entity field가 있다.
- 같은 input은 같은 recommendation result를 만든다.

## 금지사항
- 추천 API의 인증 사용자 전환을 이 단계에 섞지 마라. 이유: 추천 규칙 변경과 HTTP/API 전환을 분리해 리뷰 가능하게 유지한다.
- `styleTags`를 점수, tie-breaker, 후보 생성, 필터, 추천 이유에 사용하지 마라. 이유: 3차 범위 밖이다.
- AI/GPT 문장 생성으로 추천 이유를 만들지 마라. 이유: 추천 이유는 template 기반이다.
- `preferenceScore` 최대값을 10점보다 크게 만들지 마라. 이유: 총점 100점 체계가 고정되어 있다.
- 기존 다양성 점수를 별도 field로 유지하지 마라. 이유: 3차에서 `preferenceScore`로 교체하기로 했다.

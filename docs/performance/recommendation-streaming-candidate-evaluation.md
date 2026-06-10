# 추천 후보 스트리밍 평가 기록

## 문서 목적

이 문서는 추천 생성 중 가능한 코디 후보와 점수 후보를 모두 메모리에 materialize하던 구조를 streaming best 선택으로 바꾼 성능 개선 기록이다.

이 문서는 ADR이 아니며 공개 API, DB schema, 추천 점수 field, 후보 생성 순서, tie-break 규칙을 변경하지 않는다. 관련 GitHub Issue는 `#174`이고, 구현은 PR `#182`에서 merge했다.

## 문제

추천 생성은 사용자의 활성 옷장을 날씨 조건으로 필터링한 뒤 가능한 상의/하의/아우터 조합을 만든다.

기존 `RecommendationService` 흐름은 다음과 같았다.

1. `OutfitCandidateGenerator.generate(...)`가 가능한 모든 `OutfitCandidate`를 `List`로 만든다.
2. `RecommendationScorer.scoreAll(...)`이 모든 후보를 다시 `ScoredOutfitCandidate` `List`로 변환한다.
3. `RecommendationScorer.selectBest(...)`가 전체 점수 후보 목록에서 최종 best를 고른다.

상의 `T`, 하의 `B`, 아우터 `O`가 날씨 필터를 통과하면 후보 수는 날씨 정책에 따라 `T * B * (1 + O)` 또는 `T * B * O`까지 커질 수 있다. 큰 옷장에서는 후보 객체 목록과 점수 객체 목록이 같은 요청 안에서 동시에 커지고, 추천 저장 transaction 동안 불필요한 메모리와 GC pressure를 만든다.

## 변경

`OutfitCandidateGenerator`에 후보를 생성 순서대로 하나씩 전달하는 visitor 형태의 `forEach(...)`를 추가했다.

기존 `generate(...)`는 테스트와 기존 도메인 호출 호환을 위해 남겼고, 내부에서 새 `forEach(...)`를 사용해 동일한 생성 순서를 유지한다.

추천 생성 service path는 다음 흐름으로 바뀌었다.

1. 날씨 필터로 `WeatherFilteredClothes`를 만든다.
2. `OutfitCandidateGenerator.forEach(...)`로 후보를 하나씩 받는다.
3. 후보가 생성되는 즉시 `RecommendationScorer.score(...)`로 점수화한다.
4. `RecommendationScorer.betterOf(...)`로 기존 tie-break comparator를 재사용해 현재 best만 갱신한다.
5. 최종 best 후보로 추천 이유와 snapshot을 저장한다.

## 보존한 계약

- 추천 생성 API `POST /api/recommendations` 계약을 변경하지 않는다.
- 추천 response score field를 변경하지 않는다.
- `weatherScore`, `colorScore`, `wearHistoryScore`, `recommendationHistoryScore`, `preferenceScore` 계산식을 변경하지 않는다.
- 후보 생성 순서를 변경하지 않는다.
- `12°C` 이하 아우터 필수, `13°C..18°C` 아우터 선호, `19°C` 이상 상하의-only 선호 정책을 변경하지 않는다.
- 동일 점수 tie-break에서 score field, 옷 id, 아우터 유무, `generationOrder` 순서를 유지한다.
- 이미지 metadata, AI 분석 confidence, reviewRequiredFields를 추천 후보 필터링, 점수, tie-break, 추천 이유에 사용하지 않는다.
- 사용자 옷장 크기 제한과 후보 예산 정책은 추가하지 않는다. 후보 예산은 별도 Issue `#175` 범위다.

## 성능 영향

추천 생성은 더 이상 전체 `OutfitCandidate` 목록과 전체 `ScoredOutfitCandidate` 목록을 동시에 보관하지 않는다.

요청 중 메모리에 유지되는 추천 후보 관련 상태는 현재 순회 중인 후보, 그 후보의 점수, 현재 best 후보로 줄었다. 따라서 후보 수가 커져도 후보/점수 목록 보관 메모리는 후보 수에 비례해 증가하지 않는다.

이 변경은 메모리 사용량과 GC pressure를 줄이는 개선이다. 모든 후보를 여전히 평가하므로 CPU 시간은 후보 수에 비례할 수 있다. 대형 옷장의 CPU 상한과 candidate pool pruning 정책은 후보 예산 정책 Issue `#175`에서 별도로 다룬다.

## 회귀 기준

추천 후보 평가에서는 다음 기준을 지킨다.

- service path에서 전체 후보 `List<OutfitCandidate>`와 전체 점수 `List<ScoredOutfitCandidate>`를 함께 만들지 않는다.
- 기존 `generate(...) -> scoreAll(...) -> selectBest(...)` 경로와 streaming best 선택 경로는 같은 입력에서 같은 best 후보와 같은 score를 반환해야 한다.
- 아우터 필수, 아우터 선택, 더운 날씨 정책별 결과가 기존과 같아야 한다.
- 후보가 하나도 생성되지 않으면 기존처럼 `INSUFFICIENT_CLOSET_ITEMS` 계열 business failure로 수렴해야 한다.
- 새 최적화가 추천 규칙 문서의 score/tie-break 계약을 바꾸는 방식으로 확장되면 안 된다.

## 검증

PR `#182`에서 다음 검증을 통과했다.

- `git diff --check`
- `git diff --check origin/main...HEAD`
- `./gradlew test --tests com.smartcloset.recommendation.domain.RecommendationOutfitCandidateGeneratorTest --tests com.smartcloset.recommendation.domain.RecommendationScorerTest`
- `./gradlew test`
- 커밋 훅: `python3 -m compileall scripts`
- 커밋 훅: `./gradlew build`
- 커밋 훅: `cd frontend && npm run build`
- Codex CLI read-only review: `pass=true`, findings 없음

추가된 테스트는 visitor 후보 순회가 기존 generation order를 보존하는지 확인하고, `12°C`, `18°C`, `25°C` 날씨 입력에서 streaming best 선택 결과가 기존 materialized best 선택 결과와 같은 후보/score를 반환하는지 비교한다.

# 추천 후보 예산 정책 기록

## 문서 목적

이 문서는 대형 옷장에서 추천 생성 CPU 시간과 조합 수를 예측 가능하게 만들기 위해 도입한 category별 추천 계산 후보 예산 정책의 성능 개선 기록이다.

이 문서는 ADR이 아니며 공개 API, DB schema, 사용자 옷장 저장 개수, 추천 score field, 최종 scoring/tie-break comparator를 변경하지 않는다. 관련 GitHub Issue는 `#175`이고, 구현은 PR `#183`에서 merge했다.

## 문제

추천 생성은 날씨 필터를 통과한 상의, 하의, 아우터를 가능한 조합으로 만든 뒤 각 후보를 점수화한다.

Issue `#174`에서 전체 후보와 점수 후보 목록을 materialize하지 않는 streaming best 선택으로 메모리 사용량은 줄였지만, 평가해야 하는 조합 수 자체는 그대로 남았다. 날씨 필터 이후 상의 `T`, 하의 `B`, 아우터 `O`가 남으면 후보 수는 날씨 정책에 따라 다음과 같이 커질 수 있다.

- 아우터가 필수가 아닌 날씨: `T * B * (1 + O)`
- 아우터가 필수인 날씨: `T * B * O`

사용자 옷장이 커질수록 단일 추천 요청의 CPU 시간과 API 지연이 조합 수에 비례해 증가할 수 있다.

## 변경

`RecommendationCandidateBudgeter`를 추가해 날씨 필터 이후, 후보 조합 생성 이전에 category별 계산 후보 pool을 제한한다.

추천 생성 service path는 다음 흐름이다.

1. 사용자의 active clothes를 id 오름차순으로 조회한다.
2. `WeatherSuitabilityFilter`가 날씨 조건에 맞는 상의, 하의, 아우터 pool을 만든다.
3. `RecommendationCandidateBudgeter`가 category별 pool을 최대 32개로 줄인다.
4. `OutfitCandidateGenerator.forEach(...)`가 budgeted pool에서 후보를 streaming으로 생성한다.
5. `RecommendationScorer.score(...)`와 `RecommendationScorer.betterOf(...)`가 기존 점수와 tie-break 기준으로 best를 고른다.

## 후보 선정 기준

category별 후보가 32개 이하이면 기존 날씨 필터 결과 순서를 그대로 유지한다.

category별 후보가 32개를 초과하면 다음 deterministic 기준으로 32개를 선정한다.

- 현재 기온이 옷의 적정 온도 범위 중앙에 가까운지
- 비 또는 더운/추운 날씨에서 소재가 적합한지
- 선호 색상, 선호 소재, 선호 styleTags와 맞는지
- 요청 상황의 styleTags와 맞는지
- 최근 착용 이력 또는 최근 추천 이력에 포함되어 반복 노출 위험이 있는지
- 동점이면 옷 id 오름차순으로 안정적으로 고르는지

선정된 32개는 다시 id 오름차순으로 정렬한다. 따라서 이후 후보 생성 순서는 기존 `WeatherSuitabilityFilter`의 id 기반 순서를 유지한다.

## 보존한 계약

- 사용자 옷장에 저장 가능한 옷 개수는 제한하지 않는다.
- 추천 API request/response 계약을 변경하지 않는다.
- DB schema와 repository contract를 변경하지 않는다.
- 최종 추천 점수 field와 배점을 변경하지 않는다.
- 최종 best 선택의 score/tie-break comparator를 변경하지 않는다.
- 추천 이유 template을 변경하지 않는다.
- `12°C` 이하 아우터 필수, `13°C..18°C` 아우터 선호, `19°C` 이상 상하의-only 선호 정책을 변경하지 않는다.
- 이미지 metadata, AI 분석 결과, confidence, reviewRequiredFields를 후보 pool 선정, 후보 필터링, 점수, tie-break, 추천 이유에 사용하지 않는다.

## 성능 영향

후보 예산 적용 뒤 추천 계산 조합 수는 category별 최대 32개 기준으로 제한된다.

- 아우터가 필수가 아닌 날씨의 최대 후보 수: `32 * 32 * (1 + 32) = 33,792`
- 아우터가 필수인 날씨의 최대 후보 수: `32 * 32 * 32 = 32,768`

이 상한은 저장된 전체 옷장 크기가 아니라 날씨 필터 이후 추천 계산에 참여하는 category별 pool에만 적용된다. 큰 옷장에서도 단일 추천 요청에서 평가하는 후보 조합 수가 예측 가능한 범위로 고정된다.

후보 pool이 상한을 넘는 경우 전체 exhaustive search와 다른 추천 결과가 나올 수 있다. 이 차이는 성능 정책으로 허용되며, 같은 입력에서는 항상 같은 후보 pool과 같은 추천 결과를 반환해야 한다.

## 회귀 기준

추천 후보 예산 정책에서는 다음 기준을 지킨다.

- 날씨 필터 이전에 후보 예산을 적용하지 않는다.
- category별 후보가 32개 이하이면 기존 순서를 바꾸지 않는다.
- category별 후보가 32개를 초과하면 deterministic 기준으로 32개를 고르고, 이후 생성 순서는 id 오름차순으로 유지한다.
- budgeter가 추천 score field나 최종 tie-break comparator를 대체하면 안 된다.
- budgeter가 이미지 metadata, AI 분석 confidence, reviewRequiredFields를 읽거나 반영하면 안 된다.
- 후보 예산은 저장 제한, pagination, 삭제, archive 정책으로 확장하면 안 된다.
- 예산 적용 후에도 상의/하의/아우터 필수 조건 실패는 기존 business failure로 수렴해야 한다.

## 검증

PR `#183`에서 다음 검증을 통과했다.

- `git diff --check`
- `git diff --check origin/main...HEAD`
- `./gradlew test --tests com.smartcloset.recommendation.domain.RecommendationCandidateBudgeterTest --tests com.smartcloset.recommendation.domain.RecommendationOutfitCandidateGeneratorTest --tests com.smartcloset.recommendation.domain.RecommendationScorerTest`
- `./gradlew test`
- `python3 scripts/checks.py --docs-check --include-final-docs`
- 커밋 훅: `python3 -m compileall scripts`
- 커밋 훅: `./gradlew build`
- 커밋 훅: `cd frontend && npm run build`
- Codex CLI read-only review: `pass=true`, findings 없음

추가된 테스트는 category별 32개 상한, deterministic 선정, id 오름차순 생성 순서 보존, image metadata 비개입, 최근 착용/추천 이력 페널티, 대형 옷장 조합 수 상한을 확인한다.

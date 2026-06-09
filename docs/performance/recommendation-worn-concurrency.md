# 추천 착용 완료 동시성 기록

이 문서는 같은 추천 결과에 동시에 착용 완료 요청이 들어올 때 멱등 API가 DB unique constraint 예외로 실패할 수 있던 문제와 개선 내용을 정리한다.

이 문서는 ADR이 아니며 공개 API, DB schema, 추천 점수, 추천 이유 규칙을 변경하지 않는다. 관련 GitHub Issue는 `#157`이다.

## 문제

`PATCH /api/recommendations/{recommendationId}/worn`은 같은 추천을 여러 번 착용 완료 처리해도 기존 `WearHistory`를 재사용하는 멱등 API다.

기존 흐름은 recommendation result를 조회한 뒤 `wear_histories.recommendation_result_id`로 기존 착용 이력을 조회하고, 없으면 새 `WearHistory`를 저장했다. 이 순차 흐름은 이미 착용 완료된 추천에는 맞지만, 같은 recommendation id로 두 요청이 동시에 들어오면 두 transaction이 모두 기존 이력이 없다고 판단할 수 있었다.

DB에는 recommendation result당 착용 이력을 하나만 허용하는 unique constraint가 있으므로, 경합에서 늦은 insert는 constraint violation으로 실패할 수 있다. 이 실패가 API 500 또는 DB 예외로 노출되면 idempotent API 계약이 깨진다.

## 변경

`RecommendationService.markWorn`은 현재 사용자 소유 recommendation result를 `RecommendationResultRepository.findByIdAndUserIdForWorn`으로 조회한다.

이 조회는 `PESSIMISTIC_WRITE` lock을 사용한다. 같은 recommendation id의 착용 완료 요청은 recommendation result row에서 직렬화된다.

착용 완료 흐름은 다음 순서를 유지한다.

1. 현재 사용자 소유 recommendation result row를 write lock으로 조회한다.
2. 기존 `WearHistory`가 있으면 recommendation result를 `worn=true`로 유지하고 기존 `wornAt`을 반환한다.
3. 기존 `WearHistory`가 없으면 recommendation result를 `worn=true`로 변경한다.
4. 새 `WearHistory`를 저장하고 저장된 `wornAt`을 반환한다.
   이때 신규 `wornAt`은 DB `DATETIME(6)` precision과 맞도록 microsecond 단위로 맞춘다.
5. 같은 recommendation id로 대기하던 concurrent 요청은 첫 transaction commit 이후 기존 `WearHistory`를 조회하고 같은 `wornAt`으로 성공 응답을 반환한다.

## 성능 영향

lock 범위는 단일 `recommendation_results.id` row다.

착용 완료는 사용자 수동 액션이며, 같은 recommendation id에 대한 중복 요청은 더블클릭, retry, 네트워크 중복 전송 같은 짧은 경합 상황에서만 발생한다. 이 경우 직렬화 비용은 작고, unique constraint 예외를 API 실패로 노출하지 않기 위한 의도적인 비용이다.

추천 생성, 추천 이력 조회, 추천 점수 계산, 추천 피드백 교체에는 새 lock 조회를 사용하지 않는다. 추천 점수와 tie-break 규칙도 변경하지 않는다.

## 회귀 기준

추천 착용 완료 처리에서는 다음 기준을 지킨다.

- 같은 recommendation id에 동시 착용 완료 요청을 보내도 모든 요청은 성공 응답을 반환한다.
- 모든 성공 응답은 같은 저장 `WearHistory.wornAt`을 반환한다.
- DB에는 recommendation result당 `WearHistory`가 정확히 1개만 남는다.
- recommendation result의 `worn` 상태는 `true`가 된다.
- 다른 사용자 추천에 대한 착용 완료 요청은 기존처럼 `RECOMMENDATION_NOT_FOUND`로 실패한다.
- 추천 점수, 추천 이유, 후보 생성 규칙은 변경하지 않는다.

## 검증

`RecommendationWornConcurrencyTest`는 다음 시나리오를 검증한다.

- 추천 결과 하나를 생성한다.
- 같은 recommendation id로 두 `PATCH /api/recommendations/{recommendationId}/worn` 요청을 동시에 시작한다.
- 두 응답이 모두 `200 OK`, `worn=true`, 같은 `wornAt`인지 확인한다.
- 해당 recommendation result의 `WearHistory`가 1개만 저장되는지 확인한다.
- recommendation result snapshot의 `worn` 값이 true인지 확인한다.

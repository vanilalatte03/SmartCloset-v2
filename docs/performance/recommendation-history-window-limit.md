# 추천 계산 이력 조회 상한 기록

## 문서 목적

이 문서는 추천 생성 내부 scoring 입력으로 사용하는 착용/추천/피드백 이력 조회에 기간 조건과 row 상한을 함께 적용한 성능 안정 개선 기록이다.

이 문서는 ADR이 아니며 공개 API, DB schema, 추천 score field, tie-break 규칙, feedback 계약을 변경하지 않는다. 관련 GitHub Issue는 `#178`이고, 구현은 PR `#186`에서 merge했다.

## 문제

공개 추천 이력 조회 API는 `limit` 기본값과 최대값을 가지고 있지만, 추천 생성 내부 scoring path에서 사용하는 과거 이력 조회는 기간 조건만 가지고 있었다.

구체적으로 추천 생성은 다음 이력을 점수화 입력으로 사용한다.

- 최근 7일 착용 이력
- 최근 7일 추천 이력
- 추천 이력 보강용 최신 5건
- 최근 14일 feedback 이력

이 중 최신 5건 보강 조회를 제외한 기간 기반 조회에는 row 상한이 없었다. 활동량이 많은 사용자는 같은 기간 안의 이력이 커질 수 있고, 추천 요청마다 DB 조회 결과, id 병합, batch item 조회, history snapshot 재정렬과 점수 계산 입력이 함께 커질 수 있었다.

## 변경

추천 생성 내부 scoring path의 이력 조회를 repository `Pageable` 기반으로 제한했다.

적용한 상한은 다음과 같다.

| 입력 | 기간 | 최대 건수 | 정렬 |
| --- | --- | ---: | --- |
| 착용 이력 | 최근 7일 | 50 | 착용 시각 최신순 |
| 추천 이력 | 최근 7일 | 50 | 생성 시각 최신순 |
| 추천 이력 보강 | 전체 기간 | 5 | 생성 시각 최신순 |
| feedback 이력 | 최근 14일 | 50 | feedback 수정 시각 최신순 |

`RecommendationService`는 각 query에 `PageRequest`를 넘기고, repository method는 DB 조회 단계에서 제한을 적용한다. 추천 이력 id 병합은 기존처럼 `LinkedHashSet`으로 우선순위를 보존한다.

이력 snapshot 구성에서는 기존과 같이 `IN` query 결과 순서를 신뢰하지 않고, 제한된 id 목록의 순서를 기준으로 다시 정렬한다.

## 보존한 계약

- 공개 추천 이력 API `GET /api/recommendations?limit={limit}` 계약을 변경하지 않는다.
- 공개 추천 이력 API의 기본값 20, 최소 1, 최대 50 정책을 변경하지 않는다.
- 추천 생성 API `POST /api/recommendations` request/response shape를 변경하지 않는다.
- `weatherScore`, `colorScore`, `wearHistoryScore`, `recommendationHistoryScore`, `preferenceScore` field와 배점을 변경하지 않는다.
- 최종 best 선택 tie-break comparator를 변경하지 않는다.
- feedback 저장/삭제 API와 response 계약을 변경하지 않는다.
- DB schema, entity 관계, index를 변경하지 않는다.
- 과거 테스트용 `userId` query parameter나 field를 되살리지 않는다.
- 이미지 metadata, AI 분석 confidence, `reviewRequiredFields`를 추천 후보 필터링, 점수, tie-break, 추천 이유에 사용하지 않는다.

## 성능 영향

추천 생성에서 scoring 입력으로 읽는 이력은 이제 사용자 활동량과 독립적인 상한을 가진다.

최대 입력 규모는 다음과 같이 제한된다.

- 착용 이력 snapshot 입력: 최대 50건
- 추천 이력 id 입력: 최근 7일 50건, 최신 보강 5건, 최근 feedback 50건
- 추천 이력 id 병합 전 최대치: 105건

id 병합 후 중복은 제거되므로 실제 추천 이력 snapshot과 batch item 조회 대상은 105건 이하가 된다. 이 상한은 저장된 전체 추천/착용/feedback 이력 개수와 무관하게 단일 추천 요청의 이력 기반 점수화 비용을 예측 가능한 범위로 제한한다.

상한을 넘는 사용자는 같은 기간 안의 오래된 이력이 더 이상 현재 추천 점수에 반영되지 않을 수 있다. 이는 성능 안정 정책으로 허용하며, 같은 DB 상태와 같은 요청 시각 기준에서는 deterministic하게 최신순 상한을 적용해야 한다.

## 회귀 기준

추천 계산 이력 조회 상한에서는 다음 기준을 지킨다.

- 추천 생성 내부 착용/추천/feedback 이력 조회는 repository `Pageable` 또는 동등한 DB 단계 제한을 유지한다.
- 기간 기반 이력 조회를 무제한 `List` 조회로 되돌리면 안 된다.
- 공개 추천 이력 API의 `limit` 정책과 내부 scoring 이력 상한을 섞으면 안 된다.
- `IN` query 결과 순서를 그대로 사용하지 않고, 제한된 id 목록의 순서를 기준으로 snapshot을 정렬한다.
- JPA collection fetch join과 pagination을 결합하지 않는다.
- 이력 조회 상한이 추천 score field, 배점, tie-break comparator를 변경하면 안 된다.
- 이력 조회 상한이 사용자별 옷장 저장 제한이나 history 삭제 정책으로 확장되면 안 된다.

## 검증

PR `#186`에서 다음 검증을 통과했다.

- `git diff --check`
- `python3 scripts/checks.py --docs-check --include-final-docs`
- `./gradlew test --tests com.smartcloset.recommendation.RecommendationControllerTest`
- `./gradlew test --tests com.smartcloset.recommendation.RecommendationWornConcurrencyTest`
- `./gradlew test`
- GitHub Actions: `test-build`
- 커밋 훅: `python3 -m compileall scripts`
- 커밋 훅: `./gradlew build`
- 커밋 훅: `cd frontend && npm run build`
- Codex CLI read-only review 2회: `pass=true`, findings 없음

추가된 테스트는 51번째 오래된 착용/추천 이력이 현재 후보와 겹쳐도 상한 밖 이력이 추천 점수에 반영되지 않는지 확인한다. CI에서 드러난 `RecommendationWornConcurrencyTest`의 timestamp 문자열 trailing-zero 표현 차이는 DB 값을 `LocalDateTime`으로 parse해 비교하도록 보정했으며, 응답 문자열끼리의 일관성 검증은 유지한다.

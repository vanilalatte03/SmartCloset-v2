# Refresh Token Rotation 동시성 성능 기록

## 문서 목적
이 문서는 같은 refresh token으로 동시에 rotation 요청이 들어올 때 하나의 token 재사용에서 여러 active session이 생길 수 있던 문제와 개선 내용을 정리한다.

이 문서는 ADR이 아니며 공개 API, DB schema, refresh token 원문 저장 정책을 변경하지 않는다. 관련 GitHub Issue는 `#155`다.

## 문제
Refresh API는 매 요청마다 refresh token rotation을 수행한다.

기존 흐름은 token hash로 기존 session을 조회하고, active 여부를 확인한 뒤, 새 session을 발급하고 기존 session을 revoke했다. 이 순서 자체는 단일 요청에서는 맞지만, 같은 refresh cookie로 동시 요청이 들어오면 두 transaction이 모두 기존 session을 active로 볼 수 있었다.

그 결과 두 요청이 각각 새 refresh session을 만들면 하나의 기존 refresh token 재사용에서 여러 active session이 생길 수 있다. 이는 재사용/revoke token을 `INVALID_TOKEN`으로 실패시킨다는 MVP8 계정 안정성 계약을 약하게 만든다.

## Before
기존 구조의 위험 요소는 다음과 같았다.

- `findByTokenHash` 조회에 DB row lock이 없었다.
- active 검사와 revoke 상태 전이가 하나의 원자적 구간으로 보호되지 않았다.
- 새 session 발급이 기존 session revoke보다 먼저 수행될 수 있었다.
- 경합에서 늦은 요청이 기존 session의 revoke를 관찰하지 못하면 성공할 수 있었다.

## After
개선 후 rotation 전략은 DB row lock 기준이다.

1. 요청 refresh token 원문은 HMAC hash로 변환한다.
2. 기존 session은 `PESSIMISTIC_WRITE` lock이 걸린 `findByTokenHashForRotation`으로 조회한다.
3. lock을 잡은 transaction만 active 여부를 검사한다.
4. 기존 session이 이미 revoke되었거나 만료되었으면 `INVALID_TOKEN`으로 실패한다.
5. active session이면 다음 refresh token/hash를 생성하고 기존 session에 `revokedAt`, `replacedByTokenHash`를 기록한다.
6. 기존 session의 상태 전이 이후 새 refresh session을 저장한다.
7. 같은 token으로 대기하던 concurrent 요청은 첫 transaction commit 이후 revoke 상태를 보고 `INVALID_TOKEN`으로 실패한다.

logout의 기존 session revoke도 같은 lock 조회를 사용해 refresh rotation과 같은 row를 동시에 갱신하지 않게 했다.

## 성능 영향
rotation은 단일 `refresh_sessions.token_hash` row에 대해 짧은 write lock을 잡는다.

정상적인 refresh 요청은 사용자당 드물게 발생하고 token hash unique index로 단건 조회되므로 lock 범위는 작다. 같은 refresh token을 동시에 재사용하는 경우에만 요청들이 직렬화된다. 이는 예측 불가능한 여러 active session 생성을 막기 위한 의도적인 직렬화 비용이다.

새 session 저장은 기존 session revoke 상태 전이 이후 같은 transaction 안에서 수행된다. 따라서 경합에서 승리한 요청은 하나의 새 session만 만들고, 패배한 요청은 추가 session을 만들지 않는다.

## 회귀 방지 기준
Refresh token rotation에서는 다음 기준을 지킨다.

- 같은 refresh token으로 동시 refresh를 보내도 최대 1개 요청만 성공한다.
- 패배한 동시 요청은 `INVALID_TOKEN`으로 실패한다.
- 성공 요청은 기존 session에 `revokedAt`과 `replacedByTokenHash`를 남긴다.
- token 원문은 DB나 JSON response에 저장 또는 노출하지 않는다.
- Redis나 별도 distributed lock을 추가하지 않고 DB-backed refresh session 경계 안에서 처리한다.

## 확인한 테스트
이번 변경은 `RefreshTokenConcurrencyTest`를 추가해 다음을 검증한다.

- 같은 refresh token으로 두 rotation을 동시에 시작한다.
- 성공 결과는 정확히 1개다.
- 실패 결과는 `INVALID_TOKEN`이다.
- 해당 사용자 refresh session은 기존 revoked session 1개와 새 active session 1개만 남는다.
- 기존 session의 `replacedByTokenHash`는 새 active session의 token hash와 일치한다.
- refresh token 원문은 저장된 token hash와 다르다.

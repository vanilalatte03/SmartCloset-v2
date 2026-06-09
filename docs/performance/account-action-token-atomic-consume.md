# 계정 액션 토큰 원자적 소비

이 문서는 이메일 인증 token과 비밀번호 재설정 token을 동시에 소비할 때 single-use 계약이 깨질 수 있던 문제와 개선 내용을 정리한다.

이 문서는 ADR이 아니며 공개 API, DB schema, token 원문 비저장 정책을 변경하지 않는다. 관련 GitHub Issue는 `#156`이다.

## 문제

이메일 인증 confirm과 비밀번호 재설정 confirm은 `account_action_tokens.token_hash`로 token row를 찾고, 목적/만료/사용 여부를 확인한 뒤 `used_at`을 기록한다.

기존 흐름은 조회와 사용 처리 사이에 row lock이 없었다. 같은 token으로 두 요청이 동시에 들어오면 두 transaction이 모두 `used_at is null` 상태를 관찰하고, 둘 다 성공 흐름으로 들어갈 수 있었다.

그 결과 email verification token 또는 password reset token의 single-use 계약이 약해지고, password reset confirm에서는 같은 token으로 여러 번 password update와 refresh session revoke 흐름이 실행될 수 있다.

## 변경

`AccountActionTokenService.consume`은 token hash를 계산한 뒤 `AccountActionTokenRepository.findByTokenHashForConsume`을 사용한다.

이 조회는 `PESSIMISTIC_WRITE` lock을 사용하고 user를 함께 fetch한다. 같은 token을 소비하려는 concurrent 요청은 동일한 `account_action_tokens.token_hash` row에서 직렬화된다.

소비 흐름은 다음 순서를 유지한다.

1. 요청 token 원문을 HMAC hash로 변환한다.
2. token hash row를 write lock으로 조회한다.
3. 목적, 만료, `used_at` 상태를 검증한다.
4. 유효하면 `used_at`을 기록한다.
5. email verification confirm은 같은 transaction에서 user를 인증 완료로 변경한다.
6. password reset confirm은 같은 transaction에서 password hash를 변경하고 refresh session을 revoke한다.
7. 같은 token으로 대기하던 concurrent 요청은 첫 transaction commit 이후 사용 완료 상태를 보고 `ACCOUNT_TOKEN_INVALID`로 실패한다.

## 성능 영향

lock 범위는 unique index가 있는 단일 `account_action_tokens.token_hash` row다.

정상적인 이메일 인증과 비밀번호 재설정 confirm은 사용자가 수동으로 실행하는 드문 요청이므로 추가 lock 비용은 작다. 같은 token을 동시에 재사용하는 비정상 경로에서만 요청이 직렬화되며, 이 직렬화는 single-use 보장을 위해 의도한 비용이다.

`issue`는 기존처럼 token row를 생성한다. `consume`만 잠금 조회를 사용하므로 token 발급, 로그인, refresh session rotation, 추천/옷장 흐름에는 영향을 주지 않는다.

## 회귀 기준

계정 액션 token 소비에서는 다음 기준을 지킨다.

- 같은 이메일 인증 token으로 동시 confirm을 보내도 최대 1개 요청만 성공한다.
- 같은 비밀번호 재설정 token으로 동시 confirm을 보내도 최대 1개 요청만 성공한다.
- 실패 요청은 `ACCOUNT_TOKEN_INVALID`로 응답한다.
- 성공 token은 `used_at`이 한 번 기록된다.
- token 원문은 DB, JSON response, 로그에 저장 또는 노출하지 않는다.
- 성공한 password reset은 기존 refresh session을 revoke한다.

## 검증

`AccountActionTokenConcurrencyTest`는 다음 시나리오를 검증한다.

- 같은 이메일 인증 token으로 두 confirm을 동시에 시작한다.
- 성공 1개와 `ACCOUNT_TOKEN_INVALID` 실패 1개를 확인한다.
- 사용자 이메일 인증 상태가 완료되고 token `used_at`이 기록되는지 확인한다.
- 같은 비밀번호 재설정 token으로 두 confirm을 동시에 시작한다.
- 성공 1개와 `ACCOUNT_TOKEN_INVALID` 실패 1개를 확인한다.
- password hash 변경과 기존 refresh session revoke를 확인한다.

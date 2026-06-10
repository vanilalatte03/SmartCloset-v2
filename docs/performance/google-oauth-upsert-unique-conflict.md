# Google OAuth Upsert Unique 충돌 수렴 기록

## 문서 목적

이 문서는 Google OAuth callback에서 같은 Google 계정 또는 같은 email callback이 동시에 들어올 때 user/social account unique 충돌을 서버 오류가 아니라 정상 로그인으로 수렴시킨 안정성 개선 기록이다.

이 문서는 ADR이 아니며 OAuth 공개 API shape, DB schema, refresh token 원문 저장 정책, Google verified email 검증 계약을 변경하지 않는다. 관련 GitHub Issue는 `#179`이고, 구현은 PR `#187`에서 merge했다.

## 문제

Google OAuth callback은 provider에서 verified profile을 받은 뒤 SmartCloset user, social account, refresh session을 생성하거나 연결한다.

기존 흐름은 먼저 `social_accounts(provider, provider_user_id)`를 조회하고, 없으면 같은 email user를 조회하거나 새 Google-only user를 만든 뒤 social account를 저장했다.

동시에 같은 Google profile callback이 두 번 들어오면 두 요청이 모두 조회 단계에서 기존 row를 보지 못할 수 있다. 이후 한 요청이 먼저 `users.email` 또는 `social_accounts(provider, provider_user_id)` unique constraint를 선점하면 다른 요청은 DB unique violation으로 실패할 수 있었다.

이 실패는 실제 계정 생성 또는 연결이 이미 성공한 상황에서도 다른 callback 요청을 500으로 보이게 만들어 사용자 로그인 경험과 운영 추적을 불안정하게 만든다.

## 변경

`GoogleOAuthService`는 provider profile 조회와 verified email 검증을 기존처럼 transaction 밖에서 수행한다.

검증된 profile만 write transaction으로 넘기고, 다음 구간에서 `DataIntegrityViolationException`이 발생하면 known OAuth upsert unique 충돌인지 판별한다.

- `uk_users_email`
- `uk_social_accounts_provider_user`
- DB별 메시지에 포함된 `users/email` unique 또는 duplicate 표현
- DB별 메시지에 포함된 `social_accounts/provider/provider_user` unique 또는 duplicate 표현

known unique 충돌이면 provider를 다시 호출하지 않고, 이미 받은 profile로 새 write transaction을 한 번 더 실행한다. retry transaction은 기존 조회 경로를 그대로 사용하므로 다음 중 하나로 수렴한다.

- provider/sub 기준 social account가 이미 있으면 해당 user로 로그인한다.
- social account는 아직 없지만 같은 email user가 있으면 email user를 인증 완료로 보정하고 Google social account를 연결한다.
- 둘 다 없으면 기존 새 Google-only user 생성 흐름을 따른다.

unrelated `DataIntegrityViolationException`은 숨기지 않고 기존처럼 전파한다.

## 보존한 계약

- Google provider 호출은 DB transaction 밖에서 수행한다.
- user/social account upsert와 refresh session 발급은 write transaction 안에서 수행한다.
- provider retry는 추가하지 않는다.
- Google profile의 verified email 검증은 유지한다.
- 기존 같은 email user는 Google social account를 link한다.
- 새 Google-only user는 `emailVerified=true`, `passwordLoginEnabled=false`로 생성한다.
- provider disabled, provider timeout, unverified email, state mismatch 오류 계약을 변경하지 않는다.
- refresh token 원문은 DB나 JSON response에 저장하거나 노출하지 않는다.
- access token은 기존 `AuthResponse` bearer token으로 유지한다.
- 공개 `userId` query parameter 또는 현재 사용자 DTO `userId` 노출을 추가하지 않는다.

## 운영 영향

중복 OAuth callback, 브라우저 재시도, 네트워크 재전송이 겹쳐도 같은 Google 계정은 하나의 SmartCloset user와 하나의 social account 연결로 수렴한다.

동시에 성공한 callback 요청들은 각각 refresh session을 발급받을 수 있다. 이는 같은 계정의 정상 로그인 성공으로 취급하며, refresh token 원문은 각 응답의 HttpOnly cookie 쓰기 경로로만 전달된다.

known unique 충돌만 retry하므로 FK 오류, refresh session 저장 오류, 기타 DB 제약 위반은 기존처럼 실패해 운영자가 별도 문제로 추적할 수 있다.

## 회귀 기준

Google OAuth upsert 충돌 수렴에서는 다음 기준을 지킨다.

- unique 충돌 recovery가 Google provider를 다시 호출하면 안 된다.
- 실패한 write transaction 안에서 계속 진행하지 않고, 새 transaction에서 재조회해야 한다.
- `users.email`과 `social_accounts(provider, provider_user_id)` 외 constraint violation을 삼키면 안 된다.
- retry 후에도 refresh session 발급은 write transaction 안에서 일어나야 한다.
- social account가 이미 존재하면 새 user를 만들면 안 된다.
- 같은 email user가 존재하면 Google-only user를 새로 만들면 안 된다.
- refresh token raw value를 DB, 로그, JSON body에 추가하면 안 된다.
- concurrency test는 unique email/provider id와 context isolation으로 다른 테스트에 상태를 전파하지 않아야 한다.

## 검증

PR `#187`에서 다음 검증을 통과했다.

- `git diff --check`
- `python3 scripts/checks.py --docs-check --include-final-docs`
- `./gradlew test --tests com.smartcloset.auth.application.GoogleOAuthServiceTest --tests com.smartcloset.auth.application.GoogleOAuthConcurrencyTest --tests com.smartcloset.auth.application.GoogleOAuthTransactionBoundaryTest`
- `./gradlew test`
- `./gradlew build`
- `cd frontend && npm run build`
- GitHub Actions: `test-build`
- 커밋 훅: `python3 -m compileall scripts`
- 커밋 훅: `./gradlew build`
- 커밋 훅: `cd frontend && npm run build`
- Codex CLI read-only review: `pass=true`, findings 없음

추가된 테스트는 social account unique 충돌 retry, users email unique 충돌 retry, unrelated `DataIntegrityViolationException` 전파, 동일 Google profile 동시 callback 2건의 user/social account 단일 수렴과 refresh session 2건 발급을 확인한다.

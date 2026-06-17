# 로그인 실패 시도 제한 기록

## 문서 목적

이 문서는 공개 password login endpoint에 process-local 실패 시도 제한을 추가한 내용을 기록한다.

이 문서는 ADR이 아니며 공개 인증 API shape, refresh token cookie 계약, DB schema, 추천 규칙을 변경하지 않는다. 관련 GitHub Issue는 `#194`이고, 구현은 PR `#196`에서 merge했다.

## 문제

`POST /api/auth/login`은 access token 없이 호출할 수 있는 공개 endpoint다. 공개 endpoint 자체는 필요하지만, password 검증에는 BCrypt 비용이 있고 반복 실패 요청은 계정 보호와 서버 CPU 비용 측면에서 제한이 필요하다.

Issue `#193`에서 미존재 이메일 경로도 dummy BCrypt matcher를 거치도록 보정했기 때문에, 존재하지 않는 이메일 후보를 대량으로 시도해도 요청당 BCrypt 비용이 발생한다. 따라서 timing side-channel 완화와 별개로 반복 실패 요청 자체를 제한해야 한다.

## Before

기존 구조의 위험 요소는 다음과 같았다.

- `/api/auth/login`은 Spring Security에서 `permitAll`로 열려 있었다.
- 애플리케이션 레벨의 로그인 실패 횟수 제한이 없었다.
- 같은 이메일에 대한 password 후보 반복 시도가 계속 BCrypt 비용을 발생시킬 수 있었다.
- 여러 이메일 후보를 바꿔가며 시도하는 client도 별도 제한을 받지 않았다.
- proxy header를 잘못 신뢰하는 방식으로 client key를 설계하면 `X-Forwarded-For` 조작으로 우회될 수 있었다.

## After

개선 후 로그인은 `LoginAttemptThrottle`을 먼저 통과한다.

1. `AuthController.login`은 servlet `request.getRemoteAddr()`로 client identifier를 만든다.
2. `LoginAttemptThrottle`은 인증 비용을 쓰기 전에 현재 요청을 window에 기록한다.
3. throttle key는 두 가지를 함께 사용한다.
   - 정규화 email + client remote address
   - client remote address 단독
4. 두 key 중 하나라도 허용 실패 횟수를 초과하면 `LOGIN_ATTEMPT_LIMIT_EXCEEDED`로 실패한다.
5. 정상 로그인 성공 시 해당 email/client window는 제거한다.
6. 정상 로그인도 인증 전에 1회 예약 기록되므로, client-wide window에서는 현재 성공 요청 예약분만 1회 rollback한다.
7. 인증 실패가 아닌 unexpected runtime error는 현재 요청 예약분만 rollback한다.

기본 설정은 process-local in-memory 기준이며, Redis나 분산 저장소를 추가하지 않았다.

## Client Key 정책

현재 MVP10 local/API 범위에는 신뢰 가능한 proxy 경계가 없다. 따라서 login attempt key는 `X-Forwarded-For`, `Forwarded` 같은 client-supplied proxy header를 사용하지 않는다.

이 결정은 운영 proxy/CDN 환경에서 실제 client IP를 절대 사용하지 않는다는 뜻이 아니다. 운영 배포에서 proxy 경계를 명시적으로 신뢰하도록 설계할 때 별도 후속 이슈로 다뤄야 한다. 이번 변경은 조작 가능한 request header를 기본 신뢰하지 않는 안전한 local/application 기준이다.

## 성공 로그인 처리

성공 로그인은 같은 email/client key의 실패 이력을 제거한다. 사용자가 같은 계정에서 비밀번호를 한두 번 틀린 뒤 올바르게 로그인하면 해당 계정의 실패 window는 사라진다.

반면 client-wide 실패 이력은 성공 로그인으로 전체 초기화하지 않는다. 그렇지 않으면 같은 client가 여러 이메일 후보를 시도하다가 알고 있는 계정으로 한 번 성공 로그인해 전체 client 제한을 우회할 수 있다.

따라서 성공 로그인 시 client-wide window에서는 현재 성공 요청이 인증 전에 예약한 1회분만 rollback한다. 이전에 다른 이메일 후보로 누적한 실패 count는 window 만료 전까지 유지된다.

## 성능 및 보안 영향

허용 횟수를 초과한 요청은 BCrypt matcher 전에 `429 Too Many Requests`로 실패한다. 반복 실패 요청이 계속 BCrypt CPU 비용을 소모하지 않게 해 login endpoint의 비용 상한을 낮춘다.

email/client key는 같은 계정에 대한 password 후보 반복을 제한한다. client-only key는 여러 이메일 후보를 바꿔가며 시도하는 흐름을 제한한다.

제한은 process-local이다. 애플리케이션 재시작, 다중 instance, load balancing 환경에서는 count가 공유되지 않는다. MVP10 범위에서는 Redis, proxy/CDN 설정, 분산 rate limit 저장소를 도입하지 않는다.

## 보존한 계약

- `/api/auth/login`은 공개 endpoint로 유지한다.
- 정상 로그인 성공 시 access token은 JSON body에 담고 refresh token은 HttpOnly cookie로만 전달한다.
- refresh token 원문은 DB와 JSON response에 저장하거나 노출하지 않는다.
- refresh token rotation 계약은 변경하지 않는다.
- 미존재 이메일과 잘못된 비밀번호는 허용 횟수 안에서는 기존처럼 `401 UNAUTHORIZED`를 반환한다.
- 미인증 password 계정과 password login disabled 계정의 기존 오류 계약을 유지한다.
- 제한 초과 응답도 `{ code, message, details }` 실패 응답 shape를 유지한다.
- 공개 `userId` query parameter나 현재 사용자 DTO `userId` 노출을 추가하지 않는다.

## 회귀 방지 기준

로그인 실패 시도 제한은 다음 기준을 지킨다.

- 같은 email/client key에서 허용 횟수를 초과하면 `LOGIN_ATTEMPT_LIMIT_EXCEEDED`로 실패해야 한다.
- 같은 client key에서 여러 이메일 후보 실패가 허용 횟수를 초과해도 `LOGIN_ATTEMPT_LIMIT_EXCEEDED`로 실패해야 한다.
- 제한 초과 요청은 인증 서비스 호출 전에 실패해야 한다.
- `X-Forwarded-For` 값을 바꿔도 같은 servlet remote address 제한을 우회할 수 없어야 한다.
- window가 지나면 같은 key가 다시 시도 가능해야 한다.
- 정상 로그인 성공은 같은 email/client window를 제거해야 한다.
- 정상 로그인 성공이 client-wide 실패 window 전체를 제거하면 안 된다.
- unexpected runtime error는 현재 요청 예약분만 rollback해야 한다.
- Redis, 분산 저장소, 운영 proxy trust 설정을 이 범위에 추가하면 안 된다.

## 검증

PR `#196`에서 다음 검증을 통과했다.

- `./gradlew test --tests com.smartcloset.auth.application.LoginAttemptThrottleTest`
- `./gradlew test --tests com.smartcloset.auth.AuthLoginAttemptThrottleControllerTest`
- `./gradlew test --tests com.smartcloset.auth.AuthControllerTest`
- `python3 scripts/checks.py --docs-check --include-final-docs`
- `git diff --check`
- `./gradlew test`
- `python3 scripts/checks.py --stage manual`
- `git diff --check origin/main...HEAD`
- GitHub Actions: `test-build`
- 커밋 훅: `python3 -m compileall scripts`
- 커밋 훅: `./gradlew test`
- 커밋 훅: `./gradlew build`
- 커밋 훅: `cd frontend && npm run build`
- Codex CLI read-only review 1차: `pass=false`, client-wide window를 성공 로그인으로 지우는 blocker 발견
- Codex CLI read-only review 2차: `pass=true`, findings 없음

추가된 테스트는 같은 email/client 반복 실패 제한, 여러 email 후보에 대한 client-wide 제한, window 만료 후 재시도, 성공 로그인 reset/rollback 정책, unexpected error rollback, `X-Forwarded-For` 조작 우회 방지, refresh cookie 계약 유지를 확인한다.

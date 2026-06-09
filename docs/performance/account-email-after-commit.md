# Account Email After-Commit 성능 기록

## 문서 목적
이 문서는 회원가입, 이메일 인증 재요청, 비밀번호 재설정 요청에서 계정 액션 메일 발송을 DB transaction commit 이후로 분리한 내용을 기록한다.

이 문서는 ADR이 아니며 공개 인증 API, action token 저장 정책, `EmailSender` adapter boundary를 변경하지 않는다. 관련 GitHub Issue는 `#159`이다.

## 문제
계정 액션 token은 DB에 hash만 저장하고 원문 token은 이메일 발송에만 사용한다.

기존 `AuthService.signup`, `requestEmailVerification`, `requestPasswordReset` 흐름은 user와 action token을 write transaction 안에서 저장한 직후 같은 transaction 안에서 `EmailSender`를 호출했다.

이 구조에서는 transaction이 rollback되거나 commit에 실패해도 사용자가 이미 token 원문을 받을 수 있었다. 운영 메일 sender가 추가되면 외부 I/O가 transaction 시간을 늘리고, DB connection과 lock 보유 시간도 메일 provider 지연에 묶일 수 있다.

## Before
기존 구조의 위험 요소는 다음과 같았다.

- 회원가입 흐름에서 user, 기본 옷 preset, email verification token을 저장한 뒤 commit 전에 메일 발송을 시도했다.
- 이메일 인증 재요청과 비밀번호 재설정 요청도 action token 저장 후 commit 전에 `EmailSender`를 호출했다.
- commit 실패나 rollback이 발생하면 사용자는 DB에 존재하지 않는 token을 받을 수 있었다.
- sender 예외가 transaction 안에서 발생하면 DB write와 외부 side effect 정책이 뒤섞일 수 있었다.
- 향후 SES/SMTP sender가 추가될 경우 provider 지연 시간이 write transaction 보유 시간에 포함될 수 있었다.

## After
개선 후 계정 메일 흐름은 DB write와 발송 side effect를 분리한다.

1. `AuthService`는 기존처럼 write transaction 안에서 user/default presets/action token을 생성한다.
2. token 원문은 DB에 저장하지 않고 after-commit 예약 작업에만 전달한다.
3. `AccountEmailSendScheduler`가 Spring `TransactionSynchronization.afterCommit`에 발송 작업을 등록한다.
4. transaction commit이 성공한 뒤에만 `EmailSender`가 호출된다.
5. transaction synchronization이 없는 호출 경로에서는 즉시 발송해 application helper의 기본 동작을 명확히 한다.
6. commit 이후 sender 예외는 warning log로 남기고 삼킨다.

현재 local 구현은 계속 `ConsoleEmailSender`다. SES/SMTP 구현체는 추가하지 않았다.

## 성능 영향
메일 발송은 DB commit 이후 실행되므로 provider 지연이나 sender 장애가 user/action token write transaction 보유 시간을 늘리지 않는다.

DB transaction은 user 생성, 기본 옷 preset 생성, action token 저장 같은 영속성 변경 구간에만 집중된다. sender가 느리거나 실패하더라도 이미 commit된 DB 상태는 rollback되지 않는다.

현재 local profile에서는 console logging 비용만 분리되지만, 운영 sender가 붙는 후속 MVP에서도 auth application service는 `EmailSender` interface와 after-commit scheduler 경계를 유지할 수 있다.

## 실패 정책
계정 메일 발송 실패는 commit 이후 side effect 실패로 취급한다.

- DB commit 전 rollback: sender를 호출하지 않는다.
- DB commit 성공 후 sender 성공: 기존 성공 응답을 유지한다.
- DB commit 성공 후 sender 실패: warning log를 남기고 API transaction 결과를 되돌리지 않는다.

이 정책은 계정 존재 여부를 숨기는 이메일 인증/비밀번호 재설정 요청 응답 shape를 유지하며, token 원문을 DB나 JSON response에 노출하지 않는다.

## 회귀 방지 기준
계정 메일 발송에서는 다음 기준을 지킨다.

- signup rollback 시 email verification sender가 호출되지 않는다.
- email verification request와 password reset request는 commit 성공 후에만 sender를 호출한다.
- commit 성공 후 sender 실패가 user/action token write를 rollback하지 않는다.
- action token 원문은 DB와 API response에 저장하거나 노출하지 않는다.
- 공개 auth API response shape와 error code 계약을 변경하지 않는다.

## 확인한 테스트
이번 변경은 다음 테스트를 추가해 회귀를 막는다.

- `AccountEmailSendSchedulerTest.signupSendsEmailVerificationOnlyAfterCommit`: signup을 외부 transaction 안에서 호출하고 commit 전에는 sender가 비어 있으며 commit 후에만 인증 메일이 예약 발송되는지 확인한다.
- `AccountEmailSendSchedulerTest.rollbackDoesNotSendSignupEmailVerification`: signup 이후 transaction rollback을 강제해 user와 sender 호출이 모두 남지 않는지 확인한다.
- `AccountEmailSendSchedulerTest.emailVerificationRequestSendsOnlyAfterCommit`: 이메일 인증 재요청이 commit 후에만 sender를 호출하는지 확인한다.
- `AccountEmailSendSchedulerTest.passwordResetRequestSendsOnlyAfterCommit`: 비밀번호 재설정 요청이 commit 후에만 sender를 호출하는지 확인한다.
- `AccountEmailSendSchedulerTest.senderFailureAfterCommitDoesNotRollbackSignup`: sender 예외가 commit된 user/action token 저장을 rollback하지 않는지 확인한다.

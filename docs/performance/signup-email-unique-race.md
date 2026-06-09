# 동일 이메일 회원가입 경합 처리

## 배경

회원가입 API는 이미 존재하는 이메일에 대해 `409 EMAIL_ALREADY_EXISTS`를 반환한다. Password signup은 이메일 인증 전까지 access token을 발급하지 않고, user row, 기본 옷 프리셋, 이메일 인증 action token을 하나의 트랜잭션 안에서 만든다.

## 문제

기존 `AuthService.signup`은 `existsByEmail` 선조회 후 `save`를 호출했다. 동일 이메일 요청이 거의 동시에 들어오면 두 요청이 모두 선조회에서 false를 보고, 이후 DB unique constraint에서 패배한 요청이 `DataIntegrityViolationException`으로 실패할 수 있었다.

이 예외가 도메인 오류로 변환되지 않으면 예측 가능한 사용자 입력 경합이 `INTERNAL_SERVER_ERROR`처럼 기록되고, 클라이언트도 기존 중복 가입과 다른 실패 코드를 받을 수 있다.

## 변경

- signup user 저장을 `saveAndFlush`로 바꿔 `users.email` unique 충돌을 signup 트랜잭션 안에서 포착한다.
- `uk_users_email` constraint 또는 users/email unique·duplicate 메시지로 확인되는 충돌만 `EMAIL_ALREADY_EXISTS`로 변환한다.
- 무관한 `DataIntegrityViolationException`은 그대로 전파해 다른 DB 무결성 문제를 이메일 중복으로 숨기지 않는다.
- user 저장 flush가 성공한 뒤에만 기본 옷 프리셋과 이메일 인증 action token을 생성한다.

## 계약 유지

- 이미 존재하는 이메일과 동시 가입 경합에서 패배한 요청 모두 `409 EMAIL_ALREADY_EXISTS`를 사용한다.
- 실패 응답은 `{ code, message, details }` 공통 구조를 유지한다.
- 실패한 중복 요청은 user row, 기본 옷 프리셋, account action token을 남기지 않는다.
- 이메일 인증 메일 발송은 기존 after-commit scheduler 경계를 유지한다.

## 검증

- `AuthServiceUniqueViolationTest`
  - `users.email` unique 충돌로 보이는 flush 실패를 `EMAIL_ALREADY_EXISTS`로 변환한다.
  - 무관한 DB 무결성 실패는 `EMAIL_ALREADY_EXISTS`로 변환하지 않는다.
  - flush 실패 시 기본 옷 프리셋, action token, 메일 예약이 호출되지 않는다.
- `AuthSignupConcurrencyTest`
  - 동일 이메일 동시 signup 두 요청 중 하나만 성공한다.
  - 패배한 요청은 `EMAIL_ALREADY_EXISTS`로 실패한다.
  - 성공한 user에만 기본 옷 프리셋 5개와 이메일 인증 action token 1개가 남는다.

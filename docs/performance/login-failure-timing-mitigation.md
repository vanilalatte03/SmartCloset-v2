# 로그인 실패 응답 시간차 완화 기록

## 문서 목적

이 문서는 비밀번호 로그인 실패 경로에서 가입 이메일 여부가 응답 시간 차이로 드러날 수 있던 위험과 완화 내용을 기록한다.

이 문서는 ADR이 아니며 공개 인증 API shape, refresh token cookie 계약, password login disabled 계약을 변경하지 않는다. 관련 GitHub Issue는 `#193`이고, 구현은 PR `#195`에서 merge했다.

## 문제

`POST /api/auth/login`은 비밀번호 기반 공개 인증 endpoint다. 실패 응답의 HTTP status와 error code가 같더라도, 내부 처리 비용이 크게 다르면 공격자가 반복 요청으로 가입 이메일 후보를 추정할 수 있다.

기존 `AuthService.login`은 `UserRepository.findByEmail(...)`에서 사용자를 찾지 못하면 즉시 `UNAUTHORIZED`를 던졌다. 반면 존재하는 password 계정은 `PasswordEncoder.matches(...)`로 BCrypt 검증을 수행한 뒤 비밀번호 오류를 판단했다.

BCrypt 검증은 의도적으로 비용이 큰 연산이다. 따라서 존재하지 않는 이메일과 존재하는 이메일의 잘못된 비밀번호 요청 사이에 관찰 가능한 시간 차이가 생길 수 있었다.

## Before

기존 구조의 위험 요소는 다음과 같았다.

- 미존재 이메일은 DB 조회 직후 matcher 호출 없이 `401 UNAUTHORIZED`로 실패했다.
- 존재하는 password 계정의 잘못된 비밀번호는 BCrypt matcher 실행 후 `401 UNAUTHORIZED`로 실패했다.
- API 응답 body는 동일해도 실패 경로의 CPU 비용 차이가 컸다.
- 미인증 password 계정은 올바른 비밀번호 검증 후 `403 EMAIL_VERIFICATION_REQUIRED`로 실패해야 하므로, 단순히 모든 실패를 같은 분기로 합칠 수는 없었다.
- Google-only 등 password login disabled 계정의 기존 오류 계약도 유지해야 했다.

## After

개선 후 로그인 실패 경로는 미존재 이메일에도 고정 더미 BCrypt hash를 사용해 password matcher를 1회 실행한다.

1. `AuthService.login`이 이메일로 사용자를 조회한다.
2. 사용자가 없으면 `PasswordEncoder.matches(request.password(), DUMMY_PASSWORD_HASH)`를 호출한다.
3. matcher 결과와 무관하게 기존처럼 `UNAUTHORIZED`를 반환한다.
4. 사용자가 있으면 기존 순서를 유지해 password login 가능 여부, 비밀번호, 이메일 인증 여부를 차례로 검증한다.
5. 로그인 성공 시 access token 응답과 refresh cookie 발급 계약은 변경하지 않는다.

더미 hash는 실제 사용자 password나 token과 무관한 sentinel BCrypt hash다. DB, 로그, JSON response에는 저장하거나 노출하지 않는다.

## 성능 및 보안 영향

미존재 이메일 요청도 BCrypt 비용을 1회 소모하므로, 존재하는 이메일의 잘못된 비밀번호 요청과 실패 경로 비용이 더 가까워진다. 이는 가입 이메일 여부를 timing side-channel로 추정하는 난이도를 높인다.

이 완화는 계정 보호의 한 축일 뿐이며, 반복 로그인 시도 자체를 제한하지는 않는다. 로그인 실패 횟수 제한, client key 기반 throttle, 신뢰 가능한 proxy 경계 처리는 별도 Issue `#194` 범위로 남아 있다.

## 보존한 계약

- 미존재 이메일과 잘못된 비밀번호는 기존처럼 `401 UNAUTHORIZED`를 반환한다.
- 미인증 password 계정은 올바른 비밀번호 검증 이후 기존처럼 `403 EMAIL_VERIFICATION_REQUIRED`를 반환한다.
- password login disabled 계정은 기존 `PASSWORD_LOGIN_DISABLED` 계약을 유지한다.
- 성공한 login은 access token을 JSON body에 담고 refresh token은 HttpOnly cookie로만 전달한다.
- refresh token 원문, 계정 action token 원문, password hash는 JSON response에 노출하지 않는다.
- 공개 `userId` query parameter나 현재 사용자 DTO `userId` 노출을 추가하지 않는다.

## 회귀 방지 기준

로그인 실패 시간차 완화는 다음 기준을 지킨다.

- 미존재 이메일 로그인 요청에서도 `PasswordEncoder.matches(...)`가 더미 hash로 1회 호출되어야 한다.
- 더미 hash는 유효한 BCrypt hash여야 한다.
- 미존재 이메일과 잘못된 비밀번호의 공개 실패 응답은 `UNAUTHORIZED`로 유지해야 한다.
- 미인증 password 계정의 `EMAIL_VERIFICATION_REQUIRED` 분기를 비밀번호 검증 이전으로 옮기면 안 된다.
- password login disabled 계정의 기존 오류 계약을 바꾸면 안 된다.
- token 원문이나 raw secret을 DB, 로그, JSON response에 저장하거나 노출하면 안 된다.

## 검증

PR `#195`에서 다음 검증을 통과했다.

- `./gradlew test --tests com.smartcloset.auth.application.AuthServiceOnboardingBoundaryTest`
- `./gradlew test --tests com.smartcloset.auth.AuthControllerTest`
- `./gradlew test`
- `python3 scripts/checks.py --stage manual`
- `git diff --check origin/main...HEAD`
- GitHub Actions: `test-build`
- 커밋 훅: `python3 -m compileall scripts`
- 커밋 훅: `./gradlew test`
- 커밋 훅: `./gradlew build`
- 커밋 훅: `cd frontend && npm run build`
- Codex CLI read-only review: `pass=true`, findings 없음

추가된 테스트는 미존재 이메일 로그인에서 더미 BCrypt matcher가 호출되는지, 더미 hash가 유효한 BCrypt hash인지, 기존 로그인 컨트롤러 오류 응답 계약이 유지되는지 확인한다.

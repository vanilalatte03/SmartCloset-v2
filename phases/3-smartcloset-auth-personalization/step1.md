# 단계 1: auth-api-and-session-contract

범위: Must-have / 3차 P0

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/ARCHITECTURE.md`
- `docs/API.md`
- `docs/COMMANDS.md`
- `docs/adr/008-mvp3-authenticated-user-personalization.md`
- `src/main/java/com/smartcloset/security/**`
- `src/main/java/com/smartcloset/user/domain/User.java`
- `src/main/java/com/smartcloset/user/repository/UserRepository.java`
- `src/main/java/com/smartcloset/common/response/**`
- `src/main/java/com/smartcloset/common/exception/**`

이전 단계에서 만들어진 코드를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업
회원가입, 로그인, 현재 사용자 조회 API를 구현하고 프론트 세션 계약의 백엔드 기반을 완성한다. 기존 옷/위치/추천 API의 `userId` 제거는 이후 모듈별 step에서 수행한다.

## 변경 예상 파일
- `src/main/java/com/smartcloset/auth/**`
- `src/main/java/com/smartcloset/security/**`
- `src/main/java/com/smartcloset/user/application/**`
- `src/main/java/com/smartcloset/user/dto/**`
- `src/main/java/com/smartcloset/user/presentation/**`
- `src/test/java/com/smartcloset/auth/**`
- `src/test/java/com/smartcloset/security/**`
- `src/test/java/com/smartcloset/user/**`

## 구현 메모
- 공개 API는 이 단계에서 아래 둘을 추가한다.
  - `POST /api/auth/signup`
  - `POST /api/auth/login`
- 보호 API는 현재 사용자 조회부터 시작한다.
  - `GET /api/users/me`
- 성공 응답은 `{ "data": ... }` 형태를 유지한다.
- 실패 응답은 `{ "code": "...", "message": "...", "details": [] }` 형태를 유지하고 `details`는 항상 배열이다.
- 회원가입 request:
  - `email`: email 형식, unique
  - `password`: blank 불가, 최소 8자
  - `name`: blank 불가, 최대 50자
- 회원가입 성공 시 기본 위치 서울 `SEOUL`, role `USER`, 빈 선호도 배열을 가진 사용자를 생성한다.
- 로그인 성공 응답은 `accessToken`, `tokenType=Bearer`, `user`를 반환한다.
- `AuthResponse.user`와 `GET /api/users/me` 응답은 같은 현재 사용자 DTO를 사용한다.
- 현재 사용자 DTO에는 `userId`를 넣지 않는다.
- 중복 email은 `EMAIL_ALREADY_EXISTS`로 실패한다.
- token 없음, 잘못된 token, 만료 token은 보호 API에서 401 계열로 실패해야 한다.
- 기존 2차 API 전환 전까지 필요한 임시 permit rule은 명확히 제한하고 step 7에서 제거할 수 있게 테스트나 TODO를 남긴다.

## 검증 절차
```bash
git diff --check
./gradlew test
```

## 인수 기준
- `POST /api/auth/signup`이 `201 Created`로 신규 사용자를 만든다.
- 중복 email 회원가입은 `EMAIL_ALREADY_EXISTS`로 실패한다.
- `POST /api/auth/login`이 올바른 password에서 Bearer access token을 반환한다.
- 잘못된 password 또는 없는 email은 인증 실패로 처리한다.
- `GET /api/users/me`는 유효 token으로 현재 사용자를 반환한다.
- `GET /api/users/me` 응답에는 `userId`가 없다.
- token 없음, 잘못된 token, 만료 token에 대한 controller/security test가 있다.
- JWT claim은 `email`, `role`만 사용한다.

## 금지사항
- signup/login 외 공개 auth endpoint를 추가하지 마라. 이유: 3차 공개 API는 두 개로 고정됐다.
- 로그인 응답에 refresh token을 넣지 마라. 이유: refresh token은 3차 제외 범위다.
- 현재 사용자 응답에 `userId`를 노출하지 마라. 이유: 현재 사용자 전용 DTO에서는 `userId` 제거가 3차 기준이다.
- 회원가입 화면을 위해 위치 catalog 조회 API를 공개로 열지 마라. 이유: `GET /api/locations`는 보호 API다.
- password hash 비교를 직접 문자열 비교로 구현하지 마라. 이유: BCrypt `PasswordEncoder#matches`를 사용해야 한다.

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

이 단계의 security 적용 범위는 공개 auth 2종과 `GET /api/users/me` 검증으로 제한한다. 아직 인증 사용자 기준으로 전환하지 않은 기존 2차 API를 `/api/**` 최종 정책으로 한 번에 잠그지 않는다.

## Step 1 리뷰 기준
허용 범위:
- `POST /api/auth/signup`
- `POST /api/auth/login`
- `GET /api/users/me`
- JWT 인증 실패 JSON 응답
- 위 API를 동작시키기 위한 최소 security wiring

금지 범위:
- `frontend/**` 인증/session 전환
- clothing, location, recommendation API의 current-user 전환
- `GET/PUT /api/users/me/preferences`
- `preferenceScore` 전환
- 추천 이력 API 구현
- static demo 갱신

현재 step의 blocker가 아닌 것:
- `/api/users/me/preferences`가 아직 404인 상태
- 프론트가 아직 `sessionStorage`와 Bearer header를 사용하지 않는 상태
- 추천 이력 API가 아직 없는 상태
- 추천 점수 DTO나 테스트에 기존 다양성 점수가 남아 있는 상태

현재 step의 blocker:
- signup/login이 동작하지 않거나 응답 계약이 깨진 경우
- `GET /api/users/me`가 유효 token으로 동작하지 않는 경우
- `GET /api/users/me` 응답에 `userId`가 노출되는 경우
- refresh token을 추가한 경우
- `/api/**` 전체 인증 필수 정책을 적용해 아직 전환하지 않은 API를 깨뜨린 경우

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
- `SecurityConfig`는 이 단계에서 아래 경계를 만족해야 한다.
  - `POST /api/auth/signup`, `POST /api/auth/login`은 `permitAll`
  - `GET /api/users/me`는 Bearer token 필수
  - 아직 전환하지 않은 옷/위치/추천 API는 해당 모듈 step까지 임시 허용
  - 임시 허용 matcher에는 Step 7 제거 대상임을 이름이나 주석으로 표시
- security filter chain을 실제로 사용하기 시작하면 `http.cors(...)`를 활성화해 React dev server preflight가 막히지 않게 한다.
- JWT error response가 `ObjectMapper` 등 framework bean에 의존하면 해당 bean 등록을 보장하고 ApplicationContext test로 확인한다.

## 검증 절차
```bash
git diff --check
rg -n 'http\\.cors|cors\\(' src/main/java/com/smartcloset/security src/test/java/com/smartcloset/security
! rg -n 'anyRequest\\(\\)\\.authenticated|/api/\\*\\*.*authenticated' src/main/java/com/smartcloset/security
./gradlew test
```

## 인수 기준
- `POST /api/auth/signup`이 `201 Created`로 신규 사용자를 만든다.
- 중복 email 회원가입은 `EMAIL_ALREADY_EXISTS`로 실패한다.
- `POST /api/auth/login`이 올바른 password에서 Bearer access token을 반환한다.
- 잘못된 password 또는 없는 email은 인증 실패로 처리한다.
- `GET /api/users/me`는 유효 token으로 현재 사용자를 반환한다.
- `GET /api/users/me` 응답에는 `userId`가 없다.
- `GET /api/users/me`의 token 없음, 잘못된 token, 만료 token에 대한 controller/security test가 있다.
- 기존 옷/위치/추천 API 테스트는 이 단계 이후에도 Step 0처럼 통과한다.
- JWT claim은 `email`, `role`만 사용한다.

## 금지사항
- signup/login 외 공개 auth endpoint를 추가하지 마라. 이유: 3차 공개 API는 두 개로 고정됐다.
- 로그인 응답에 refresh token을 넣지 마라. 이유: refresh token은 3차 제외 범위다.
- 현재 사용자 응답에 `userId`를 노출하지 마라. 이유: 현재 사용자 전용 DTO에서는 `userId` 제거가 3차 기준이다.
- 회원가입 화면을 위해 위치 catalog 조회 API를 공개로 열지 마라. 이유: `GET /api/locations`는 보호 API다.
- password hash 비교를 직접 문자열 비교로 구현하지 마라. 이유: BCrypt `PasswordEncoder#matches`를 사용해야 한다.
- `/api/**` 전체를 인증 필수로 만들지 마라. 이유: 옷/위치/추천 API는 아직 인증 사용자 기준으로 전환되지 않았다.
- 옷/위치/추천 controller의 `userId` 제거를 이 단계에 섞지 마라. 이유: 모듈별 전환 step의 리뷰 범위를 흐린다.
- preferences API, `preferenceScore`, 추천 이력, frontend session flow를 이 단계에 섞지 마라. 이유: 각각 Step 4, 5, 6, 8 이후 책임이다.

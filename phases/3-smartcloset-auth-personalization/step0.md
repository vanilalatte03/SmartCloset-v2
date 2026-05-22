# 단계 0: user-account-schema-and-token-infra

범위: Must-have / 3차 P0

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/ARCHITECTURE.md`
- `docs/API.md`
- `docs/ERD.md`
- `docs/COMMANDS.md`
- `docs/adr/004-spring-boot-version.md`
- `docs/adr/008-mvp3-authenticated-user-personalization.md`
- `build.gradle`
- `src/main/java/com/smartcloset/user/domain/User.java`
- `src/main/java/com/smartcloset/user/repository/UserRepository.java`
- `src/main/resources/application.yml`

## 작업
3차 인증 사용자 기반 전환을 위한 User schema, token/security infra, test helper 기반을 만든다. 이 단계는 HTTP auth endpoint, 기존 API 전환, 최종 보호 API 정책을 아직 수행하지 않는다.

이 단계의 목표는 “보안 경계를 최종 적용하는 것”이 아니라 “이후 단계가 사용할 인증 인프라를 추가하면서 기존 2차 API와 테스트를 깨뜨리지 않는 것”이다.

## 변경 예상 파일
- `build.gradle`
- `src/main/java/com/smartcloset/user/domain/User.java`
- `src/main/java/com/smartcloset/user/domain/UserRole.java`
- `src/main/java/com/smartcloset/user/repository/UserRepository.java`
- `src/main/java/com/smartcloset/security/**`
- `src/main/java/com/smartcloset/common/config/**`
- `src/main/resources/application.yml`
- `src/test/java/com/smartcloset/user/**`
- `src/test/java/com/smartcloset/security/**`

## 구현 메모
- Spring Boot 버전은 `4.0.6`으로 유지한다.
- `users`에 아래 필드를 추가한다.
  - `email`
  - `password_hash`
  - `role`
  - `preferred_colors_json`
  - `preferred_materials_json`
  - `style_tags_json`
- `email`은 unique 조회가 가능해야 한다.
- 신규 사용자 생성용 factory 또는 method는 role `USER`, 기본 위치 `SEOUL`, 빈 선호도 JSON `[]`를 설정해야 한다.
- 기존 row 호환을 위해 위치 snapshot 컬럼은 2차 정책을 유지한다. 필요하면 애플리케이션 backfill을 사용한다.
- JWT infra는 다음 기준을 만족한다.
  - `HS256`
  - secret source `JWT_SECRET`
  - subject는 현재 사용자 id 문자열
  - claims는 `email`, `role`
  - 만료 시간은 2시간
- `CurrentUserPrincipal`은 `userId`, `email`, `role`을 담는다.
- password hash 저장을 위해 BCrypt 기반 `PasswordEncoder` bean을 준비한다.
- Spring Security dependency 추가로 기존 API가 즉시 깨지지 않도록 명시적인 Step 0 임시 security 설정을 둔다.
  - 모든 request를 임시 `permitAll`로 둔다.
  - form login, basic auth, CSRF로 인해 API 테스트가 redirect 또는 HTML 응답을 받지 않게 한다.
  - 임시 허용 이름에는 `MVP3_STEP0_TEMP_PERMIT_ALL`처럼 Step 7 제거 대상임을 드러낸다.
- Step 0에서는 `JwtAuthenticationFilter`를 security filter chain에 연결하지 않는다. 이유: auth endpoint와 current-user endpoint가 아직 없고, 보호 API 전환도 모듈별 step에서 수행한다.
- Step 0에서는 `/api/**` 또는 기존 location/clothes/recommendation API에 대한 401/403 테스트를 추가하지 않는다. 이유: 이 단계의 acceptance는 기존 API 테스트 유지다.
- `JwtAuthenticationEntryPoint`처럼 JSON error response를 쓰는 component를 만들 경우, Jackson `ObjectMapper` 등 필요한 bean이 실제 ApplicationContext에 존재하는지 테스트하거나 이 단계에서는 bean 등록을 늦춘다.
- `SecurityConfig`에 CORS 설정이 필요하면 나중에 보호 API 전환 시 `http.cors(...)`로 연결할 수 있게 TODO를 남긴다. 단, Step 0에서 CORS 때문에 기존 테스트가 깨지면 안 된다.

## 검증 절차
```bash
git diff --check
rg -n 'MVP3_STEP0_TEMP_PERMIT_ALL|anyRequest\\(\\)\\.permitAll' src/main/java src/test/java
! rg -n 'anyRequest\\(\\)\\.authenticated|addFilterBefore\\(.*JwtAuthenticationFilter|ProtectedApiSecurityConfigTest' src/main/java src/test/java
! rg -n 'GET /api/locations.*401|/api/clothes.*401|/api/recommendations.*401' src/test/java
./gradlew test
```

## 인수 기준
- `User`가 email/passwordHash/role/preference JSON field를 가진다.
- 신규 사용자 생성 경로에서 role `USER`, 기본 위치 `SEOUL`, 선호도 JSON `[]`가 설정된다.
- `UserRepository`가 email unique 조회와 중복 판단을 지원한다.
- JWT 발급/검증 단위 테스트가 subject, claims, 만료 정책을 검증한다.
- 잘못된 token 또는 만료 token 검증 실패가 표현 가능한 예외/결과로 처리된다.
- Step 0의 `SecurityConfig`는 기존 API를 임시 허용하며, `/api/**` 전체 인증 요구를 적용하지 않는다.
- `JwtAuthenticationFilter`가 아직 security filter chain에 연결되지 않는다.
- 보호 API 401/403 회귀 테스트는 이 단계에 추가되지 않는다.
- 기존 위치, 옷, 추천 테스트가 이 단계 이후에도 통과한다.

## 금지사항
- plaintext password를 저장하지 마라. 이유: 3차 인증 기준은 BCrypt hash 저장이다.
- JWT secret을 코드나 문서에 실제 값으로 박지 마라. 이유: 민감정보는 커밋 금지다.
- refresh token 테이블이나 rotation 로직을 만들지 마라. 이유: 3차 제외 범위다.
- 선호도 별도 테이블을 만들지 마라. 이유: 3차는 `users` JSON 문자열 컬럼으로 고정한다.
- `styleTags`를 추천 점수 계산 모델에 연결하지 마라. 이유: 3차에서는 저장/조회/표시만 한다.
- `/api/**` 전체를 인증 필수로 만들지 마라. 이유: Step 0에는 signup/login/me endpoint와 프론트 Bearer token 흐름이 아직 없다.
- 기존 controller의 `userId` request parameter를 제거하지 마라. 이유: API 전환은 Step 2, 3, 6에서 모듈별로 수행한다.
- 보호 API 401/403 테스트를 추가하지 마라. 이유: 최종 보안 경계 회귀 테스트는 Step 7의 책임이다.
- 임시 security 허용을 최종 정책처럼 문서화하지 마라. 이유: step 7에서 공개 API를 auth 2종으로 제한해야 한다.

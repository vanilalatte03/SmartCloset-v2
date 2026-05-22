# 단계 7: security-boundary-and-regression-tests

범위: Must-have / 3차 P0

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/API.md`
- `docs/ARCHITECTURE.md`
- `docs/COMMANDS.md`
- `docs/adr/008-mvp3-authenticated-user-personalization.md`
- `src/main/java/com/smartcloset/security/**`
- `src/main/java/com/smartcloset/auth/**`
- `src/main/java/com/smartcloset/*/presentation/**`
- `src/test/java/com/smartcloset/**`

이전 단계에서 만들어진 코드를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업
3차 백엔드 API 보안 경계를 최종 형태로 잠근다. 임시 permit rule을 제거하고, 공개 API는 auth 2종뿐이며 나머지 API는 Bearer token을 요구한다는 회귀 테스트와 검색 검증을 추가한다.

이 단계는 Step 1~6의 API 전환이 완료된 뒤에만 수행한다. 아직 `@RequestParam Long userId`, today 추천 GET 경로, 기존 다양성 점수 필드가 남아 있으면 SecurityConfig를 고치기 전에 해당 모듈 step을 수정하거나 현재 step을 blocked로 기록한다.

## 변경 예상 파일
- `src/main/java/com/smartcloset/security/SecurityConfig.java`
- `src/main/java/com/smartcloset/security/JwtAuthenticationFilter.java`
- `src/main/java/com/smartcloset/common/exception/**`
- `src/test/java/com/smartcloset/security/**`
- `src/test/java/com/smartcloset/auth/**`
- `src/test/java/com/smartcloset/**`

## 구현 메모
- 공개 API는 아래 둘뿐이다.
  - `POST /api/auth/signup`
  - `POST /api/auth/login`
- Swagger UI, OpenAPI JSON, static resource 허용은 개발/문서 확인용으로 유지할 수 있다.
- 그 외 `/api/**`는 `Authorization: Bearer {accessToken}`을 요구한다.
- security filter chain은 API JSON 응답과 React frontend 호출을 지원해야 한다.
  - form login, HTTP basic, logout, CSRF는 API 흐름에 맞게 비활성화한다.
  - `http.cors(...)`를 활성화해 frontend preflight가 Security filter에서 막히지 않게 한다.
  - 세션은 stateless로 유지한다.
- token 없음은 `UNAUTHORIZED` 또는 문서화된 401 응답으로 처리한다.
- 잘못된 token 또는 만료 token은 `INVALID_TOKEN` 또는 문서화된 401 응답으로 처리한다.
- JWT authentication entry point가 JSON body를 만들 때 필요한 serializer bean이 ApplicationContext에 등록되어 있는지 테스트한다.
- 인증 principal은 Controller method에서 현재 user id를 일관되게 얻을 수 있어야 한다.
- 공개 HTTP API의 `userId` query parameter 제거를 검색 검증으로 고정한다.
- 현재 사용자 전용 response DTO의 `userId` 제거를 테스트 또는 JSON path assertion으로 고정한다.
- 2차 호환용 임시 security permit rule과 임시 TODO를 제거한다.

## 검증 절차
```bash
git diff --check
! rg -n 'permitAll\\(\\).*api|TEMP|temporary|compatibility.*userId|userId.*RequestParam' src/main/java
! rg -n 'GET /api/recommendations/(today)' src/main/java src/test/java
! rg -n -F -e 'POST /api/recommendations?userId' -e '/api/clothes?userId' -e '/api/users/location?userId' src/main/java src/test/java
! rg -n -e '@RequestParam.*userId' -e 'RequestParam Long userId' -e '\\.param\\("userId"' src/main/java src/test/java
rg -n 'http\\.cors|cors\\(' src/main/java/com/smartcloset/security src/test/java/com/smartcloset/security
./gradlew test
./gradlew build
```

## 인수 기준
- 공개 auth 2종은 token 없이 호출 가능하다.
- `GET /api/users/me`, 위치, 선호도, 옷, 추천 API는 token 없이 401로 실패한다.
- 잘못된 token과 만료 token이 401로 실패하는 테스트가 있다.
- React dev server origin의 CORS preflight가 security layer에서 거부되지 않는 테스트 또는 설정 검증이 있다.
- 기존 API controller에서 `@RequestParam Long userId` 또는 동등한 공개 userId 식별자가 제거됐다.
- 현재 사용자 전용 response DTO에서 `userId`가 제거됐다는 테스트가 있다.
- 사용자 A/B data isolation test가 옷, 위치, 선호도, 추천 이력, 착용 완료 중 위험도가 높은 경로를 커버한다.
- `./gradlew build`가 통과한다.

## 금지사항
- `/api/**` 전체를 `permitAll`로 남기지 마라. 이유: 3차 보호 API는 Bearer token이 필요하다.
- public auth endpoint를 signup/login 외에 추가하지 마라. 이유: 공개 API는 두 개로 고정됐다.
- security failure를 HTML login page redirect로 응답하지 마라. 이유: API 클라이언트는 JSON 실패 응답을 기대한다.
- userId query parameter를 테스트 편의용으로 되살리지 마라. 이유: 공개 HTTP API에서 제거됐다.
- 미전환 API를 security permit rule로 숨기지 마라. 이유: Step 7은 남은 임시 허용을 제거하고 실제 계약을 검증하는 단계다.

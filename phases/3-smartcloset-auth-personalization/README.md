# Phase: SmartCloset 3차 Auth + Personalization MVP

## 목표
SmartCloset 2차의 위치 기반 React 프론트엔드와 KMA 추천 흐름을 인증 사용자 기반 서비스로 전환한다. 회원가입/로그인, Spring Security, JWT Bearer access token, 현재 사용자 기준 API, 사용자 선호도, `preferenceScore`, 추천 이력 조회, 프론트 세션 흐름, Docker Compose 공유 검증을 3차 MVP 범위로 완성한다.

## 작업 범위
- Must-have / 3차 P0: 회원가입/로그인, Spring Security, JWT Bearer access token, `sessionStorage` token 저장, 공개 API와 보호 API 분리, 공개 HTTP API `?userId=` 제거, 현재 사용자 전용 response DTO `userId` 제거, 사용자별 옷장/위치/추천 이력/착용 이력 분리, 선호도 API와 `users` JSON 문자열 컬럼 저장, `preferenceScore`, 추천 이력 limit 정책, React 인증/개인화 화면, Docker Compose DB 초기화 안내
- Should-have / 3차 P1: 인증 만료 시 프론트 로그인 화면 전환, 401/403 표시 정리, 추천 이력 화면 polish, 인증/개인화 테스트 보강

## 제외 범위
- refresh token
- 소셜 로그인
- 이메일 인증
- 비밀번호 재설정
- 관리자 권한 기능
- 외부 주소/지도 API
- 브라우저 현재 위치 자동 감지
- 위경도-KMA 격자 변환 API
- 선호도 별도 테이블 정규화
- styleTags 기반 점수 계산 또는 추천 이유
- AI/GPT 추천
- 옷 이미지 업로드
- Redis
- AWS 배포와 CD 자동화

## Steps
| Step | Name | Range |
| ---: | --- | --- |
| 0 | user-account-schema-and-token-infra | Must-have / 3차 P0 |
| 1 | auth-api-and-session-contract | Must-have / 3차 P0 |
| 2 | clothing-current-user-api | Must-have / 3차 P0 |
| 3 | location-current-user-api | Must-have / 3차 P0 |
| 4 | preferences-api-and-storage | Must-have / 3차 P0 |
| 5 | preference-score-rules | Must-have / 3차 P0 |
| 6 | recommendation-current-user-api | Must-have / 3차 P0 |
| 7 | security-boundary-and-regression-tests | Must-have / 3차 P0 |
| 8 | frontend-auth-session | Must-have / 3차 P0 |
| 9 | frontend-personalization-flows | Must-have / 3차 P0 |
| 10 | sharing-verification-and-doc-sync | Must-have / 3차 P0 |

## 단계 진행 원칙
- Step 0은 user schema, JWT 발급/검증 유틸리티, password encoder, test helper 기반만 만든다. 이 단계에서 최종 보호 API 정책을 적용하지 않는다.
- Spring Security dependency를 Step 0에서 추가하면 기존 2차 API와 테스트가 계속 통과하도록 명시적인 임시 permit-all filter chain을 둔다. 이 임시 허용은 Step 7 제거 대상으로 이름이나 주석에 드러낸다.
- `JwtAuthenticationFilter`, `JwtAuthenticationEntryPoint`, 보호 API 401/403 회귀 테스트는 auth endpoint와 current-user endpoint가 생긴 뒤 단계별로 연결한다. Step 0에서 `/api/**` 전체 인증 요구를 걸지 않는다.
- Step 1은 공개 auth 2종과 `GET /api/users/me`만 인증 경계로 검증한다. 아직 전환하지 않은 옷/위치/추천 API는 해당 모듈 step까지 임시 허용을 유지한다.
- Step 2, 3, 4, 6은 자신이 전환한 API만 보호 API로 잠근다. 최종 “auth 2종 외 전부 보호” 회귀 테스트는 Step 7에서만 추가한다.
- Step 7을 시작하기 전 `@RequestParam Long userId`, today 추천 GET 경로, 기존 다양성 점수 필드가 남아 있으면 먼저 해당 모듈 step을 수정한다. SecurityConfig로 덮어서 통과시키지 않는다.

## 완료 기준
- 공개 API는 `POST /api/auth/signup`, `POST /api/auth/login`뿐이다.
- 모든 보호 API는 `Authorization: Bearer {accessToken}`을 요구한다.
- JWT access token은 `HS256` + `JWT_SECRET`으로 서명하며 만료 시간은 2시간이다.
- 신규 사용자는 role `USER`, 기본 위치 `SEOUL`, 빈 `preferredColors`, `preferredMaterials`, `styleTags`로 생성된다.
- 공개 HTTP API와 프론트 타입에서 `userId` query parameter가 제거된다.
- 현재 사용자 전용 response DTO에는 `userId`가 없다.
- 옷장, 위치, 선호도, 추천 생성, 추천 이력, 착용 완료는 현재 인증 사용자 기준으로 격리된다.
- 추천 생성 API는 `POST /api/recommendations`만 사용한다.
- 추천 이력 조회는 `GET /api/recommendations?limit={limit}`이며 기본 20, 최소 1, 최대 50, 최신순이다.
- 기존 다양성 점수는 `preferenceScore`로 교체된다.
- `preferenceScore`는 선호 색상 일치 5점, 선호 소재 일치 5점, 최대 10점이다.
- `styleTags`는 저장/조회/표시만 하며 점수와 추천 이유에는 영향을 주지 않는다.
- 프론트는 access token을 `sessionStorage`에 저장하고 새로고침 후 `GET /api/users/me`로 세션을 복구한다.
- Docker Compose 공유 흐름은 `mysql`, `app`, `frontend`를 포함하고 MVP 3 전환 시 `docker compose down -v`를 안내한다.

## 검증 명령
```bash
git diff --check
! rg -n 'GET /api/recommendations/(today)' README.md docs/PRD.md docs/API.md docs/ARCHITECTURE.md docs/FRONTEND.md docs/RECOMMENDATION_RULES.md docs/ERD.md docs/DEMO_SCENARIO.md docs/SHARING_GUIDE.md docs/COMMANDS.md AGENTS.md .agents/skills/smartcloset-backend/SKILL.md
! rg -n 'POST /api/recommendations\?userId|/api/clothes\?userId|/api/users/location\?userId' README.md docs/PRD.md docs/API.md docs/ARCHITECTURE.md docs/FRONTEND.md docs/RECOMMENDATION_RULES.md docs/ERD.md docs/DEMO_SCENARIO.md docs/SHARING_GUIDE.md docs/COMMANDS.md AGENTS.md .agents/skills/smartcloset-backend/SKILL.md
rg -n 'preferenceScore|preferred_colors_json|preferred_materials_json|style_tags_json' README.md docs/PRD.md docs/API.md docs/ARCHITECTURE.md docs/FRONTEND.md docs/RECOMMENDATION_RULES.md docs/ERD.md docs/DEMO_SCENARIO.md docs/SHARING_GUIDE.md docs/COMMANDS.md AGENTS.md .agents/skills/smartcloset-backend/SKILL.md
./gradlew test
./gradlew build
(cd frontend && npm run build)
docker compose config
```

Docker Compose smoke까지 확인하는 최종 step에서는 아래를 추가로 실행한다.

```bash
docker compose down -v
test -f .env || cp .env.example .env
docker compose up --build -d
curl -fsS http://localhost:8080/v3/api-docs >/dev/null
docker compose down
```

## 실행 예시
```bash
python3 scripts/execute.py 3-smartcloset-auth-personalization --next-step-only
python3 scripts/execute.py 3-smartcloset-auth-personalization
python3 scripts/autopilot.py 3-smartcloset-auth-personalization --base main --max-review-fixes 2 --unsafe
```

## 리스크
- Spring Security dependency만 추가하고 명시적인 `SecurityConfig`를 두지 않으면 기존 API와 테스트가 의도치 않게 기본 로그인으로 막힐 수 있다.
- Step 0에서 최종 SecurityConfig를 적용하면 auth endpoint와 frontend Bearer token 흐름이 아직 없어 기존 API와 테스트가 대량으로 깨진다.
- JWT error response bean이 Jackson `ObjectMapper` 같은 미등록 bean에 의존하면 전체 ApplicationContext가 실패할 수 있다. 해당 bean을 쓰는 시점에 등록 여부를 테스트한다.
- API를 한 번에 전환하지 않으면 중간 단계에 임시 허용 구간이 생길 수 있다. 임시 허용은 전환된 API별로 좁히고 최종 security step에서 반드시 제거한다.
- `userId` query parameter 제거와 response DTO 제거가 프론트 타입과 동시에 맞지 않으면 build는 통과해도 런타임 API 호출이 깨질 수 있다.
- `styleTags`를 추천 점수 또는 추천 이유에 반영하면 3차 범위를 넘는다.
- Docker Compose DB volume에 2차 schema가 남아 있으면 인증 필드와 seed data 충돌이 발생할 수 있다.

## 운영 메모
- `archive/`는 구현 source of truth가 아니며 과거 MVP 최소 요약만 둔다.
- 3차 구현 기준은 루트 `README.md`, `docs/`, `docs/adr/008-mvp3-authenticated-user-personalization.md`, `.agents/skills/smartcloset-backend/SKILL.md`다.
- 문서 충돌 시 `docs/PRD.md`, `docs/API.md`, `docs/RECOMMENDATION_RULES.md`를 우선한다.

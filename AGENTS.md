# AGENTS.md

## 필수 읽기
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/ARCHITECTURE.md`
- `docs/FRONTEND.md`
- `docs/RECOMMENDATION_RULES.md`
- `docs/API.md`
- `docs/ERD.md`
- `docs/DEMO_SCENARIO.md`
- `docs/SHARING_GUIDE.md`
- `docs/design/mvp4/README.md`
- `docs/adr/`

## 규칙
- 구현 전 반드시 `.agents/skills/smartcloset-backend/SKILL.md`를 먼저 읽는다.
- 구현 기준 문서는 루트 `README.md`와 `docs/` 아래 현재 문서다.
- 문서가 충돌하면 `docs/PRD.md`, `docs/API.md`, `docs/RECOMMENDATION_RULES.md`를 우선한다.
- `archive/`는 과거 MVP 참고용이며 구현 source of truth가 아니다.
- `archive/`에는 MVP별 전체 문서 복사본을 두지 않고 최소 요약만 둔다.
- 현재 구현 baseline은 회원가입/로그인, Spring Security, JWT Bearer access token, 인증 사용자 기반 API, 사용자별 옷장/위치/추천 이력/착용 이력 분리, 선호도 최소 버전까지다.
- MVP4 범위는 `docs/PRD.md`와 ADR에서 승인된 뒤 구현한다.
- 공개 API는 `POST /api/auth/signup`, `POST /api/auth/login`만 둔다.
- 보호 API는 `Authorization: Bearer {accessToken}`을 요구한다.
- 프론트 access token 저장 위치는 `sessionStorage`로 고정한다.
- JWT access token은 `HS256` + `JWT_SECRET`으로 서명하고 만료 시간은 2시간으로 고정한다.
- 공개 HTTP API에서 `?userId=` query parameter를 제거한다.
- 현재 사용자 전용 response DTO에서 `userId` 필드를 제거한다.
- 추천 생성 API는 `POST /api/recommendations`만 사용한다.
- today 추천 GET 경로는 사용하지 않는다.
- 추천 이력 조회 API는 `GET /api/recommendations?limit={limit}`를 사용하며 기본 20, 최소 1, 최대 50, 최신순으로 고정한다.
- 현재 날씨 요약 API는 `GET /api/weather/current`를 사용하며 보호 API다.
- 현재 날씨 요약 API는 현재 인증 사용자 위치 기준의 `WeatherResponse`만 반환하고 추천 결과를 생성/저장하지 않는다.
- Spring Boot 버전은 `4.0.6`으로 고정한다.
- 외부 Weather API는 기상청 단기예보 `getVilageFcst` JSON 연동만 허용한다.
- 위치 선택은 외부 지도/주소 API 없이 서버 내장 대표 격자 catalog를 사용한다.
- `GET /api/locations`는 보호 API이며 로그인 후 위치 선택 흐름에서만 사용한다.
- 선호도는 `users` 테이블 JSON 문자열 컬럼 `preferred_colors_json`, `preferred_materials_json`, `style_tags_json`에 저장한다.
- 선호도 API는 `preferredColors`, `preferredMaterials`, `styleTags` 배열로 주고받는다.
- 신규 사용자의 기본 선호도는 모두 빈 배열이다.
- 기존 다양성 점수는 현재 baseline에서 `preferenceScore`로 교체했다.
- `preferenceScore`는 최대 10점이며 선호 색상 일치 5점, 선호 소재 일치 5점으로 계산한다.
- 선호 색상/소재가 모두 비어 있으면 `preferenceScore=0`이다.
- `styleTags`는 저장/조회/표시만 하고 추천 점수와 추천 이유에는 반영하지 않는다.
- 선호도 별도 테이블 정규화는 후속 MVP 후보이며 MVP4에서는 구현하지 않는다.
- MVP-3 완료 baseline 전환 시 로컬 Docker Compose DB는 `docker compose down -v` 후 `docker compose up --build`를 권장한다.
- AWS 배포, refresh token, 소셜 로그인, 이메일 인증, 비밀번호 재설정, AI/GPT 추천, 이미지 업로드, Redis는 구현하지 않는다.
- 공유 방식은 Docker Compose 기준이다.
- 커밋은 항상 Codex 앱 커밋 지침을 따른다.
- 자동 PR 루프는 clean worktree에서만 실행하고, Codex 앱 커밋/PR 지침을 따른다.
- 자동 병합은 로컬 검증과 자체 리뷰가 모두 통과한 PR에만 허용한다.
- 자동 리뷰 실패는 GitHub Issue와 `issues/{phase}/issue-N.md`에 함께 기록한다.
- 민감정보(API key, token, password, private key)는 코드와 문서에 커밋하지 않는다.

## Harness step PR 리뷰 규칙
- 완료된 MVP-3 phase 기준은 phase 전체 완료 기준이다.
- 중간 step PR 구현과 리뷰는 `phases/{phase}/README.md`와 해당 `stepN.md`의 작업, 인수 기준, 금지사항을 우선한다.
- 아직 미래 step에 배정된 기능이 없다는 이유만으로 현재 step PR을 blocker 처리하지 않는다.
- 현재 step이 미래 step 범위를 선행 구현하면 blocker로 본다.
- 리뷰 실패를 수정할 때는 현재 step 범위 안에서만 해결하고, 미래 step 기능을 구현해서 통과시키지 않는다.
- 최종 공개/보호 API 경계, 남은 `userId` 제거, `preferenceScore`, 추천 이력, 프론트 전환은 각 step 문서가 지정한 단계에서 검증한다.

## 서브에이전트
- 프로젝트 전용 서브에이전트 정의는 `.codex/agents/*.toml`을 기준으로 한다.
- 서브에이전트는 사용자가 명시적으로 요청했거나, 병렬로 안전하게 분리 가능한 작업일 때만 사용한다.
- 부모 에이전트는 작업 범위와 수정 가능/금지 경로를 명시하고, 최종 통합과 검증을 책임진다.
- 사용 가능: `smartcloset_scope_reviewer`, `smartcloset_backend_implementer`, `smartcloset_recommendation_rules_engineer`, `smartcloset_test_guardian`, `smartcloset_docs_sync`.

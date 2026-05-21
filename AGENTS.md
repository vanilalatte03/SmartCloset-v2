# AGENTS.md

## 필수 읽기
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/ARCHITECTURE.md`
- `docs/RECOMMENDATION_RULES.md`
- `docs/API.md`
- `docs/ERD.md`
- `docs/DEMO_SCENARIO.md`
- `docs/SHARING_GUIDE.md`
- `docs/adr/`

## 규칙
- 구현 전 반드시 `.agents/skills/smartcloset-backend/SKILL.md`를 먼저 읽는다.
- 문서가 충돌하면 `docs/PRD.md`, `docs/API.md`, `docs/RECOMMENDATION_RULES.md`를 우선한다.
- 1차 MVP 범위를 넘는 기능을 임의로 구현하지 않는다.
- Spring Boot 버전은 `4.0.6`으로 고정한다.
- 외부 Weather API, AWS 배포, 로그인/회원가입, AI/GPT 추천, 이미지 업로드, Redis는 구현하지 않는다.
- 추천 생성 API는 `POST /api/recommendations?userId={userId}`만 사용한다.
- `GET /api/recommendations/today`는 사용하지 않는다.
- 공유 방식은 Docker Compose 기준이다.
- 커밋은 항상 Codex 앱 커밋 지침을 따른다.
- 민감정보(API key, token, password, private key)는 코드와 문서에 커밋하지 않는다.

## 서브에이전트
- 프로젝트 전용 서브에이전트 정의는 `.codex/agents/*.toml`을 기준으로 한다.
- 서브에이전트는 사용자가 명시적으로 요청했거나, 병렬로 안전하게 분리 가능한 작업일 때만 사용한다.
- 부모 에이전트는 작업 범위와 수정 가능/금지 경로를 명시하고, 최종 통합과 검증을 책임진다.
- 사용 가능: `smartcloset_scope_reviewer`, `smartcloset_backend_implementer`, `smartcloset_recommendation_rules_engineer`, `smartcloset_test_guardian`, `smartcloset_docs_sync`.

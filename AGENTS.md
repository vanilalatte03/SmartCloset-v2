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
- `docs/adr/`

## 규칙
- 구현 전 반드시 `.agents/skills/smartcloset-backend/SKILL.md`를 먼저 읽는다.
- 구현 기준 문서는 루트 `README.md`와 `docs/` 아래 현재 문서다.
- 문서가 충돌하면 `docs/PRD.md`, `docs/API.md`, `docs/RECOMMENDATION_RULES.md`를 우선한다.
- `archive/`는 과거 MVP 참고용이며 구현 source of truth가 아니다.
- `archive/`에는 MVP별 전체 문서 복사본을 두지 않고 최소 요약만 둔다.
- 승인된 2차 범위는 사용자 위치 저장, 내장 대표 격자 위치 선택 API, React+Vite+TypeScript 프론트엔드 앱까지로 제한한다.
- `frontend/`가 없는 상태에서 2차 프론트를 구현할 때는 스캐폴드와 Docker Compose `frontend` 서비스를 같은 구현 흐름에서 함께 추가한다.
- Spring Boot 버전은 `4.0.6`으로 고정한다.
- 외부 Weather API는 기상청 단기예보 `getVilageFcst` JSON 연동만 허용한다.
- 위치 선택은 외부 지도/주소 API 없이 서버 내장 대표 격자 catalog를 사용한다.
- AWS 배포, 로그인/회원가입, AI/GPT 추천, 이미지 업로드, Redis는 구현하지 않는다.
- 추천 생성 API는 `POST /api/recommendations?userId={userId}`만 사용한다.
- today 추천 GET 경로는 사용하지 않는다.
- 공유 방식은 Docker Compose 기준이다.
- 커밋은 항상 Codex 앱 커밋 지침을 따른다.
- 자동 PR 루프는 clean worktree에서만 실행하고, Codex 앱 커밋/PR 지침을 따른다.
- 자동 병합은 로컬 검증과 자체 리뷰가 모두 통과한 PR에만 허용한다.
- 자동 리뷰 실패는 GitHub Issue와 `issues/{phase}/issue-N.md`에 함께 기록한다.
- 민감정보(API key, token, password, private key)는 코드와 문서에 커밋하지 않는다.

## 서브에이전트
- 프로젝트 전용 서브에이전트 정의는 `.codex/agents/*.toml`을 기준으로 한다.
- 서브에이전트는 사용자가 명시적으로 요청했거나, 병렬로 안전하게 분리 가능한 작업일 때만 사용한다.
- 부모 에이전트는 작업 범위와 수정 가능/금지 경로를 명시하고, 최종 통합과 검증을 책임진다.
- 사용 가능: `smartcloset_scope_reviewer`, `smartcloset_backend_implementer`, `smartcloset_recommendation_rules_engineer`, `smartcloset_test_guardian`, `smartcloset_docs_sync`.

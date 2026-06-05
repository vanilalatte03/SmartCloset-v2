# AGENTS.md

## 역할
- 이 파일은 Codex 작업 라우터다. 제품/기술 계약의 본문은 루트 `README.md`와 `docs/` 아래 현재 문서를 따른다.
- `README.md`는 백엔드 프로젝트 소개, 기술 스택, 도메인/아키텍처 요약, 주요 결정 이유, 실행 안내, 현재 MVP 포인터를 담당한다. MVP별 상세 계약은 전용 `docs/` 문서를 우선한다.
- 구현 세부 규칙을 이 파일에 복사하지 않는다. 세부 규칙이 필요하면 아래 SSOT 문서를 확인한다.
- `archive/`는 과거 MVP 참고용이며 구현 source of truth가 아니다. `archive/`에는 MVP별 전체 문서 복사본을 두지 않고 최소 요약만 둔다.

## 구현 전 필수
- 작업 시작 시 사용자 요청과 변경 파일 기준으로 백엔드/프론트/문서 범위를 먼저 분류한다.
- 백엔드 구현, 리뷰, 문서 동기화 또는 HTTP API, 인증, 추천, 날씨, 위치, DB/entity, 옷 이미지 저장소 변경이 포함되면 `.agents/skills/smartcloset-backend/SKILL.md`를 먼저 읽는다.
- 프론트 전용 UI/UX 구현, 리뷰, 문서 동기화는 백엔드 스킬을 강제하지 않고 `docs/FRONTEND.md`를 먼저 확인한다.
- 프론트 작업이 API 계약, 인증 흐름, 보호 이미지 fetch, DTO shape, 백엔드 파일 변경을 함께 포함하면 관련 SSOT 문서와 `.agents/skills/smartcloset-backend/SKILL.md`를 함께 확인한다.
- 현재 baseline은 MVP9 프론트 UI/UX 리디자인 문서 전환 상태다. MVP9 계약은 `docs/PRD.md`와 ADR-014를 따르고, 옷장 보관함 복원 확장은 ADR-015를 따른다.
- 별도 활성 phase/step이 없으면 새 작업 범위는 사용자 요청과 현재 `README.md`, `docs/` 기준으로 정한다.
- phase/step 작업은 `phases/{phase}/README.md`와 해당 `stepN.md`의 작업, 인수 기준, 금지사항을 함께 따른다.

## SSOT 문서 지도
| 영역 | 기준 문서 |
| --- | --- |
| 제품 목표, MVP 범위, 포함/제외 | `docs/PRD.md` |
| HTTP API, 인증 경계, DTO, 에러 코드 | `docs/API.md` |
| 추천 후보, 점수, tie-break, 추천 이유 | `docs/RECOMMENDATION_RULES.md` |
| 백엔드 구조, 저장소, 트랜잭션, 금지 패턴 | `docs/ARCHITECTURE.md` |
| DB schema, entity/JPA 기준 | `docs/ERD.md` |
| 프론트 타입, API client, UX, 반응형 기준 | `docs/FRONTEND.md` |
| MVP9 프론트 UI/UX 기준 | `docs/FRONTEND.md` |
| 데모와 수동 검증 | `docs/DEMO_SCENARIO.md` |
| Docker Compose 공유와 환경변수 | `docs/SHARING_GUIDE.md` |
| 실행 명령과 검증 명령 | `docs/COMMANDS.md` |
| MVP 변경 시 확인할 문서/스크립트/서브에이전트 체크리스트 | `docs/MVP_CHANGE_CHECKLIST.md` |
| 결정 기록과 변경 이력 | `docs/ADR.md`, `docs/adr/` |

## 문서 충돌 해석
- 제품 범위 충돌은 `docs/PRD.md`를 우선한다.
- HTTP 계약 충돌은 `docs/API.md`를 우선한다.
- 추천 규칙 충돌은 `docs/RECOMMENDATION_RULES.md`를 우선한다.
- 구조/DB/프론트 충돌은 각각 `docs/ARCHITECTURE.md`, `docs/ERD.md`, `docs/FRONTEND.md`를 우선한다.
- 오래된 ADR, phase 문서, archive 내용이 현재 문서와 충돌하면 현재 `README.md`와 `docs/` 기준을 우선한다.
- 완료된 phase 문서는 과거 실행 기록이며, 현재 구현 source of truth를 override하지 않는다.
- `docs/DEMO_SCENARIO.md`, `docs/SHARING_GUIDE.md`, 프론트 UX 문서는 검증과 사용 흐름 기준이며, API/DB/추천 계약을 override하지 않는다.

## Codex 작업 규칙
- 변경은 현재 요청과 현재 phase/step 범위 안에서만 수행한다.
- 동작이 바뀌면 관련 SSOT 문서도 함께 확인하고 필요한 경우 동기화한다.
- 공개 HTTP API와 현재 사용자 전용 DTO는 `docs/API.md` 기준을 따르며, 과거 테스트용 `userId` query parameter/field를 되살리지 않는다.
- Token/action token 원문 저장·노출 금지 기준은 `docs/API.md`와 `.agents/skills/smartcloset-backend/SKILL.md`를 따른다.
- 추천은 `docs/RECOMMENDATION_RULES.md` 기준의 규칙 기반 추천으로 유지하며, 현재 범위 밖 AI/GPT 추천, 이미지 기반 추천 점수, 자동 태깅을 추가하지 않는다.
- 민감정보(API key, token, password, private key)는 코드와 문서에 커밋하지 않는다.
- 커밋과 PR은 Codex 앱의 한국어 Conventional Commits / PR 작성 지침을 따른다.
- 자동 PR 루프는 clean worktree에서만 실행한다.
- 자동 병합은 로컬 검증과 자체 리뷰가 모두 통과한 PR에만 허용한다.
- 자동 리뷰 실패는 GitHub Issue와 `issues/{phase}/issue-N.md`에 함께 기록한다.

## Codex Reasoning Effort Policy
- 기본 구현, 문서 수정, 작은 UI 문구 변경은 `medium`을 사용한다.
- Plan Mode, PR self-review, scope review는 `high`를 사용한다.
- `xhigh`는 추천 규칙 변경, 인증/인가 구조 변경, JPA 성능 개선, 대규모 리팩토링, 머지 전 최종 범위 감사에만 사용한다.
- Harness 자동 실행은 `scripts/execute.py`와 `scripts/autopilot.py`의 effort 옵션을 우선한다.

## Harness step PR 리뷰 규칙
- 완료 기준은 phase 전체 완료 기준이다.
- 중간 step PR은 해당 step 문서의 작업, 인수 기준, 금지사항을 우선한다.
- 미래 step 기능이 현재 step에 없다는 이유만으로 blocker 처리하지 않는다.
- 현재 step이 미래 step 범위를 선행 구현하면 blocker로 본다.
- 리뷰 실패 수정은 현재 step 범위 안에서 해결한다.

## 서브에이전트
- 프로젝트 전용 서브에이전트 정의는 `.codex/agents/*.toml`을 기준으로 한다.
- 서브에이전트는 사용자가 명시적으로 요청했거나, 병렬로 안전하게 분리 가능한 작업일 때만 사용한다.
- 부모 에이전트는 작업 범위와 수정 가능/금지 경로를 명시하고, 최종 통합과 검증을 책임진다.
- 사용 가능: `smartcloset_scope_reviewer`, `smartcloset_backend_implementer`, `smartcloset_recommendation_rules_engineer`.

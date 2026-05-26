# MVP 변경 체크리스트

## 목적

이 문서는 SmartCloset의 새 MVP를 기획하거나 현재 baseline을 전환할 때 누락을 줄이기 위한 운영 체크리스트다.

이 문서는 제품 계약의 source of truth가 아니다. 제품 범위는 `docs/PRD.md`, API 계약은 `docs/API.md`, 추천 규칙은 `docs/RECOMMENDATION_RULES.md`, 구조/DB/프론트 계약은 각 전용 문서를 우선한다.

## 사용 시점

- 새 MVP 또는 새 phase를 기획할 때
- 현재 baseline 문구가 바뀔 때
- 포함/제외 범위, API, DB, 추천 규칙, 공유 방식이 바뀔 때
- Harness `docs-checks.json`, 스크립트 예시, 서브에이전트 규칙이 현재 MVP와 맞는지 확인할 때

## 기본 원칙

- 완료된 phase 문서와 `archive/`는 과거 실행 기록이다. 현재 구현 기준은 루트 `README.md`와 `docs/` 아래 현재 문서다.
- 오래된 ADR을 새 결정처럼 고치지 않는다. 새 MVP 결정은 새 ADR 파일을 만들고 `docs/ADR.md`에 연결한다.
- MVP별 정규식 검증은 `scripts/checks.py`에 넣지 않는다. 새 phase의 `phases/{phase}/docs-checks.json`에 둔다.
- `docs-checks.json`은 자동으로 잡을 수 있는 핵심 회귀 신호만 담는다. 에이전트가 읽고 판단해야 하는 정성 규칙은 `.agents/skills/smartcloset-backend/SKILL.md`와 현재 문서에 둔다.
- 서브에이전트는 부모 에이전트가 범위와 수정 가능/금지 경로를 지정할 때만 사용한다. 최종 통합과 검증 책임은 부모 에이전트에 있다.

## 항상 확인할 파일

| 파일 | 확인할 내용 |
| --- | --- |
| `README.md` | 현재 MVP 한 줄 설명, 현재 baseline, 포함/제외 범위 요약, API 요약, 실행/검증 요약, 문서 기준 표 |
| `docs/PRD.md` | MVP 정의, 목표, 포함/제외, P0/P1 우선순위, 완료 기준, 현재 baseline |
| `docs/ADR.md`, `docs/adr/{NNN}-*.md` | 새 MVP 결정 ADR 추가, ADR index 연결, 이전 ADR과의 관계 |
| `docs/API.md` | 공개/보호 API, 인증 경계, DTO, 에러 코드, 폐기된 endpoint 제거 |
| `docs/RECOMMENDATION_RULES.md` | 추천 후보, 점수, tie-break, 추천 이유, 추천 이력 영향 |
| `docs/ARCHITECTURE.md` | 모듈 구조, application/domain/repository 경계, transaction, storage/provider 정책 |
| `docs/ERD.md` | schema, entity/JPA 기준, migration 또는 로컬 schema 전환 기준 |
| `docs/FRONTEND.md` | API client 타입, 상태 관리, UX 흐름, 반응형 기준 |
| `docs/DEMO_SCENARIO.md` | 새 MVP 데모 흐름, 수동 QA 경로, 폐기된 데모 제거 |
| `docs/SHARING_GUIDE.md` | Docker Compose 공유 방식, 환경변수, volume, reset/재시작 흐름 |
| `docs/COMMANDS.md` | 활성 명령, phase/autopilot 예시, 최종 검증 명령, docs-check 설명 |
| `docs/design/mvp{N}/` | 새 MVP 화면 설계가 필요할 때 디자인 기준과 참고 이미지 |
| `docs/qa/` | 수동 QA 기록 또는 새 MVP 검증 기록 |

## 에이전트 규칙

| 파일 | 확인할 내용 |
| --- | --- |
| `AGENTS.md` | 현재 baseline 문구, SSOT 문서 지도, 문서 충돌 우선순위, Harness step PR 리뷰 규칙, 사용 가능한 서브에이전트 목록 |
| `.agents/skills/smartcloset-backend/SKILL.md` | `Current Execution Baseline`, `Historical Context`, `Strict Out of Scope`, 변경 영역별 Rules, `Documentation Sync Rules` |
| `.agents/skills/harness/SKILL.md` | phase 설계 방식, `docs-checks.json` 작성 규칙, `execute.py`/`autopilot.py` 운영 방식 |
| `.agents/skills/review/SKILL.md` | 리뷰 기준 자체가 바뀌는 경우의 체크리스트와 읽기 문서 |

## 서브에이전트 규칙

| 파일 | 확인할 내용 |
| --- | --- |
| `.codex/agents/smartcloset_scope_reviewer.toml` | 현재 MVP baseline, 승인/금지 범위, 반드시 읽을 문서, 범위 리뷰 관점 |
| `.codex/agents/smartcloset_backend_implementer.toml` | 백엔드 소유 경로, 수정 가능 범위, 금지 기능, 검증 기준 |
| `.codex/agents/smartcloset_recommendation_rules_engineer.toml` | 추천/날씨 도메인 소유 범위, 점수 체계, 금지 변경, 집중 테스트 기준 |

새 MVP에서 서브에이전트가 더 필요하거나 역할이 바뀌면 `.codex/agents/*.toml`을 먼저 갱신하고, `AGENTS.md`의 사용 가능 목록도 함께 맞춘다.

## Harness phase 파일

새 MVP를 phase로 실행할 때는 아래를 함께 만든다.

| 파일 | 확인할 내용 |
| --- | --- |
| `phases/index.json` | 새 phase dir 추가와 상태 |
| `phases/{phase}/README.md` | phase 목표, 범위, step 목록, 완료 기준, 검증 명령 |
| `phases/{phase}/index.json` | step 순서, 이름, 상태 |
| `phases/{phase}/step{N}.md` | 읽어야 할 파일, 작업, 인수 기준, 검증 절차, 금지사항 |
| `phases/{phase}/docs-checks.json` | final stage에서 검증할 required/forbidden 문서 마커 |

`docs-checks.json`의 `paths`에는 현재 phase에서 실제로 확인해야 하는 경로만 넣는다. MVP 관련 금지 기능, 폐기 endpoint, 서브에이전트 충돌 가능성은 이 파일의 `forbidden` 규칙으로 잡는다.

## 스크립트 확인 기준

MVP가 바뀌었다는 이유만으로 `scripts/*.py`를 먼저 수정하지 않는다.

| 파일 | 수정하는 경우 |
| --- | --- |
| `scripts/checks.py` | docs-check 엔진, stage 처리, command discovery 자체가 바뀔 때 |
| `scripts/execute.py` | branch/commit/final 검증 같은 phase 실행 workflow가 바뀔 때 |
| `scripts/autopilot.py` | PR 생성, 자체 리뷰, issue 기록, merge loop workflow가 바뀔 때 |
| `scripts/test_*.py` | 위 스크립트 동작을 바꾼 경우 |
| `.codex/project-profile.json` | lint/test/build 명령 자체를 프로젝트 프로필로 바꾸는 경우 |

MVP별 문서 검증 규칙은 `scripts/checks.py`가 아니라 `phases/{phase}/docs-checks.json`에 둔다.

## 영역별 추가 확인

| 변경 영역 | 추가 확인 |
| --- | --- |
| 인증/인가 | `docs/API.md`, `docs/ARCHITECTURE.md`, `docs/ERD.md`, `.agents/skills/smartcloset-backend/SKILL.md`, 관련 서브에이전트 금지사항 |
| API/DTO | `docs/API.md`, `docs/FRONTEND.md`, `README.md`, `docs/DEMO_SCENARIO.md`, frontend type/client |
| 추천 규칙 | `docs/RECOMMENDATION_RULES.md`, `docs/API.md`, `docs/ERD.md`, recommendation subagent, 추천 도메인 테스트 |
| DB/entity | `docs/ERD.md`, `docs/ARCHITECTURE.md`, backend subagent, repository/entity 테스트 |
| 프론트 UX | `docs/FRONTEND.md`, `docs/design/mvp{N}/`, `docs/DEMO_SCENARIO.md`, `docs/SHARING_GUIDE.md` |
| Docker/공유 | `docs/SHARING_GUIDE.md`, `docs/COMMANDS.md`, `docker-compose.yml`, `.env.example` |
| 운영 자동화 | `docs/COMMANDS.md`, `.agents/skills/harness/SKILL.md`, `phases/{phase}/docs-checks.json` |

## 검색 체크

새 MVP 문서 전환 후에는 오래된 MVP 표현과 폐기 계약을 검색한다.

```bash
rg -n "MVP5|MVP4|5-smartcloset|clothing-images" README.md AGENTS.md docs .agents .codex/agents phases scripts
rg -n "userId|today|AI/GPT|S3|CDN|Redis|refresh token" README.md AGENTS.md docs .agents .codex/agents phases
```

위 명령의 검색어는 새 MVP에 맞게 바꾼다. 완료된 phase, 오래된 ADR, QA 기록처럼 과거 맥락이 명확한 파일의 과거 표현은 남길 수 있지만, 현재 baseline 문서와 에이전트 규칙 안의 충돌 표현은 제거하거나 현재 기준으로 바꾼다.

## 최종 검증

문서만 바꿨더라도 최소한 아래를 확인한다.

```bash
git diff --check
python3 scripts/checks.py --docs-check
```

새 phase를 막 설계한 상태라면 해당 phase rule 파일을 직접 지정해 확인한다.

```bash
python3 scripts/checks.py --docs-check-config phases/{phase}/docs-checks.json --docs-check
```

코드나 실행 흐름까지 바뀐 MVP라면 `docs/COMMANDS.md`의 P0 공유 검증 명령과 phase final 검증을 함께 실행한다.

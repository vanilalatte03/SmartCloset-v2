# 단계 0: mvp8-scope-docs-archive

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/MVP_CHANGE_CHECKLIST.md`
- `README.md`
- `docs/PRD.md`
- `docs/API.md`
- `docs/ADR.md`
- `docs/adr/013-mvp8-account-stability.md`
- `phases/8-smartcloset-account-stability/README.md`
- `phases/8-smartcloset-account-stability/docs-checks.json`

## 작업

- 이 단계는 문서 전환 확인 전용이다.
- `archive/mvp-7/` 최소 archive가 현재 MVP7 요약과 링크를 포함하는지 확인한다.
- `README.md`, `AGENTS.md`, `.agents/skills/smartcloset-backend/SKILL.md`, `.codex/agents/*.toml`의 현재 baseline이 MVP8과 ADR-013을 가리키는지 확인한다.
- `docs/PRD.md`, `docs/API.md`, `docs/ARCHITECTURE.md`, `docs/ERD.md`, `docs/FRONTEND.md`, `docs/RECOMMENDATION_RULES.md`, `docs/DEMO_SCENARIO.md`, `docs/SHARING_GUIDE.md`, `docs/COMMANDS.md`가 MVP8 계정 안정성 계약과 충돌하지 않는지 확인한다.
- `phases/8-smartcloset-account-stability/docs-checks.json`이 MVP8 핵심 회귀 신호만 검사하는지 확인한다.
- 누락된 MVP8 문서 마커가 있으면 문서만 수정한다.

## 인수 기준

```bash
git diff --check
python3 scripts/checks.py --docs-check-config phases/8-smartcloset-account-stability/docs-checks.json --docs-check
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. 아키텍처 체크리스트를 확인한다:
   - 현재 문서 baseline이 MVP8과 ADR-013을 가리키는가?
   - MVP7 archive는 과거 맥락으로만 남았는가?
   - AWS 구현이 MVP8 포함 범위로 문서화되지 않았는가?
   - refresh token 원문이 JSON 응답이나 DB 저장 대상으로 문서화되지 않았는가?
3. 결과에 따라 `phases/8-smartcloset-account-stability/index.json`의 해당 단계를 업데이트한다:
   - 성공 -> `"status": "completed"`, `"summary": "MVP8 문서 전환과 MVP7 archive, agent baseline, docs-check 회귀 신호를 확인했다."`
   - 수정 3회 시도 후에도 실패 -> `"status": "error"`, `"error_message": "구체적 에러 내용"`
   - 사용자 개입 필요 -> `"status": "blocked"`, `"blocked_reason": "구체적 사유"` 후 즉시 중단

검증 또는 리뷰가 통과하지 못하면 `issues/8-smartcloset-account-stability/issue-N.md`에 재현 명령, 핵심 에러, 수정 방향을 기록하고 fix step을 추가한다.

## 금지사항

- 백엔드 또는 프론트 구현 코드를 수정하지 마라. 이유: 이 단계는 MVP8 문서 전환 확인만 담당한다.
- AWS 배포 구현을 포함 범위로 바꾸지 마라. 이유: MVP8은 AWS-ready adapter boundary만 준비한다.
- refresh token 원문을 JSON 응답에 포함한다고 문서화하지 마라. 이유: refresh token은 HttpOnly cookie 전용이다.
- 기존 테스트를 깨뜨리지 마라.

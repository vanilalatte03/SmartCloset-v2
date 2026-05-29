# 단계 0: mvp9-docs-archive

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/MVP_CHANGE_CHECKLIST.md`
- `docs/PRD.md`
- `docs/FRONTEND.md`
- `docs/ADR.md`
- `docs/adr/014-mvp9-ui-ux-redesign.md`
- `docs/design/mvp9/README.md`

## 작업

- MVP9 문서 전환 결과가 현재 docs와 agent 규칙에 일관되게 반영됐는지 확인한다.
- MVP8 계정 안정성 archive가 `archive/mvp-8/`에 최소 요약으로 남았는지 확인한다.
- `docs/design/mvp9/`가 `tmp/design-preview` 기준 reference를 보관하는지 확인한다.
- `phases/9-smartcloset-ui-ux-redesign/docs-checks.json`이 MVP9 핵심 회귀 신호만 검사하는지 확인한다.

## 인수 기준

```bash
git diff --check
python3 scripts/checks.py --docs-check-config phases/9-smartcloset-ui-ux-redesign/docs-checks.json --docs-check
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. 문서 전환 체크리스트를 확인한다:
   - 현재 문서 baseline이 MVP9와 ADR-014를 가리키는가?
   - MVP8 archive가 현재 source of truth처럼 보이지 않는가?
   - AWS 배포가 MVP9 포함 범위로 문서화되지 않았는가?
   - API/DB/추천 규칙 변경이 MVP9 요구사항으로 추가되지 않았는가?
3. 결과에 따라 `phases/9-smartcloset-ui-ux-redesign/index.json`의 해당 단계를 업데이트한다:
   - 성공 -> `"status": "completed"`, `"summary": "MVP9 문서 전환과 MVP8 archive, 디자인 reference, docs-check 회귀 신호를 확인했다."`
   - 수정 3회 시도 후에도 실패 -> `"status": "error"`, `"error_message": "구체적 에러 내용"`
   - 사용자 개입 필요 -> `"status": "blocked"`, `"blocked_reason": "구체적 사유"` 후 즉시 중단

## 금지사항

- 백엔드 또는 프론트 구현 코드를 수정하지 마라. 이유: 이 단계는 MVP9 문서 전환 확인만 담당한다.
- `archive/` 문서를 현재 구현 source of truth처럼 링크하지 마라. 이유: 현재 기준은 루트 `README.md`와 `docs/`다.
- AWS 배포를 MVP9 구현 범위로 되살리지 마라. 이유: MVP9는 UI/UX 리디자인 MVP다.

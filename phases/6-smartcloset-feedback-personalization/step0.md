# 단계 0: mvp6-scope-docs

## 읽어야 할 파일

먼저 아래 파일들을 읽고 MVP6 문서 전환 범위를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/MVP_CHANGE_CHECKLIST.md`
- `docs/PRD.md`
- `docs/API.md`
- `docs/RECOMMENDATION_RULES.md`
- `docs/ADR.md`
- `phases/6-smartcloset-feedback-personalization/README.md`

## 작업

MVP5를 archive로 보내고 MVP6 문서 기준을 확정한다.

- `archive/mvp-5/` 최소 요약 문서를 유지한다.
- `docs/adr/011-mvp6-feedback-personalization.md`와 `docs/ADR.md` 연결을 확인한다.
- README와 `docs/`의 현재 기준이 MVP6 추천 피드백/개인화로 일관되는지 확인한다.
- `AGENTS.md`, `.agents/skills/smartcloset-backend/SKILL.md`, `.codex/agents/*.toml`이 MVP6 baseline과 phase step 규칙을 설명하는지 확인한다.
- `phases/6-smartcloset-feedback-personalization/docs-checks.json`이 MVP6 핵심 문서 회귀만 검사하는지 확인한다.

## 인수 기준

```bash
git diff --check
python3 scripts/checks.py --docs-check-config phases/6-smartcloset-feedback-personalization/docs-checks.json --docs-check
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. `rg -n 'styleTags.*(반영하지 않는다|사용하지 않는다)|/api/recommendations/today|POST /api/recommendations\?userId|/api/clothes\?userId|/api/users/location\?userId' README.md AGENTS.md docs .agents .codex/agents phases/6-smartcloset-feedback-personalization`로 폐기 계약 충돌을 확인한다.
   - MVP5 완료 baseline, 이미지 계약 유지, archive 링크 문맥의 `MVP5` 표현은 허용한다.
3. phase index의 현재 step 상태를 업데이트한다.

## 금지사항

- 코드 구현을 시작하지 마라. 이유: Step 0은 문서 전환과 phase 정의만 다룬다.
- 완료된 MVP5 phase 문서를 현재 기준처럼 고치지 마라. 이유: 완료 phase는 과거 실행 기록이다.
- 새 MVP 결정을 오래된 ADR-010에 덮어쓰지 마라. 이유: MVP6 결정은 ADR-011에서 관리한다.

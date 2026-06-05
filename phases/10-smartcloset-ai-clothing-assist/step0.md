# 단계 0: mvp10-docs-archive

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/MVP_CHANGE_CHECKLIST.md`
- `docs/PRD.md`
- `docs/API.md`
- `docs/ARCHITECTURE.md`
- `docs/FRONTEND.md`
- `docs/RECOMMENDATION_RULES.md`
- `docs/ADR.md`
- `docs/adr/016-mvp10-ai-clothing-registration-assist.md`
- `archive/mvp-9/README.md`
- `phases/10-smartcloset-ai-clothing-assist/docs-checks.json`

## 작업

- MVP10 문서 전환 결과가 현재 docs와 agent 규칙에 일관되게 반영됐는지 확인한다.
- MVP9 UI/UX 리디자인 archive가 `archive/mvp-9/`에 최소 요약으로 남았는지 확인한다.
- ADR-016이 Spring AI 2.0 preview, `gpt-5.4-nano`, 비용 가드, 추천 비개입 경계를 명확히 설명하는지 확인한다.
- `phases/10-smartcloset-ai-clothing-assist/docs-checks.json`이 MVP10 핵심 회귀 신호만 검사하는지 확인한다.
- 이 step의 변경 대상은 문서와 phase metadata로 제한한다: `README.md`, `AGENTS.md`, `.agents/skills/**`, `.codex/agents/**`, `docs/**`, `archive/mvp-9/**`, `phases/**`.

## 인수 기준

```bash
git diff --check
python3 scripts/checks.py --docs-check-config phases/10-smartcloset-ai-clothing-assist/docs-checks.json --docs-check
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. 문서 전환 체크리스트를 확인한다:
   - 현재 문서 baseline이 MVP10과 ADR-016을 가리키는가?
   - MVP9 archive가 현재 source of truth처럼 보이지 않는가?
   - AI는 옷 등록 후보에만 쓰이고 추천에는 쓰이지 않는다고 문서화됐는가?
   - DB schema 변경과 분석 결과 저장이 MVP10 요구사항으로 추가되지 않았는가?
3. 결과에 따라 `phases/10-smartcloset-ai-clothing-assist/index.json`의 해당 단계를 업데이트한다:
   - 성공 -> `"status": "completed"`, `"summary": "MVP10 문서 전환과 MVP9 archive, ADR-016, docs-check 회귀 신호를 확인했다."`
   - 수정 3회 시도 후에도 실패 -> `"status": "error"`, `"error_message": "구체적 에러 내용"`
   - 사용자 개입 필요 -> `"status": "blocked"`, `"blocked_reason": "구체적 사유"` 후 즉시 중단

## 금지사항

- 백엔드 또는 프론트 구현 코드를 수정하지 마라. 이유: 이 단계는 MVP10 문서 전환 확인만 담당한다.
- `build.gradle`, `src/**`, `.env.example`, `docker-compose.yml`, `frontend/**`를 수정하지 마라. 이유: Spring AI 의존성, 설정, 구현 변경은 Step 1 이후 범위다.
- `archive/` 문서를 현재 구현 source of truth처럼 링크하지 마라. 이유: 현재 기준은 루트 `README.md`와 `docs/`다.
- AI/GPT 옷차림 추천을 MVP10 범위로 되살리지 마라. 이유: MVP10은 옷 등록 후보 보조 MVP다.

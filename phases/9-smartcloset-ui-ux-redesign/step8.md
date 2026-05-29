# 단계 8: docs-qa

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/MVP_CHANGE_CHECKLIST.md`
- `docs/PRD.md`
- `docs/FRONTEND.md`
- `docs/DEMO_SCENARIO.md`
- `docs/SHARING_GUIDE.md`
- `docs/COMMANDS.md`
- `docs/qa/mvp9-ui-ux-redesign-qa.md` (없으면 생성)
- `phases/9-smartcloset-ui-ux-redesign/docs-checks.json`

## 작업

- MVP9 구현 결과와 SSOT 문서가 일치하는지 최종 동기화한다.
- `docs/qa/mvp9-ui-ux-redesign-qa.md`에 브라우저 기반 QA 결과를 기록한다.
- Codex Browser를 우선 사용하고, 필요하면 Chrome 또는 Computer Use로 대체해 데스크톱 1440px과 모바일 390px에서 Auth, 추천, 옷장, 내 취향, 위치, 기록, 계정 설정 화면을 확인한다.
- QA 기록은 아래 14개 케이스를 모두 포함하며, 각 행에는 viewport, 화면명, 결과 `PASS`, 확인 도구, 확인 메모를 남긴다.
  - `desktop 1440px`: Auth, 추천, 옷장, 내 취향, 위치, 기록, 계정 설정
  - `mobile 390px`: Auth, 추천, 옷장, 내 취향, 위치, 기록, 계정 설정
- API/DB/추천 규칙 변경이 문서나 코드에 섞이지 않았는지 확인한다.
- Docker Compose 공유 흐름 문서가 여전히 local 실행 기준인지 확인한다.

## 인수 기준

```bash
git diff --check
python3 scripts/checks.py --stage manual
python3 -m pytest scripts/test_checks.py scripts/test_execute.py scripts/test_autopilot.py scripts/test_guard.py
python3 scripts/checks.py --docs-check-config phases/9-smartcloset-ui-ux-redesign/docs-checks.json --docs-check
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. 최종 체크리스트를 확인한다:
   - MVP9 포함/제외 범위가 문서와 구현에 일치하는가?
   - primary nav와 account 진입 계약이 문서와 구현에 일치하는가?
   - MVP8 계정 안정성 기능이 유지되는가?
   - AWS 구현이 MVP9에 들어오지 않았는가?
   - 공개 `userId` query parameter, today 추천 GET, refresh token JSON 노출이 없는가?
   - 1440px 데스크톱과 390px 모바일에서 7개 화면의 겹침/잘림 QA가 `PASS`로 기록되었는가?
3. 결과에 따라 `phases/9-smartcloset-ui-ux-redesign/index.json`의 해당 단계를 업데이트한다:
   - 성공 -> `"status": "completed"`, `"summary": "MVP9 최종 docs-check와 UI/UX QA 기록을 정리했다."`
   - 수정 3회 시도 후에도 실패 -> `"status": "error"`, `"error_message": "구체적 에러 내용"`
   - 사용자 개입 필요 -> `"status": "blocked"`, `"blocked_reason": "구체적 사유"` 후 즉시 중단

## 금지사항

- MVP9 범위를 넘는 AWS 배포나 백엔드 계약 변경을 구현하지 마라. 이유: Step 8은 최종 동기화와 검증 단계다.
- 실패한 QA를 기록 없이 통과 처리하지 마라. 이유: MVP9의 핵심은 시각/반응형 완성도 검증이다.
- 브라우저 자동화 없이 추정으로 QA `PASS`를 기록하지 마라. 이유: final docs-check는 기록 형식만 강제하므로 실제 확인 책임은 Step 8 실행자에게 있다.
- `scripts/checks.py`에 MVP9 전용 규칙을 추가하지 마라. 이유: MVP별 규칙은 phase-local `docs-checks.json`에 둔다.

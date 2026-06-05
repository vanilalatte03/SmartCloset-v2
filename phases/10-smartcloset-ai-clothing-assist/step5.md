# 단계 5: docs-qa-final

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
- `docs/DEMO_SCENARIO.md`
- `docs/SHARING_GUIDE.md`
- `docs/COMMANDS.md`
- `docs/qa/mvp10-ai-clothing-assist-qa.md` (없으면 생성)
- `phases/10-smartcloset-ai-clothing-assist/docs-checks.json`

## 작업

- MVP10 구현 결과와 SSOT 문서가 일치하는지 최종 동기화한다.
- `docs/qa/mvp10-ai-clothing-assist-qa.md`에 브라우저와 API QA 결과를 기록한다.
- Codex Browser를 우선 사용하고, 필요하면 Chrome 또는 Computer Use로 대체해 데스크톱 1440px과 모바일 390px에서 옷장 AI 후보 체크 흐름을 확인한다.
- QA 기록은 아래 케이스를 포함하며, 각 행에는 viewport, 화면명, 결과 `PASS`, 확인 도구, 확인 메모를 남긴다.
  - `desktop 1440px`: 옷장 AI 후보 체크
  - `mobile 390px`: 옷장 AI 후보 체크
  - `backend API`: analysis cases
  - `recommendation`: AI 분석 전후 추천 불변
- API/DB/추천 규칙 변경이 문서나 코드에 섞이지 않았는지 확인한다.
- Docker Compose 공유 흐름 문서가 기본 비활성 AI 분석 기준에서도 실행 가능한지 확인한다.

## 인수 기준

```bash
git diff --check
./gradlew test
./gradlew build
(cd frontend && npm run build)
docker compose config --quiet
python3 scripts/checks.py --docs-check-config phases/10-smartcloset-ai-clothing-assist/docs-checks.json --docs-check --include-final-docs
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. 최종 체크리스트를 확인한다:
   - MVP10 포함/제외 범위가 문서와 구현에 일치하는가?
   - 분석 기능 비활성 상태에서도 기존 앱이 동작하는가?
   - API key, refresh token, action token 원문이 노출되지 않는가?
   - AI 분석 결과가 추천 점수/후보/이유/이력에 관여하지 않는가?
   - 1440px 데스크톱과 390px 모바일에서 옷 등록 AI 후보 체크 QA가 `PASS`로 기록되었는가?
3. 결과에 따라 `phases/10-smartcloset-ai-clothing-assist/index.json`의 해당 단계를 업데이트한다:
   - 성공 -> `"status": "completed"`, `"summary": "MVP10 최종 docs-check와 AI 옷 등록 보조 QA 기록을 정리했다."`
   - 수정 3회 시도 후에도 실패 -> `"status": "error"`, `"error_message": "구체적 에러 내용"`
   - 사용자 개입 필요 -> `"status": "blocked"`, `"blocked_reason": "구체적 사유"` 후 즉시 중단

## 금지사항

- 실패한 QA를 기록 없이 통과 처리하지 마라. 이유: final docs-check는 기록 형식만 강제하므로 실제 확인 책임은 Step 5 실행자에게 있다.
- 브라우저 자동화 없이 추정으로 QA `PASS`를 기록하지 마라. 이유: 모바일/데스크톱 레이아웃은 실제 확인이 필요하다.
- `scripts/checks.py`에 MVP10 전용 규칙을 추가하지 마라. 이유: MVP별 규칙은 phase-local `docs-checks.json`에 둔다.
- AI/GPT 옷차림 추천을 후속 polish로 끼워 넣지 마라. 이유: MVP10 범위 밖이다.

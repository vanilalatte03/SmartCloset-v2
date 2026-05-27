# 단계 7: compose-docs-qa

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/MVP_CHANGE_CHECKLIST.md`
- `README.md`
- `docs/PRD.md`
- `docs/API.md`
- `docs/ARCHITECTURE.md`
- `docs/ERD.md`
- `docs/FRONTEND.md`
- `docs/DEMO_SCENARIO.md`
- `docs/SHARING_GUIDE.md`
- `docs/COMMANDS.md`
- `phases/8-smartcloset-account-stability/docs-checks.json`

## 작업

- MVP8 구현 결과와 SSOT 문서가 일치하는지 최종 동기화한다.
- `docs/qa/mvp8-account-stability-qa.md`를 작성한다.
- Docker Compose smoke를 수행한다.
- 브라우저 수동 QA 또는 가능한 범위의 API smoke로 회원가입, 이메일 인증, 로그인, refresh, reset, provider status, 계정 삭제, 기존 추천/위치/이미지 흐름을 확인한다.
- 최종 docs-check를 실행한다.
- 오래된 MVP7 current baseline 표현이 현재 baseline 문서에 남아 있으면 정리한다. 완료된 archive, 완료된 phase, 과거 ADR, 과거 QA 기록의 과거 표현은 유지할 수 있다.

## 인수 기준

```bash
git diff --check
./gradlew test
./gradlew build
(cd frontend && npm run build)
docker compose config --quiet
python3 scripts/checks.py --docs-check-config phases/8-smartcloset-account-stability/docs-checks.json --docs-check
```

Docker Compose smoke:

```bash
docker compose down -v
test -f .env || cp .env.example .env
docker compose up --build -d
curl -fsS http://localhost:8080/v3/api-docs >/dev/null
curl -fsS http://localhost:5173 >/dev/null
docker compose down
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. 아키텍처 체크리스트를 확인한다:
   - MVP8 포함/제외 범위가 문서와 구현에 일치하는가?
   - MVP5/MVP6/MVP7 기존 기능이 유지되는가?
   - refresh token, account token 원문이 저장/노출되지 않는가?
   - AWS 구현이 MVP8에 들어오지 않았는가?
   - QA 기록에 확인하지 못한 항목이 있으면 명시했는가?
3. 결과에 따라 `phases/8-smartcloset-account-stability/index.json`의 해당 단계를 업데이트한다:
   - 성공 -> `"status": "completed"`, `"summary": "MVP8 최종 docs-check, Docker Compose smoke, QA 기록을 정리했다."`
   - 수정 3회 시도 후에도 실패 -> `"status": "error"`, `"error_message": "구체적 에러 내용"`
   - 사용자 개입 필요 -> `"status": "blocked"`, `"blocked_reason": "구체적 사유"` 후 즉시 중단

검증 또는 리뷰가 통과하지 못하면 `issues/8-smartcloset-account-stability/issue-N.md`에 재현 명령, 핵심 에러, 수정 방향을 기록하고 fix step을 추가한다.

## 금지사항

- MVP8 범위를 넘는 polish를 구현하지 마라. 이유: Step 7은 최종 동기화와 검증 단계다.
- AWS 배포를 구현하지 마라. 이유: MVP9 범위다.
- docs-check 규칙을 통과시키기 위해 SSOT 문서의 실제 계약을 숨기지 마라. 이유: docs-check는 회귀 신호이며 SSOT를 대체하지 않는다.
- 기존 테스트를 깨뜨리지 마라.

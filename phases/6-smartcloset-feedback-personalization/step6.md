# 단계 6: compose-docs-qa

## 읽어야 할 파일

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/MVP_CHANGE_CHECKLIST.md`
- `docs/PRD.md`
- `docs/API.md`
- `docs/RECOMMENDATION_RULES.md`
- `docs/ERD.md`
- `docs/ARCHITECTURE.md`
- `docs/FRONTEND.md`
- `docs/DEMO_SCENARIO.md`
- `docs/SHARING_GUIDE.md`
- `docs/COMMANDS.md`
- `phases/6-smartcloset-feedback-personalization/docs-checks.json`

## 작업

MVP6 최종 공유 기준을 맞추고 검증한다.

- README와 `docs/`가 실제 구현과 일치하는지 확인한다.
- `docs/DEMO_SCENARIO.md`와 `docs/SHARING_GUIDE.md`의 MVP6 수동 QA 흐름을 확인한다.
- `docs/COMMANDS.md`의 명령이 현재 phase와 맞는지 확인한다.
- `phases/6-smartcloset-feedback-personalization/docs-checks.json`을 final docs-check 기준으로 정리한다.
- 필요하면 `docs/qa/` 아래 MVP6 QA 기록을 추가한다.
- Docker Compose smoke를 실행한다.

## 인수 기준

```bash
git diff --check
./gradlew test
./gradlew build
(cd frontend && npm run build)
docker compose config --quiet
python3 scripts/checks.py --docs-check-config phases/6-smartcloset-feedback-personalization/docs-checks.json --docs-check
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
2. 브라우저에서 회원가입, 옷 styleTags 저장, 상황 선택 추천, 착용 완료, 피드백 저장/clear, History 상태 표시를 확인한다.
3. 기존 이미지 업로드와 썸네일 표시가 유지되는지 확인한다.
4. phase index의 최종 step 상태를 업데이트한다.

## 금지사항

- AWS/S3/CDN 배포를 추가하지 마라. 이유: MVP6 공유 방식은 Docker Compose 기준이다.
- 자동 merge나 PR 운영 스크립트를 임의 변경하지 마라. 이유: MVP6 기능 범위가 아니다.
- 확인하지 않은 QA 결과를 PASS로 기록하지 마라. 이유: 수동 QA 문서는 실제 확인만 적는다.

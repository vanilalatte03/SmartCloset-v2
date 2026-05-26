# 단계 6: compose-docs-qa

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/MVP_CHANGE_CHECKLIST.md`
- `docs/PRD.md`
- `docs/API.md`
- `docs/RECOMMENDATION_RULES.md`
- `docs/ARCHITECTURE.md`
- `docs/ERD.md`
- `docs/FRONTEND.md`
- `docs/DEMO_SCENARIO.md`
- `docs/SHARING_GUIDE.md`
- `docs/COMMANDS.md`
- `phases/7-smartcloset-location-weather-trust/docs-checks.json`

## 작업

- MVP7 구현 결과와 SSOT 문서가 일치하는지 최종 동기화한다.
- `docs/qa/mvp7-location-weather-trust-qa.md`를 작성한다.
- Docker Compose smoke를 실행하고 결과를 QA 문서에 기록한다.
- 브라우저에서 동네 검색, 현재 위치 후보, 예보 시간대 선택, KMA/fallback/base/forecast 표시, History snapshot을 확인한다.
- `phases/7-smartcloset-location-weather-trust/docs-checks.json`이 최종 구현과 맞는지 필요하면 조정한다.

## 인수 기준

```bash
git diff --check
./gradlew test
./gradlew build
(cd frontend && npm run build)
docker compose config --quiet
python3 scripts/checks.py --docs-check-config phases/7-smartcloset-location-weather-trust/docs-checks.json --docs-check
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

1. 위 AC 커맨드와 Docker Compose smoke를 실행한다.
2. QA 문서에 실제 실행 결과만 기록한다.
3. 결과에 따라 `phases/7-smartcloset-location-weather-trust/index.json`의 해당 단계를 업데이트한다.
4. 모든 step 완료 후 phase final docs-check를 실행한다.

## 금지사항

- 확인하지 않은 QA 항목을 PASS로 기록하지 마라. 이유: QA 문서는 실제 검증 기록이다.
- 실제 API key, JWT, 비밀번호, private key를 문서에 기록하지 마라. 이유: 민감정보 커밋 금지다.
- MVP7 범위를 넘는 polish를 구현하지 마라. 이유: Step 6은 최종 동기화와 검증 단계다.

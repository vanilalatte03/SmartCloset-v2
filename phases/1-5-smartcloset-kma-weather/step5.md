# 단계 5: sharing-verification-and-doc-sync

범위: Must-have / 1.5 P0

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `README.md`
- `docs/PRD.md`
- `docs/ARCHITECTURE.md`
- `docs/API.md`
- `docs/RECOMMENDATION_RULES.md`
- `docs/ERD.md`
- `docs/DEMO_SCENARIO.md`
- `docs/SHARING_GUIDE.md`
- `docs/COMMANDS.md`
- `docs/ADR.md`
- `docs/adr/006-kma-vilage-forecast-weather-provider.md`
- `phases/1-5-smartcloset-kma-weather/step0.md`
- `phases/1-5-smartcloset-kma-weather/step1.md`
- `phases/1-5-smartcloset-kma-weather/step2.md`
- `phases/1-5-smartcloset-kma-weather/step3.md`
- `phases/1-5-smartcloset-kma-weather/step4.md`
- `.env.example`
- `docker-compose.yml`
- `src/main/resources/static/demo/**`

이전 단계에서 만들어진 구현과 테스트를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업
1.5차 KMA weather 구현이 공유 문서, Docker Compose, Demo UI 흐름과 맞는지 검증하고 필요한 최소 문서 동기화를 수행한다.

## 변경 예상 파일
- `README.md`
- `docs/DEMO_SCENARIO.md`
- `docs/SHARING_GUIDE.md`
- `docs/API.md`
- `docs/ARCHITECTURE.md`
- `docs/COMMANDS.md`
- `.env.example`
- `docker-compose.yml`
- 필요 시 `src/main/resources/static/demo/**`

## 구현 메모
- root `README.md`와 `docs/`가 구현 source of truth다.
- `archive/`는 참고용이며 구현 기준으로 삼지 않는다.
- `.env.example`에는 실제 서비스키를 넣지 않는다.
- Docker Compose는 `.env`의 KMA 환경변수를 app service로 전달해야 한다.
- 서비스키 없이 `WEATHER_FALLBACK_ENABLED=true` 기본값으로 앱과 추천 생성이 성공해야 한다.
- 서비스키가 있는 로컬 환경에서는 Swagger 또는 Demo UI에서 KMA 기반 weather snapshot을 수동 확인할 수 있어야 한다.
- Demo UI는 기존 추천 API를 호출해야 한다. 외부 KMA API를 브라우저에서 직접 호출하지 않는다.
- 필요하면 `docs/COMMANDS.md`의 autopilot 예시를 `1-5-smartcloset-kma-weather` 기준으로 보강하되, 실행 명령의 단일 출처 역할을 유지한다.

## 검증 절차
```bash
git diff --check
! rg -n 'GET /api/recommendations/(today)' . --glob '!archive/**'
rg -n "POST /api/recommendations\\?userId" README.md docs AGENTS.md .agents/skills/smartcloset-backend/SKILL.md
python3 -m compileall scripts
python3 -m pytest scripts/test_autopilot.py
./gradlew test
./gradlew build
```

선택 수동 검증:

```bash
test -f .env || cp .env.example .env
docker compose up --build
curl -s http://localhost:8080/v3/api-docs
```

## 인수 기준
- README와 공유 문서가 KMA 서비스키 설정 위치, 기본 격자, fallback 데모 흐름, 실제 API 연동 확인 흐름을 설명한다.
- `.env.example`과 `docker-compose.yml`의 KMA 환경변수가 문서와 일치한다.
- Demo UI 또는 Swagger 기준 추천 생성 흐름이 `POST /api/recommendations?userId={userId}`만 사용한다.
- 문서 검증에서 old today GET route exact 계약이 잡히지 않는다.
- `./gradlew test`와 `./gradlew build`가 통과한다.

## 금지사항
- archive 문서를 source of truth처럼 갱신하지 마라. 이유: archive는 과거 MVP 참고 요약만 담는다.
- 실제 API key를 문서, `.env.example`, 테스트 fixture에 넣지 마라. 이유: 서비스키는 민감정보다.
- Docker Compose 공유 방식을 AWS나 외부 배포 기준으로 바꾸지 마라. 이유: 1.5차 공유 방식은 Docker Compose다.
- Demo UI에서 KMA API를 직접 호출하지 마라. 이유: KMA 연동은 backend `WeatherProvider` 내부 책임이다.

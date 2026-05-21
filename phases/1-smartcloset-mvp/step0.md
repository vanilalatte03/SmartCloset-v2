# 단계 0: documentation-contract-sync

범위: Must-have / P0

## 읽어야 할 파일
- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/ARCHITECTURE.md`
- `docs/RECOMMENDATION_RULES.md`
- `docs/API.md`
- `docs/ERD.md`
- `docs/DEMO_SCENARIO.md`
- `docs/SHARING_GUIDE.md`
- `docs/adr/`
- `docs/COMMANDS.md`

## 작업
구현 전에 API 계약, MVP 범위, 실행 명령 문서가 서로 충돌하지 않는지 확인하고, 충돌이 있으면 구현과 분리된 문서 동기화 변경으로만 정리한다.

## 변경 예상 파일
- `docs/*.md`
- `README.md`
- `docs/COMMANDS.md`

## 구현 메모
- `POST /api/recommendations?userId={userId}`가 추천 생성 API의 유일한 기준인지 확인한다.
- `GET /api/recommendations/today`가 금지 규칙/검색 명령이 아닌 API 계약으로 남아 있으면 제거한다.
- StaticWeatherProvider 기본값은 `temperature=12`, `weatherType=CLOUDY`, `rainy=false`, `windy=false`로 맞춘다.
- Docker Compose가 유일한 필수 공유 방식인지 확인한다.
- Demo UI는 P1 단일 페이지 데모로만 표현한다.
- 코드 구현, Dockerfile, docker-compose.yml, `.env.example`, seed data 파일 생성은 이 step에서 하지 않는다.

## 검증 절차
```bash
git diff --check -- README.md docs AGENTS.md docs/COMMANDS.md .agents/skills/smartcloset-backend/SKILL.md
rg -n "GET /api/recommendations/today|recommendations/today|오늘의 추천 조회|오늘의 추천 결과 조회|추천 결과 조회 API" README.md docs
rg -n "POST /api/recommendations\\?userId" README.md docs/PRD.md docs/ARCHITECTURE.md docs/API.md docs/DEMO_SCENARIO.md docs/SHARING_GUIDE.md
```

## 인수 기준
- 구현 전에 정리해야 할 문서 충돌이 없거나, 별도 문서 변경으로 정리되어 있다.
- API 계약은 POST 추천 생성 기준으로 통일되어 있다.
- P0/P1 범위가 AGENTS와 SmartCloset backend skill 기준과 충돌하지 않는다.

## 금지사항
- Java 코드를 작성하지 마라. 이유: 이 단계는 문서 계약 동기화 전용이다.
- Docker 실행 파일을 만들지 마라. 이유: 실행 환경 구성은 이후 P0 step에서 다룬다.

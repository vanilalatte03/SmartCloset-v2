# 단계 5: sharing-verification-and-doc-sync

범위: Must-have / 2차 P0

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `README.md`
- `docs/PRD.md`
- `docs/API.md`
- `docs/ARCHITECTURE.md`
- `docs/ERD.md`
- `docs/FRONTEND.md`
- `docs/COMMANDS.md`
- `docs/DEMO_SCENARIO.md`
- `docs/SHARING_GUIDE.md`
- `docs/ADR.md`
- `docs/adr/007-mvp2-user-location-and-react-frontend.md`
- `phases/2-smartcloset-location-frontend/index.json`
- `docker-compose.yml`
- `.env.example`
- `frontend/package.json`

이전 단계에서 만들어진 백엔드와 프론트 구현을 기준으로 문서와 검증 흐름을 맞춘다.

## 작업
2차 MVP 구현 결과를 README와 docs에 동기화하고, 백엔드/프론트/Compose 검증을 끝낸다. 문서가 실제 실행 상태와 어긋나는 표현을 제거하고, 공유 대상자가 README만 보고 핵심 흐름을 확인할 수 있게 만든다.

## 변경 예상 파일
- `README.md`
- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/API.md`
- `docs/ARCHITECTURE.md`
- `docs/ERD.md`
- `docs/FRONTEND.md`
- `docs/COMMANDS.md`
- `docs/DEMO_SCENARIO.md`
- `docs/SHARING_GUIDE.md`
- `docs/ADR.md`
- 필요 시 `phases/2-smartcloset-location-frontend/README.md`

## 구현 메모
- `frontend/`와 Compose `frontend` 서비스가 실제로 존재하면 문서의 "구현 후" 표현을 현재 실행 가능한 흐름으로 갱신한다.
- README에는 Docker Compose로 백엔드, MySQL, 프론트엔드를 함께 실행하는 방법을 명확히 적는다.
- `docs/COMMANDS.md`에서 `frontend-build`가 현재 필수 검증인지 확인한다.
- API 문서와 프론트 타입 문서의 `ErrorResponse.details` shape가 같은지 확인한다.
- 위치 컬럼의 nullable/backfill 정책이 PRD, ERD, ARCHITECTURE, API에서 일치하는지 확인한다.
- KMA grid source of truth가 사용자 위치라는 표현이 PRD, ARCHITECTURE, skill 문서에서 일치하는지 확인한다.
- 1.5차 archive는 과거 참고용이며 source of truth가 아님을 유지한다.

## 검증 절차
```bash
git diff --check
! rg -n 'GET /api/recommendations/(today)' . --glob '!archive/**'
./gradlew test
./gradlew build
cd frontend && npm run build
docker compose config
test -f .env || cp .env.example .env
docker compose up --build
docker compose down
```

## 인수 기준
- README만 보고 Docker Compose로 `mysql`, `app`, `frontend`를 실행할 수 있다.
- `http://localhost:5173`에서 React 앱을 열 수 있다.
- `http://localhost:8080/swagger-ui/index.html`에서 API를 확인할 수 있다.
- demo scenario는 React 앱 중심 흐름으로 정리되어 있다.
- sharing guide는 실제 Compose 서비스 구성과 일치한다.
- active docs에서 1.5차가 현재 MVP처럼 남아 있지 않다.
- today 추천 GET 계약이 생기지 않았다.
- 백엔드 test/build와 프론트 build가 모두 통과한다.
- Compose 실행 후 위치 선택, 옷 등록, 추천 생성, 착용 완료를 수동으로 확인할 수 있다.

## 금지사항
- 문서에 실제로 실행되지 않는 URL이나 서비스를 현재 가능하다고 쓰지 마라. 이유: 공유자가 README만 보고 검증해야 한다.
- 1.5차 archive 문서를 active source of truth처럼 링크하지 마라. 이유: `archive/`는 과거 MVP 참고용이다.
- API 계약과 다른 프론트 DTO를 문서화하지 마라. 이유: 타입과 wire shape가 어긋나면 화면 오류가 난다.
- Docker Compose 검증 없이 공유 완료로 표시하지 마라. 이유: 2차 완료 기준에 프론트 포함 Compose 공유가 들어 있다.
- 실패한 검증을 숨기지 마라. 이유: Harness는 실패 시 `issues/2-smartcloset-location-frontend/issue-N.md`에 재현 명령과 수정 방향을 남겨야 한다.

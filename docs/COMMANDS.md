# Commands

이 파일은 프로젝트 실행 명령의 단일 출처입니다.

현재는 Java 21 Spring Boot 4.0.6 + Gradle + Docker Compose + React/Vite/TypeScript frontend 기준입니다. MVP4는 Spring Security + JWT Bearer 인증 사용자 API 위에서 반응형 웹 UX를 검증하므로, API 검증은 `userId` query parameter 없이 인증 사용자 기준으로 수행합니다.

MVP4 데모 전 로컬 Docker Compose DB는 기존 schema/seed data와 충돌할 수 있으므로 초기화를 권장합니다.

```bash
docker compose down -v
docker compose up --build
```

Gradle wrapper가 생성되어 `test`와 `build`는 Gradle 기준으로 실행합니다. Dockerfile과 Docker Compose 파일이 생성되어 P0 공유 검증은 Docker Compose로 실행합니다. 비어 있는 명령은 실행하지 않습니다.

## 개발 전 준비

```bash
python3 -m pip install -r requirements-dev.txt
git config core.hooksPath .githooks
```

## 활성 명령

| 이름 | 명령 | 필수 | 설명 |
| --- | --- | --- | --- |
| dev | `./gradlew bootRun` | no | 백엔드 로컬 개발 서버 실행 |
| frontend-dev | `cd frontend && npm run dev` | no | 프론트엔드 Vite 개발 서버 실행 |
| frontend-build | `cd frontend && npm run build` | yes | TypeScript type check 및 Vite build |
| lint | `python3 -m compileall scripts` | no | Harness 운영 스크립트 문법 검사 |
| test | `./gradlew test` | yes | Spring Boot/JUnit 테스트 실행 |
| build | `./gradlew build` | yes | Spring Boot 애플리케이션 빌드 |
| compose-config | `docker compose config` | no | Docker Compose 파일 구문과 서비스 구성 확인 |
| compose-up | `test -f .env || cp .env.example .env; docker compose up --build` | yes | Docker Compose로 MySQL, 백엔드, 프론트엔드 실행 |
| compose-down | `docker compose down` | yes | Docker Compose 중지 |
| compose-reset | `docker compose down -v` | yes | Docker Compose 중지 및 DB volume 초기화 |
| review | `python3 scripts/doctor.py` | no | 템플릿과 프로젝트 운영 상태 점검 |
| autopilot-test | `python3 -m pytest scripts/test_autopilot.py` | no | Harness autopilot 스크립트 테스트 |
| phase | `python3 scripts/execute.py <phase-name>` | no | Harness phase 실행 |
| autopilot | `python3 scripts/autopilot.py 3-smartcloset-auth-personalization --base main --max-review-fixes 2 --unsafe` | no | MVP-3 phase 기록용 step별 PR 생성, 자체 리뷰, 이슈 기록, 자동 병합 루프 |

## Harness 실행 로그

`phases/**/step*-output.json`과 `phases/**/phase*-output.json`은 Harness 실행 중 원인 분석을 위해 남기는 로그다. 출력이 크므로 `.rgignore`에서 기본 검색 대상에서 제외한다.

실패 원인을 확인할 때는 파일 전체를 열지 말고 필요한 앞부분만 잘라서 확인한다.

```bash
jq '{step, name, exitCode, stderr: (.stderr // "")[:1000], stdout: (.stdout // "")[:1000]}' phases/{phase-name}/step0-output.json
```

## P0 공유 검증 명령

```bash
# 로컬 테스트
./gradlew test

# 빌드
./gradlew build

# 프론트 빌드
(cd frontend && npm run build)

# MVP4 데모 전 DB 초기화
docker compose down -v

# Docker Compose 실행
test -f .env || cp .env.example .env
docker compose up --build

# Docker Compose 중지
docker compose down
```

MySQL 컨테이너 내부 포트는 `3306`이고, 호스트 공개 포트 기본값은 `.env.example` 기준 `3307`이다.

## URLs

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Frontend: `http://localhost:5173`
- 보조 Demo UI: `http://localhost:8080/demo/index.html`

## 문서 검증

```bash
git diff --check
! rg -n 'T[B]D|MVP4 기능 범위는 아직 확[정]|MVP4 작성 메[모]' README.md docs/PRD.md docs/API.md docs/ARCHITECTURE.md docs/FRONTEND.md docs/RECOMMENDATION_RULES.md docs/ERD.md docs/DEMO_SCENARIO.md docs/SHARING_GUIDE.md docs/COMMANDS.md
! rg -n 'GET /api/recommendations/(today)' README.md docs/PRD.md docs/API.md docs/ARCHITECTURE.md docs/FRONTEND.md docs/RECOMMENDATION_RULES.md docs/ERD.md docs/DEMO_SCENARIO.md docs/SHARING_GUIDE.md docs/COMMANDS.md AGENTS.md .agents/skills/smartcloset-backend/SKILL.md
! rg -n -F -e 'POST /api/recommendations''?userId' -e '/api/clothes''?userId' -e '/api/users/location''?userId' README.md docs/PRD.md docs/API.md docs/ARCHITECTURE.md docs/FRONTEND.md docs/RECOMMENDATION_RULES.md docs/ERD.md docs/DEMO_SCENARIO.md docs/SHARING_GUIDE.md docs/COMMANDS.md AGENTS.md .agents/skills/smartcloset-backend/SKILL.md
rg -n 'POST /api/recommendations' README.md docs/PRD.md docs/API.md docs/ARCHITECTURE.md docs/FRONTEND.md docs/RECOMMENDATION_RULES.md docs/ERD.md docs/DEMO_SCENARIO.md docs/SHARING_GUIDE.md docs/COMMANDS.md AGENTS.md .agents/skills/smartcloset-backend/SKILL.md
rg -n 'preferenceScore|preferred_colors_json|preferred_materials_json|style_tags_json' README.md docs/PRD.md docs/API.md docs/ARCHITECTURE.md docs/FRONTEND.md docs/RECOMMENDATION_RULES.md docs/ERD.md docs/DEMO_SCENARIO.md docs/SHARING_GUIDE.md docs/COMMANDS.md AGENTS.md .agents/skills/smartcloset-backend/SKILL.md
rg -n 'GET /api/locations' README.md docs/API.md docs/FRONTEND.md docs/DEMO_SCENARIO.md docs/SHARING_GUIDE.md
rg -n 'GET /api/weather/current' README.md docs/PRD.md docs/API.md docs/ARCHITECTURE.md docs/FRONTEND.md docs/DEMO_SCENARIO.md docs/SHARING_GUIDE.md docs/COMMANDS.md AGENTS.md .agents/skills/smartcloset-backend/SKILL.md
rg -n 'sessionStorage' README.md docs/PRD.md docs/API.md docs/ARCHITECTURE.md docs/FRONTEND.md docs/RECOMMENDATION_RULES.md docs/ERD.md docs/DEMO_SCENARIO.md docs/SHARING_GUIDE.md docs/COMMANDS.md AGENTS.md .agents/skills/smartcloset-backend/SKILL.md
rg -n '2분 안에 첫 추천 성공|오늘 입기 좋은 이유|하단 탭|색상 swatch|소재 chip' README.md docs/PRD.md docs/FRONTEND.md docs/DEMO_SCENARIO.md docs/SHARING_GUIDE.md
rg -n 'docker compose down -v' README.md docs/PRD.md docs/API.md docs/ARCHITECTURE.md docs/FRONTEND.md docs/RECOMMENDATION_RULES.md docs/ERD.md docs/DEMO_SCENARIO.md docs/SHARING_GUIDE.md docs/COMMANDS.md
```

프론트 앱과 Docker Compose `frontend` 서비스가 포함되어 있으므로 아래 명령도 필수로 통과해야 한다.

```bash
cd frontend && npm run build
docker compose down -v
docker compose up --build
```

## 자동 PR 루프

아래 명령은 clean worktree, 유효한 `gh auth status`, `origin` 원격, 최신 `main` 브랜치를 전제로 한다.

```bash
python3 scripts/autopilot.py 3-smartcloset-auth-personalization --base main --max-review-fixes 2 --unsafe
```

자동 루프는 다음 pending step만 `codex/{phase}-step{N}-{name}` 브랜치에서 실행하고 Draft PR을 생성한다. 로컬 검증과 자체 리뷰가 통과하면 PR을 ready로 전환한 뒤 squash merge하고 다음 step으로 진행한다. 실패하면 같은 PR에 자체 리뷰 코멘트, GitHub Issue, `issues/{phase}/issue-N.md`를 남긴 뒤 같은 브랜치에서 최대 2회 자동 수정과 재리뷰를 진행한다. 재시도 후에도 실패하면 PR과 Issue를 열어둔 채 중단한다.

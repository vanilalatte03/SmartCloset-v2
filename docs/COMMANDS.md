# Commands

이 파일은 프로젝트 실행 명령의 단일 출처입니다.

현재는 Java 21 Spring Boot 4.0.6 + Gradle + Docker Compose 기준입니다.
Gradle wrapper가 생성되어 `test`와 `build`는 Gradle 기준으로 실행합니다.
Dockerfile과 Docker Compose 파일이 생성되어 P0 공유 검증은 Docker Compose로
실행합니다. 비어 있는 명령은 실행하지 않습니다.

## 개발 전 준비

```bash
python3 -m pip install -r requirements-dev.txt
git config core.hooksPath .githooks
```

## 활성 명령

| 이름 | 명령 | 필수 | 설명 |
| --- | --- | --- | --- |
| dev |  | no | 개발 서버 또는 watch 실행 |
| lint | `python3 -m compileall scripts` | no | Harness 운영 스크립트 문법 검사 |
| test | `./gradlew test` | yes | Spring Boot/JUnit 테스트 실행 |
| build | `./gradlew build` | yes | Spring Boot 애플리케이션 빌드 |
| compose-up | `test -f .env || cp .env.example .env; docker compose up --build` | yes | Docker Compose로 앱과 MySQL 실행 |
| compose-down | `docker compose down -v` | yes | Docker Compose 중지 및 DB volume 초기화 |
| review | `python3 scripts/doctor.py` | no | 템플릿과 프로젝트 운영 상태 점검 |
| phase | `python3 scripts/execute.py <phase-name>` | no | Harness phase 실행 |
| autopilot | `python3 scripts/autopilot.py 1-smartcloset-mvp --base main` | no | step별 PR 생성, 자체 리뷰, 이슈 기록, 자동 병합 루프 |

## P0 공유 검증 명령

```bash
# 로컬 테스트
./gradlew test

# 빌드
./gradlew build

# Docker Compose 실행
test -f .env || cp .env.example .env
docker compose up --build

# Docker Compose 중지
docker compose down

# DB volume까지 초기화
docker compose down -v
```

MySQL 컨테이너 내부 포트는 `3306`이고, 호스트 공개 포트 기본값은 `.env.example` 기준 `3307`이다.

## URLs

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Demo UI(P1 구현 후): `http://localhost:8080/demo/index.html`

## 문서 검증

```bash
git diff --check
rg -n "GET /api/recommendations/today" .
rg -n "POST /api/recommendations" .
```

## 자동 PR 루프

아래 명령은 clean worktree, 유효한 `gh auth status`, `origin` 원격, 최신 `main` 브랜치를 전제로 한다.

```bash
python3 scripts/autopilot.py 1-smartcloset-mvp --base main
```

자동 루프는 다음 pending step만 `codex/{phase}-step{N}-{name}` 브랜치에서 실행하고 Draft PR을 생성한다. 로컬 검증과 자체 리뷰가 통과하면 PR을 ready로 전환한 뒤 squash merge하고 다음 step으로 진행한다. 실패하면 같은 PR에 자체 리뷰 코멘트, GitHub Issue, `issues/{phase}/issue-N.md`를 남긴 뒤 같은 브랜치에서 최대 2회 자동 수정과 재리뷰를 진행한다. 재시도 후에도 실패하면 PR과 Issue를 열어둔 채 중단한다.

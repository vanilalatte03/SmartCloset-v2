# Commands

이 파일은 프로젝트 실행 명령의 단일 출처입니다.

현재는 Java 21 Spring Boot 4.0.6 + Gradle + Docker Compose 기준으로 구현할 계획이
확정되어 있습니다. 다만 Gradle wrapper, Dockerfile, Docker Compose 파일은
아직 생성 전입니다. Step 1 전에는 Harness 운영 스크립트 검증을 활성 명령으로
사용하고, Step 1에서 Gradle wrapper가 생성된 뒤 `test`와 `build`를 Gradle
기준으로 갱신합니다. 비어 있는 명령은 실행하지 않습니다.

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
| test | `python3 -m pytest scripts/test_checks.py scripts/test_guard.py scripts/test_execute.py scripts/test_autopilot.py` | yes | Step 1 전 Harness 운영 테스트, Step 1 이후 `./gradlew test`로 갱신 |
| build |  | yes | Step 1 이후 `./gradlew build`로 갱신 |
| review | `python3 scripts/doctor.py` | no | 템플릿과 프로젝트 운영 상태 점검 |
| phase | `python3 scripts/execute.py <phase-name>` | no | Harness phase 실행 |
| autopilot | `python3 scripts/autopilot.py 1-smartcloset-mvp --base main` | no | phase 실행, PR 생성, 자체 리뷰, 이슈 기록, 자동 병합 루프 |

## 구현 후 사용할 명령

아래 명령은 관련 파일이 생성된 뒤 실행한다.

```bash
# 로컬 테스트
./gradlew test

# 빌드
./gradlew build

# Docker Compose 실행
docker compose up --build

# Docker Compose 중지
docker compose down

# DB volume까지 초기화
docker compose down -v
```

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
python3 scripts/autopilot.py 1-smartcloset-mvp --base main --max-review-fixes 2
```

자동 루프는 `codex/{phase}` 브랜치에서 phase를 실행하고 draft PR을 생성한다. 로컬 검증과 자체 리뷰가 통과하면 PR을 ready로 전환한 뒤 squash merge한다. 실패하면 GitHub Issue와 `issues/{phase}/issue-N.md`를 만들고 최대 2회 fix PR을 재시도한다.

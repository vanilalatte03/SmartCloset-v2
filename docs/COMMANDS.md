# Commands

이 파일은 프로젝트 실행 명령의 단일 출처입니다.

현재는 Java 21 Spring Boot 4.0.6 + Gradle + Docker Compose + React/Vite/TypeScript frontend 기준입니다. MVP9는 MVP8 계정 안정성 완료 baseline 위에서 프론트 UI/UX 리디자인을 진행합니다.

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
| harness-test | `python3 -m pytest scripts/test_checks.py scripts/test_execute.py scripts/test_autopilot.py scripts/test_guard.py` | yes | Harness 운영 스크립트 회귀 테스트 |
| docs-check | `python3 scripts/checks.py --docs-check --include-final-docs` | yes | phase 최종 문서 계약과 MVP 제외 범위 검증 |
| compose-config | `docker compose config --quiet` | no | Docker Compose 파일 구문과 서비스 구성 확인 |
| compose-up | `test -f .env || cp .env.example .env; docker compose up --build` | yes | Docker Compose로 MySQL, 백엔드, 프론트엔드 실행 |
| compose-down | `docker compose down` | yes | Docker Compose 중지 |
| compose-reset | `docker compose down -v` | yes | Docker Compose 중지 및 DB/image volume 초기화 |
| review | `python3 scripts/doctor.py` | no | 템플릿과 프로젝트 운영 상태 점검 |
| autopilot-test | `python3 -m pytest scripts/test_autopilot.py` | no | Harness autopilot 스크립트 테스트 |
| phase | `python3 scripts/execute.py <phase-name>` | no | Harness phase 실행 |
| autopilot | `python3 scripts/autopilot.py 9-smartcloset-ui-ux-redesign --base main --max-review-fixes 2 --unsafe` | no | MVP9 step별 PR 생성, 자체 리뷰, 이슈 기록, 자동 병합 루프 |

Harness Codex 호출은 전역 Codex 설정을 그대로 상속하지 않고 reasoning effort를 명시한다. `execute.py`의 step 구현 기본값은 `medium`이고, `autopilot.py`의 기본값은 step 구현 `medium`, PR self-review `high`, 자동 fix `medium`이다. `xhigh`는 `--allow-xhigh`와 함께 명시한 경우에만 허용한다.

## 문서 전환 검증 명령

```bash
git diff --check
python3 scripts/checks.py --docs-check-config phases/9-smartcloset-ui-ux-redesign/docs-checks.json --docs-check
```

## P0 공유 검증 명령

```bash
./gradlew test
./gradlew build
(cd frontend && npm run build)
docker compose config --quiet
```

MVP9 프론트 구현 step의 최소 검증:

```bash
(cd frontend && npm run build)
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

Docker Compose 기본 profile은 `.env.example`의 `SPRING_PROFILES_ACTIVE=local`이다. 후속 MVP에서 `prod` profile을 추가하더라도 local Compose 명령과 local env 기본값은 유지한다.

MVP9 최종 QA에서는 Codex Browser를 우선 사용하고, 필요하면 Chrome 또는 Computer Use로 대체해 Auth, 추천, 옷장, 내 취향, 위치, 기록, 계정 설정 화면을 데스크톱 1440px과 모바일 390px 기준으로 확인한다. 결과는 `docs/qa/mvp9-ui-ux-redesign-qa.md`에 viewport, 화면명, 결과 `PASS`, 확인 도구, 확인 메모가 있는 14개 행으로 기록한다. Final docs-check는 이 기록이 없으면 실패한다. MVP8 세션 복구, 이메일 인증, 비밀번호 재설정, Google provider 상태, 계정 삭제, 기존 위치/날씨 추천과 이미지/피드백 흐름 유지 여부도 함께 확인한다.

## URLs

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Frontend: `http://localhost:5173`
- 보조 Demo UI: `http://localhost:8080/demo/index.html`

## 문서 검증

MVP 범위나 baseline을 바꿀 때 전체 확인 대상은 `docs/MVP_CHANGE_CHECKLIST.md`를 먼저 본다.
검증 규칙은 `phases/{phase}/docs-checks.json`에서 관리한다. MVP 범위가 바뀌면 `scripts/checks.py`가 아니라 해당 phase의 rule 파일을 갱신한다.
`harness-test`와 `docs-check`는 step-local `manual` 검증에서는 제외되고, 마지막 step 이후 `final` stage 또는 직접 실행에서만 수행한다.
프로젝트 skill의 Documentation Sync Rules는 에이전트가 따라야 하는 정성 규칙이고, `docs-checks.json`은 final stage에서 자동으로 잡을 수 있는 핵심 회귀 신호만 담는다.

```bash
python3 scripts/checks.py --stage final
python3 -m pytest scripts/test_checks.py scripts/test_execute.py scripts/test_autopilot.py scripts/test_guard.py
python3 scripts/checks.py --docs-check-config phases/9-smartcloset-ui-ux-redesign/docs-checks.json --docs-check
```

## 자동 PR 루프

아래 명령은 clean worktree, 유효한 `gh auth status`, `origin` 원격, 최신 `main` 브랜치를 전제로 한다.

```bash
python3 scripts/autopilot.py 9-smartcloset-ui-ux-redesign --base main --max-review-fixes 2 --unsafe
```

자동 루프는 다음 pending step만 `codex/{phase}-step{N}-{name}` 브랜치에서 실행하고 Draft PR을 생성한다. 자체 리뷰 gate는 해당 step 문서의 `## 인수 기준` fenced command와 `git diff --check`를 실행한다. `execute.py --step` 또는 `--next-step-only`가 마지막 pending step을 완료하면 phase 완료 metadata를 기록하기 전에 `python3 scripts/checks.py --stage final`을 실행하므로 마지막 step PR은 merge 전에 final gate를 통과해야 한다. 로컬 검증과 자체 리뷰가 통과하면 PR을 ready로 전환한 뒤 squash merge하고 다음 step으로 진행한다. 실패하면 같은 PR에 자체 리뷰 코멘트, GitHub Issue, `issues/{phase}/issue-N.md`를 남긴 뒤 같은 브랜치에서 최대 2회 자동 수정과 재리뷰를 진행한다.

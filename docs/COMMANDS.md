# Commands

이 파일은 프로젝트 실행 명령의 단일 출처입니다.

현재는 Java 21 Spring Boot 4.0.6 + Gradle + Docker Compose + React/Vite/TypeScript frontend 기준입니다. MVP10은 MVP9 UI/UX 리디자인 완료 baseline 위에서 Spring AI 기반 사진 업로드 옷 등록 보조를 추가합니다.

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
| migration-smoke | `./gradlew test --tests com.smartcloset.persistence.SchemaMigrationSmokeTest` | yes | Flyway baseline migration 후 Hibernate validate 검증 |
| build | `./gradlew build` | yes | Spring Boot 애플리케이션 빌드 |
| harness-test | `python3 -m pytest scripts/tests` | yes | Harness 운영 스크립트 회귀 테스트 |
| docs-check | `python3 scripts/checks.py --docs-check --include-final-docs` | yes | phase 최종 문서 계약과 MVP 제외 범위 검증 |
| compose-config | `docker compose config --quiet` | no | Docker Compose 파일 구문과 서비스 구성 확인 |
| compose-up | `test -f .env || cp .env.example .env; docker compose up --build` | yes | Docker Compose로 MySQL, 백엔드, 프론트엔드 실행 |
| compose-down | `docker compose down` | yes | Docker Compose 중지 |
| compose-reset | `docker compose down -v` | yes | Docker Compose 중지 및 DB/image volume 초기화 |
| review | `python3 scripts/doctor.py --instance` | no | 템플릿과 프로젝트 운영 상태 점검 |
| autopilot-test | `python3 -m pytest scripts/tests/test_autopilot.py` | no | Harness autopilot 스크립트 테스트 |
| phase | `python3 scripts/execute.py <phase-name>` | no | Harness phase 실행 |
| autopilot | `python3 scripts/autopilot.py 10-smartcloset-ai-clothing-assist --base main --max-review-fixes 2 --unsafe` | no | MVP10 step별 PR 생성, 자체 리뷰, 이슈 기록, 자동 병합 루프 |

Harness Codex 호출은 전역 Codex 설정을 그대로 상속하지 않고 reasoning effort를 명시한다. `execute.py`의 step 구현 기본값은 `medium`이고, `autopilot.py`의 기본값은 step 구현 `medium`, PR self-review `high`, 자동 fix `medium`이다. `xhigh`는 `--allow-xhigh`와 함께 명시한 경우에만 허용한다.

autopilot 운영 옵션과 안전장치:

- `--base`를 생략하면 origin HEAD 브랜치를 자동 감지한다(실패 시 `main`).
- `--dry-run`은 실행 없이 pending step과 브랜치 계획만 출력한다.
- `--max-steps N`은 한 번의 실행에서 최대 N개의 step PR만 병합하고 멈춘다.
- 동시 실행은 `.codex/autopilot.lock`으로 차단한다(stale lock은 자동 회수).
- step 문서의 인수 기준 명령은 실행 전에 `guard.py` 위험 명령 정책을 통과해야 하고, `execute.py`도 codex의 completed 보고 후 인수 기준을 직접 재실행해 검증한다.
- PR은 `gh pr checks --watch`로 원격 체크 통과를 확인한 뒤에만 squash merge한다. "no checks reported"는 ready 직후 체크 런 생성 전 레이스일 수 있어 60초 grace 동안 재확인하고, 그래도 없으면 체크 없는 저장소로 판단해 진행한다. CI가 없는 저장소는 `--allow-no-checks`로 grace 대기를 생략한다.
- 금지 범위 규칙은 전부 데이터로 관리한다: 전역 규칙은 `.codex/scope-rules.json`(`forbidden`), phase별 확장/허용은 `phases/<phase>/scope-rules.json`(`extraForbidden`, `allowedScopeMessages` — `steps`/`stepNames`/`requiresAnyLowered`/`forbidsAnyLowered`). 스캐너 코드에는 키워드가 없으므로 autopilot.py 자체는 스캔 대상이고, scope-rules.json 파일만 이름 기준으로 제외된다.
- stop 훅의 기본 검사는 lint만 실행한다. test/build까지 돌리려면 `.codex/project-profile.json`의 `stageChecks.stop`으로 확장한다.
- `execute.py`는 phase README/step 문서가 참조하는 `docs/*.md`만 step 프롬프트에 첨부한다(참조가 없으면 전체 첨부, `guardrailDocs` 프로필 키가 있으면 그 목록이 우선).

## 문서 전환 검증 명령

```bash
git diff --check
python3 scripts/checks.py --docs-check-config phases/10-smartcloset-ai-clothing-assist/docs-checks.json --docs-check
```

## P0 공유 검증 명령

```bash
./gradlew test
./gradlew test --tests com.smartcloset.persistence.SchemaMigrationSmokeTest
./gradlew build
(cd frontend && npm run build)
docker compose config --quiet
```

MVP10 구현 step의 최소 검증:

```bash
./gradlew test
(cd frontend && npm run build)
python3 scripts/checks.py --docs-check-config phases/10-smartcloset-ai-clothing-assist/docs-checks.json --docs-check
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

Docker Compose 기본 profile은 `.env.example`의 `SPRING_PROFILES_ACTIVE=local`이다. demo user와 최소 옷장 seed는 `local`/`demo` profile에서 `SMARTCLOSET_SEED_ENABLED=true`일 때만 생성된다. 깨끗한 MySQL volume은 Flyway `V*.sql` migration으로 생성하고 Hibernate `ddl-auto=validate`로 검증한다. MVP10 AI 분석은 기본 비활성이며, `CLOTHING_ANALYSIS_ENABLED=false`, `SPRING_AI_MODEL_CHAT=none`, 빈 `OPENAI_API_KEY` 상태에서도 Compose 실행이 가능해야 한다.

`prod` profile은 local placeholder `JWT_SECRET`과 Hibernate `ddl-auto=update`를 fail-fast로 막고, Swagger UI/API docs를 기본 비활성화한다. 운영 DB schema 변경은 Flyway migration으로 추적하며, 기존 운영 DB 편입은 배포 절차에서 `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true`를 명시한 경우에만 허용한다. AWS adapter, RDS/Secrets Manager 구성은 후속 범위다.

MVP10 최종 QA에서는 Codex Browser를 우선 사용하고, 필요하면 Chrome 또는 Computer Use로 대체해 옷장 AI 후보 체크, Auth, 추천, 내 취향, 위치, 기록, 계정 설정 화면을 데스크톱 1440px과 모바일 390px 기준으로 확인한다. 결과는 `docs/qa/mvp10-ai-clothing-assist-qa.md`에 기록한다. Final docs-check는 아래 행이 없으면 실패한다.

- `desktop 1440px | 옷장 AI 후보 체크 | PASS`
- `mobile 390px | 옷장 AI 후보 체크 | PASS`
- `backend API | analysis cases | PASS`
- `recommendation | AI 분석 전후 추천 불변 | PASS`

MVP8 세션 복구, 이메일 인증, 비밀번호 재설정, Google provider 상태, 계정 삭제, 기존 위치/날씨 추천과 이미지/피드백 흐름 유지 여부도 함께 확인한다.

## URLs

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Frontend: `http://localhost:5173`

## 문서 검증

MVP 범위나 baseline을 바꿀 때 전체 확인 대상은 `docs/MVP_CHANGE_CHECKLIST.md`를 먼저 본다.
검증 규칙은 `phases/{phase}/docs-checks.json`에서 관리한다. MVP 범위가 바뀌면 `scripts/checks.py`가 아니라 해당 phase의 rule 파일을 갱신한다.
`harness-test`와 `docs-check`는 step-local `manual` 검증에서는 제외되고, 마지막 step 이후 `final` stage 또는 직접 실행에서만 수행한다.
프로젝트 skill의 Documentation Sync Rules는 에이전트가 따라야 하는 정성 규칙이고, `docs-checks.json`은 final stage에서 자동으로 잡을 수 있는 핵심 회귀 신호만 담는다.

```bash
python3 scripts/checks.py --stage final
python3 -m pytest scripts/tests
python3 scripts/checks.py --docs-check-config phases/10-smartcloset-ai-clothing-assist/docs-checks.json --docs-check
```

## 자동 PR 루프

아래 명령은 clean worktree, 유효한 `gh auth status`, `origin` 원격, 최신 `main` 브랜치를 전제로 한다.

```bash
python3 scripts/autopilot.py 10-smartcloset-ai-clothing-assist --base main --max-review-fixes 2 --unsafe
```

자동 루프는 다음 pending step만 `codex/{phase}-step{N}-{name}` 브랜치에서 실행하고 Draft PR을 생성한다. 자체 리뷰 gate는 해당 step 문서의 `## 인수 기준` fenced command와 `git diff --check`를 실행한다. `execute.py --step` 또는 `--next-step-only`가 마지막 pending step을 완료하면 phase 완료 metadata를 기록하기 전에 `python3 scripts/checks.py --stage final`을 실행하므로 마지막 step PR은 merge 전에 final gate를 통과해야 한다. 로컬 검증과 자체 리뷰가 통과하면 PR을 ready로 전환한 뒤 squash merge하고 다음 step으로 진행한다. 실패하면 같은 PR에 자체 리뷰 코멘트, GitHub Issue, `issues/{phase}/issue-N.md`를 남긴 뒤 같은 브랜치에서 최대 2회 자동 수정과 재리뷰를 진행한다.

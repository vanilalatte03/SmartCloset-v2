# Seed demo 데이터 profile gate 기록

## 문서 목적

이 문서는 local/demo 실행 편의를 위한 `SeedDataInitializer`가 default/prod runtime에서 실행되지 않도록 제한한 운영 안전 개선 기록이다.

이 문서는 ADR이 아니며 공개 API, DB schema, 회원가입/Google 로그인 후 기본 옷 preset seed 흐름을 변경하지 않는다. 관련 GitHub Issue는 `#176`이고, 구현은 PR `#184`에서 merge했다.

## 문제

`SeedDataInitializer`는 애플리케이션 시작 시 id `1` 사용자와 최소 옷장 데이터를 보정하는 bootstrap initializer다.

기존에는 `@Component`만 선언되어 active profile과 관계없이 `ApplicationRunner`로 등록됐다. 운영 profile 또는 운영 DB에 연결된 환경에서 애플리케이션이 기동되면 다음 위험이 있었다.

- id `1` 사용자가 없을 때 `demo-user` seed 계정이 자동 생성될 수 있다.
- seed 계정은 고정 bcrypt hash, `emailVerified=true`, `passwordLoginEnabled=true` 상태로 만들어진다.
- 최소 옷장 seed 데이터가 운영 DB에 섞여 데이터 오염을 만들 수 있다.
- local/demo 편의 코드와 실제 runtime bootstrap 경계가 흐려진다.

## 변경

`SeedDataInitializer` 등록 조건을 profile gate와 property gate로 나눴다.

1. `@Profile({"local", "demo"})`로 local/demo profile에서만 bean 후보가 된다.
2. `@ConditionalOnProperty(prefix = "smartcloset.seed", name = "enabled", havingValue = "true")`로 명시적 seed 활성 조건을 추가했다.
3. `application.yml`의 기본값은 `SMARTCLOSET_SEED_ENABLED=false`로 둔다.
4. `application-local.yml`과 `application-demo.yml`은 `SMARTCLOSET_SEED_ENABLED=true`를 기본값으로 둔다.
5. Docker Compose와 `.env.example`은 기존 공유 흐름을 유지하기 위해 `SPRING_PROFILES_ACTIVE=local`, `SMARTCLOSET_SEED_ENABLED=true`를 명시한다.

## 보존한 계약

- local Docker Compose 실행은 기존처럼 demo user와 최소 옷장 seed를 준비한다.
- demo profile도 명시적으로 허용된 seed 실행 경로로 유지한다.
- `SMARTCLOSET_SEED_ENABLED=false`이면 local/demo profile에서도 seed initializer를 비활성화할 수 있다.
- default/prod profile은 `SMARTCLOSET_SEED_ENABLED=true`가 있어도 local/demo profile이 아니면 seed initializer를 등록하지 않는다.
- 이미 생성된 seed/demo 데이터에 대한 자동 삭제나 destructive cleanup은 추가하지 않는다.
- 신규 회원가입, 로그인, refresh, Google OAuth session 생성에서 사용하는 `DefaultClothingPresetSeeder` 경로는 변경하지 않는다.
- 공개 API와 인증/인가 계약을 변경하지 않는다.

## 운영 영향

운영/default runtime은 기동만으로 demo 계정과 seed clothes를 자동 생성하지 않는다. 따라서 운영 DB가 비어 있어도 id `1` seed 계정이 만들어지지 않고, 고정 password-login 가능 계정이 운영 인증 경계에 노출되는 위험이 줄어든다.

local/demo runtime은 profile과 property가 모두 맞을 때만 seed를 생성한다. 이는 Docker Compose 공유 환경의 데모 가능성을 유지하면서도 profile이 없는 실행, prod profile 실행, seed disabled 실행을 명확하게 분리한다.

## 회귀 기준

seed demo gate는 다음 기준을 지킨다.

- `SeedDataInitializer`는 default/test/prod profile에서 bean으로 등록되면 안 된다.
- `SeedDataInitializer`는 local/demo profile과 `smartcloset.seed.enabled=true`가 함께 있을 때만 등록된다.
- local/demo profile에서 `smartcloset.seed.enabled=false`이면 등록되지 않는다.
- Docker Compose 기본 실행은 local profile과 seed enabled 상태를 유지한다.
- 운영 profile에 seed property가 실수로 주입되어도 local/demo profile이 아니면 seed initializer가 실행되지 않는다.
- seed 안전장치는 이미 저장된 운영 데이터를 자동 삭제하지 않는다.
- auth signup/OAuth 기본 옷 preset seeding과 혼동해 `DefaultClothingPresetSeeder`를 profile gate 뒤로 옮기면 안 된다.

## 검증

PR `#184`에서 다음 검증을 통과했다.

- `git diff --check`
- `git diff --check origin/main...HEAD`
- `docker compose config --quiet`
- `python3 scripts/checks.py --docs-check --include-final-docs`
- `./gradlew test --tests com.smartcloset.common.config.SeedDataInitializerProfileTest --tests com.smartcloset.persistence.SeedDataPersistenceTest --tests com.smartcloset.persistence.SeedDataLocalProfilePersistenceTest`
- `./gradlew test`
- 커밋 훅: `python3 -m compileall scripts`
- 커밋 훅: `./gradlew build`
- 커밋 훅: `cd frontend && npm run build`
- Codex CLI read-only review: `pass=true`, findings 없음

추가된 테스트는 local/demo/prod/default profile과 seed property 조합에서 initializer bean 등록 여부를 검증하고, local profile에서는 기존 demo user와 최소 옷장 seed가 유지되며 test/default 계열에서는 runtime initializer가 seed 데이터를 만들지 않는지 확인한다.

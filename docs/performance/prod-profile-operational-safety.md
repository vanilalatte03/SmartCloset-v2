# Prod profile 운영 안전장치 기록

## 문서 목적

이 문서는 local/Docker Compose 실행 편의를 위한 기본 설정이 `prod` profile로 이어지지 않도록 분리한 운영 안전 개선 기록이다.

이 문서는 ADR이 아니며 공개 API shape, DB schema, 인증/refresh token 계약, Docker Compose local 기본 실행 흐름을 변경하지 않는다. 관련 GitHub Issue는 `#181`이고, 구현은 PR `#189`에서 merge했다.

## 문제

기본 `application.yml`과 Docker Compose 설정은 MVP local 실행을 빠르게 검증하기 위해 Hibernate `ddl-auto=update`, local JWT placeholder, Swagger UI/API docs 노출을 허용한다.

이 기본값은 local 개발과 공유용 Docker Compose에는 맞지만, `prod` profile 또는 운영 DB 연결 환경에 그대로 적용되면 다음 위험이 생긴다.

- 운영 DB schema가 Hibernate `ddl-auto=update`로 예기치 않게 변경될 수 있다.
- `change-me-local-development-only` 같은 local placeholder로 JWT가 서명될 수 있다.
- Swagger UI/API docs가 운영 profile에서 의도치 않게 노출될 수 있다.
- local 실행 기준과 운영 실행 기준이 설정/문서상 분리되지 않아 운영자가 잘못된 기본값을 놓치기 쉽다.

## 변경

`application-prod.yml`을 추가해 prod profile의 기본값을 local 기본값과 분리했다.

- `spring.jpa.hibernate.ddl-auto` 기본값은 `validate`다.
- `smartcloset.security.jwt.secret`은 `JWT_SECRET`이 없으면 blank로 남아 fail-fast 대상이 된다.
- `springdoc.api-docs.enabled` 기본값은 `false`다.
- `springdoc.swagger-ui.enabled` 기본값은 `false`다.

`ProdProfileSafetyGuard`를 추가해 `prod` profile context 초기화 초기에 다음 조건을 검증한다.

- JWT secret이 비어 있으면 기동 실패
- JWT secret이 local placeholder와 같으면 기동 실패
- Hibernate `ddl-auto`가 `validate` 또는 `none`이 아니면 기동 실패

guard는 `BeanFactoryPostProcessor`로 동작하므로 JPA entity manager 초기화와 schema action 전에 unsafe DDL 설정을 차단한다.

## 보존한 계약

- `application.yml`의 local-friendly 기본값은 유지한다.
- Docker Compose 기본 profile은 `local`이다.
- Docker Compose 기본 실행은 demo user와 최소 옷장 seed를 기존처럼 준비한다.
- `SeedDataInitializer`는 계속 `local`/`demo` profile과 `smartcloset.seed.enabled=true`에서만 활성화된다.
- local Swagger UI와 OpenAPI JSON 경로는 기본적으로 계속 사용할 수 있다.
- migration tool 전환, AWS 배포, RDS/Secrets Manager, 운영 adapter 구현은 추가하지 않는다.
- 공개 `userId` query parameter 또는 현재 사용자 DTO `userId` 노출을 추가하지 않는다.
- refresh token 원문은 DB, 로그, JSON response에 저장하거나 노출하지 않는다.

## 운영 영향

운영 profile은 secret 누락과 local placeholder 사용을 시작 단계에서 명확히 실패시킨다. 따라서 잘못된 secret으로 access token을 발급하는 상황을 배포 직후 요청 처리 중이 아니라 기동 단계에서 발견할 수 있다.

운영 profile의 schema action은 기본적으로 `validate`이며, guard가 `update/create/create-drop` 계열 자동 schema 변경을 막는다. MVP10은 migration tool을 도입하지 않으므로 운영 DB 변경은 별도 후속 범위에서 명시적으로 다뤄야 한다.

Swagger UI/API docs는 prod profile에서 기본 비활성화된다. 필요하면 `SPRINGDOC_API_DOCS_ENABLED`와 `SPRINGDOC_SWAGGER_UI_ENABLED`를 명시적으로 설정할 수 있지만, local profile처럼 기본 노출되지는 않는다.

## 회귀 기준

prod profile 운영 안전장치는 다음 기준을 지킨다.

- local profile 또는 Docker Compose 기본 실행에서 `ddl-auto=update`, local JWT placeholder, Swagger docs 기본 노출이 깨지면 안 된다.
- `prod` profile에서 `JWT_SECRET`이 비어 있으면 기동하면 안 된다.
- `prod` profile에서 `JWT_SECRET=change-me-local-development-only`이면 기동하면 안 된다.
- `prod` profile에서 Hibernate `ddl-auto`가 `validate` 또는 `none` 외 값이면 기동하면 안 된다.
- `application-prod.yml`의 Springdoc docs/UI 기본값은 `false`여야 한다.
- prod guard는 JPA schema action 전에 실행되어야 한다.
- seed/demo profile gate를 완화하거나 `prod` profile에서 demo seed initializer를 활성화하면 안 된다.
- Flyway/Liquibase, AWS/RDS/Secrets Manager, Redis, 운영 adapter를 이 안전장치 범위에 끌어오면 안 된다.

## 검증

PR `#189`에서 다음 검증을 통과했다.

- `./gradlew test --tests com.smartcloset.common.config.ProdProfileSafetyGuardTest --tests com.smartcloset.common.config.SeedDataInitializerProfileTest`
- `git diff --check`
- `python3 scripts/checks.py --docs-check --include-final-docs`
- `./gradlew test`
- `./gradlew build`
- `cd frontend && npm run build`
- GitHub Actions: `test-build`
- 커밋 훅: `python3 -m compileall scripts`
- 커밋 훅: `./gradlew test`
- 커밋 훅: `./gradlew build`
- 커밋 훅: `cd frontend && npm run build`
- Codex CLI read-only review: `pass=true`, findings 없음

추가된 테스트는 local profile에서 guard가 생성되지 않는지, prod profile에서 정상 secret과 safe DDL 값은 허용되는지, JWT secret 누락/local placeholder와 unsafe DDL 값은 실패하는지, `application-prod.yml` 기본값이 Swagger docs/UI 비활성 및 `ddl-auto=validate`인지 확인한다.

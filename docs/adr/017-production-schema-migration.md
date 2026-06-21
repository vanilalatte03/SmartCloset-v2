# 운영 schema migration 재도입

## 상태

승인됨

## 맥락

MVP8-10에서는 local Docker Compose와 기능 검증 속도를 우선해 production DB migration 도구 전환을 후속 범위로 두었다. 대신 `prod` profile은 Hibernate `ddl-auto=validate`와 `ProdProfileSafetyGuard`로 위험한 자동 schema 변경을 막았다.

운영 준비 이슈 #203에서는 이 상태의 남은 위험을 P0로 분류했다. 운영 DB를 생성하거나 변경할 때 수동 SQL이나 Hibernate `update`에 의존하면 배포 재현성, 환경 간 schema drift 검출, rollback 판단이 약해진다.

## 결정

Flyway를 schema migration 도구로 재도입한다.

- Spring Boot 4의 `spring-boot-starter-flyway`와 Flyway MySQL support를 사용한다.
- 현재 JPA entity 기준 baseline schema는 `src/main/resources/db/migration/V1__baseline_schema.sql`에 둔다.
- 깨끗한 MySQL DB는 Flyway migration만으로 현재 schema를 생성해야 한다.
- Hibernate `ddl-auto` 기본값은 `validate`로 두고, schema 생성/변경 책임은 Flyway `V*.sql` migration에 둔다.
- `prod` profile은 기존처럼 `ddl-auto=update/create/create-drop` 계열을 허용하지 않는다.
- `prod` profile의 `SPRING_FLYWAY_BASELINE_ON_MIGRATE` 기본값은 `false`다. 기존 운영 DB를 baseline으로 편입할 때만 배포 절차에서 명시적으로 `true`를 설정한다.
- `local`/`demo` profile은 기존 non-empty Docker Compose volume 편입을 위해 `baseline-on-migrate=true`를 기본값으로 둘 수 있다.
- test profile의 일반 JPA 테스트는 Flyway를 비활성화하고 기존 H2 `create-drop` 경로를 유지한다. migration 자체는 전용 smoke test에서 Flyway 적용 후 Hibernate validate로 검증한다.

## 결과

- 신규 운영 또는 로컬 clean DB schema 생성 절차가 재현 가능한 migration 파일로 남는다.
- 운영 profile은 Hibernate 자동 변경 없이 entity/schema drift를 시작 단계에서 검출한다.
- 기존 local/demo volume은 baseline 편입으로 완만하게 전환할 수 있다.
- 다음 schema 변경은 JPA entity 변경과 함께 새 `V*.sql` migration을 추가해야 한다.
- AWS/RDS 배포 산출물, backup/restore 자동화, migration rollback runbook은 별도 운영 readiness 이슈에서 다룬다.

## 범위 제외

- AWS 배포 구현
- RDS 또는 Secrets Manager 구성
- Docker image hardening
- DB backup/restore job
- Flyway rollback 자동화
- MVP10 AI 분석 결과 저장 schema
- 추천 점수, 후보 필터, tie-break 변경

# 운영 runtime 산출물 정의

## Status

Accepted

## Context

운영 준비 이슈 #204는 local/demo Docker Compose와 운영 runtime 산출물을 분리하는 작업이다. #200에서 health/prometheus endpoint가 생겼고, #202에서 app image hardening과 DB backup/restore baseline이 생겼으며, #203에서 Flyway migration baseline이 정리됐다.

기존 `docker-compose.yml`은 local/demo 실행을 위한 파일이다. frontend도 `node:22-alpine`에서 Vite dev server로 실행하므로 운영 정적 서빙 산출물로 보기 어렵다.

## Decision

- local/demo runtime은 기존 `docker-compose.yml`로 유지한다.
- prod runtime은 `docker-compose.prod.yml`로 분리한다.
- prod compose는 top-level project name `smartcloset-prod`와 기본 명시 volume name `smartcloset-prod-mysql-data`, `smartcloset-prod-clothing-image-data`를 사용해 local/demo Docker volume과 충돌하지 않게 한다.
- prod compose는 `SPRING_PROFILES_ACTIVE=prod`를 고정하고, secret과 URL 성격의 필수 env를 `${VAR:?message}`로 요구한다.
- prod app은 Flyway enabled, Hibernate `ddl-auto=validate`, `SMARTCLOSET_SEED_ENABLED=false`, Swagger/OpenAPI disabled 기본값으로 실행한다.
- prod app은 refresh cookie와 OAuth state cookie의 `Secure=true`, `SameSite=None` 기본값을 사용한다.
- `ProdProfileSafetyGuard`는 prod profile에서 local JWT secret, unsafe ddl-auto, insecure refresh/OAuth state cookie를 fail-fast로 막는다.
- frontend production image는 `frontend/Dockerfile`에서 Vite build 산출물을 만들고 Nginx non-root static server로 서빙한다.
- `.env.prod.example`은 운영 env/secret checklist로 제공하되 실제 secret 값은 비워 둔다.
- `scripts/prod-compose-smoke.sh`는 임시 env로 prod compose를 빌드/기동해 app health, public API, disabled OpenAPI, frontend health/root HTML을 확인한다. Smoke project override는 `SMARTCLOSET_PROD_SMOKE_PROJECT`만 허용하고 `smartclosetprodsmoke` prefix로 제한하며, volume name도 smoke project prefix로 override한다.

## Consequences

- local/demo 실행과 prod-like runtime 검증이 파일 수준에서 분리된다.
- frontend production path가 Vite dev server에 의존하지 않는다.
- prod profile이 insecure cookie와 local secret으로 기동되는 실수를 더 빨리 발견한다.
- 현재 범위에는 AWS, RDS, Kubernetes, Secrets Manager, external TLS termination, CD automation, Redis/session externalization을 포함하지 않는다.

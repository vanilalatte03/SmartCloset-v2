# Docker runtime과 DB 운영 하드닝

## Status

Accepted

## Context

운영 준비 이슈 #202는 Dockerfile runtime과 MySQL 운영 절차의 남은 위험을 다룬다. #200에서 Actuator health endpoint가 생겼고, #203에서 Flyway migration 기준이 생겼으므로 container healthcheck와 DB backup/restore runbook을 현재 운영 baseline에 연결할 수 있다.

기존 runtime image는 root user로 실행됐고, Dockerfile healthcheck와 JVM container memory option이 없었다. MySQL은 Docker volume만 있었고 backup/restore 절차가 문서와 스크립트로 고정되어 있지 않았다.

## Decision

- app runtime image는 UID/GID `10001:10001`의 `smartcloset` non-root user로 실행한다.
- runtime image에는 Actuator health endpoint를 확인할 수 있도록 `curl`을 포함한다.
- Dockerfile healthcheck는 `SMARTCLOSET_HEALTHCHECK_URL` 기본값 `http://127.0.0.1:8080/actuator/health`를 호출한다.
- Docker Compose frontend service는 app service가 healthy가 된 뒤 시작한다.
- JVM container memory option은 표준 `JAVA_TOOL_OPTIONS`로 주입하고 local 기본값은 `-XX:MaxRAMPercentage=75.0`이다.
- image volume 경로 `/data/smartcloset/clothing-images`는 non-root user가 쓸 수 있게 image build 단계에서 소유권을 맞춘다.
- Docker Compose는 `clothing-image-volume-permissions` one-shot service로 기존 `clothing-image-data` volume 소유권을 app UID/GID `10001:10001`로 보정한 뒤 app을 시작한다.
- MySQL backup은 `scripts/mysql-backup.sh`로 생성한다.
- MySQL restore는 `SMARTCLOSET_RESTORE_CONFIRM=restore scripts/mysql-restore.sh <backup.sql>`처럼 명시 확인이 있어야 실행되며, 기존 DB에 dump를 replay하고 extra object를 자동 제거하지 않는다.
- backup dump는 `backups/` local artifact로 취급하고 git과 Docker build context에서 제외한다.
- schema 생성/변경은 ADR-017의 Flyway migration 정책을 유지한다.

## Consequences

- app container가 root 권한 없이 실행되어 container breakout 또는 파일 권한 실수의 blast radius가 줄어든다.
- Docker/Compose가 app health를 Actuator 기준으로 판단할 수 있다.
- JVM memory 비율을 배포 환경별 env로 조정할 수 있다.
- 기존 local 이미지 volume을 재사용해도 non-root app이 이미지 파일을 쓸 수 있다.
- local MySQL backup/restore 절차를 스크립트와 smoke command로 검증할 수 있다.
- 현재 범위에는 RDS snapshot, point-in-time recovery, offsite backup encryption, restore drill automation, Kubernetes probes, AWS 배포 산출물을 포함하지 않는다.

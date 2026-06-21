# Architecture Decision Records

## 운영 규칙
- ADR 본문은 `docs/adr/` 아래에 둔다.
- 이 파일은 ADR 인덱스 역할만 유지한다.
- ADR은 결정을 바꾸기 전에 먼저 추가하거나 갱신한다.

## ADR 목록

ADR-008은 MVP-3 완료 baseline의 인증 사용자 기반 API 계약을 정의하며, 이전 ADR의 `userId` request parameter 기반 API 표현보다 우선한다.

ADR-009는 MVP4를 백엔드 API/DB/추천 규칙 변경이 아닌 반응형 웹 실사용 UX 범위로 확정하고, Step 7 P0 release cut 이후 Step 8-13을 P1 polish tail로 분리한다.

ADR-010은 MVP5를 옷 이미지 업로드 MVP로 확정하고, 기존 옷 JSON API 유지와 별도 보호 이미지 API, Docker Compose 로컬 볼륨 저장 방식을 정의한다.

ADR-011은 MVP6를 추천 피드백/개인화 MVP로 확정하고, 추천 상황, 옷별 `styleTags`, 추천 피드백 snapshot, `preferenceScore` 내부 확장 방식을 정의한다.

ADR-012는 MVP7을 위치/날씨 신뢰도 MVP로 확정하고, KMA 행정구역 catalog, 브라우저 좌표 resolve, 예보 시간대 선택, 위치/날씨 source snapshot 저장 방식을 정의한다.

ADR-013은 MVP8을 계정 안정성 MVP로 확정하고, refresh token session, 이메일 인증, 비밀번호 재설정, Google login, 세션 만료 UX, 계정/데이터 삭제, MVP9 AWS-ready adapter 경계를 정의한다.

ADR-014는 원래 MVP9 후보였던 AWS 배포를 후속 MVP로 연기하고, MVP9를 프론트 UI/UX 리디자인 MVP로 확정한다.

ADR-015는 기존 `clothing_items.archived` 컬럼을 재사용해 옷장 보관함 조회와 보관 해제 API/UX를 추가하고, DB schema와 추천 규칙은 유지한다.

ADR-016은 MVP10을 AI 옷 등록 보조 MVP로 확정하고, Spring AI와 OpenAI `gpt-5.4-nano`로 사진 기반 등록 후보를 제안하되 추천 규칙과 DB schema는 유지한다.

ADR-017은 운영 준비 작업으로 Flyway schema migration을 재도입하고, 깨끗한 DB 생성은 migration, 운영 schema 검증은 Hibernate `ddl-auto=validate`로 분리한다.

ADR-018은 운영 준비 작업으로 Actuator와 Prometheus 기반 관측성 baseline을 도입하고, 추천/KMA/OpenAI 분석 metric과 local alert/dashboard 산출물을 정의한다.

ADR-019는 운영 준비 작업으로 Docker image non-root runtime, Actuator 기반 container healthcheck, JVM memory env, MySQL backup/restore runbook baseline을 정의한다.

ADR-020은 운영 준비 작업으로 local/demo compose와 분리된 prod compose, Nginx 기반 frontend production image, prod cookie/secret/schema runtime guard와 smoke 검증을 정의한다.

ADR-021은 운영 준비 작업으로 PR CI의 dependency vulnerability scan, backend/frontend Docker image build validation, image vulnerability scan gate를 정의한다.

ADR-022는 운영 준비 작업으로 PR CI의 backend 정적 분석, JaCoCo coverage verification, frontend lint/test 품질 게이트를 정의한다.

ADR-023은 운영 준비 작업으로 Spring AI milestone 의존성을 Spring AI 2.0.0 GA로 전환하고, Boot 4.0.6 baseline과 AI 분석 계약을 유지한다.

- [ADR-001: Use StaticWeatherProvider for MVP Weather](adr/001-static-weather-provider.md)
- [ADR-002: Share MVP with Docker Compose](adr/002-docker-compose-sharing.md)
- [ADR-003: MVP Scope and Implementation Constraints](adr/003-mvp-scope-decisions.md)
- [ADR-004: Use Spring Boot 4.0.6](adr/004-spring-boot-version.md)
- [ADR-005: Harness PR Autopilot Workflow](adr/005-harness-pr-autopilot-workflow.md)
- [ADR-006: Use KMA Vilage Forecast Weather Provider](adr/006-kma-vilage-forecast-weather-provider.md)
- [ADR-007: Use User Location Catalog and React TypeScript Frontend](adr/007-mvp2-user-location-and-react-frontend.md)
- [ADR-008: Use Authenticated User APIs and Preference Score](adr/008-mvp3-authenticated-user-personalization.md)
- [ADR-009: Define MVP4 as Responsive Usable UX](adr/009-mvp4-usable-ux.md)
- [ADR-010: Define MVP5 as Clothing Image Upload](adr/010-mvp5-clothing-images.md)
- [ADR-011: Define MVP6 as Feedback Personalization](adr/011-mvp6-feedback-personalization.md)
- [ADR-012: Define MVP7 as Location Weather Trust](adr/012-mvp7-location-weather-trust.md)
- [ADR-013: Define MVP8 as Account Stability](adr/013-mvp8-account-stability.md)
- [ADR-014: Define MVP9 as UI/UX Redesign](adr/014-mvp9-ui-ux-redesign.md)
- [ADR-015: Define Closet Archive Restore](adr/015-closet-archive-restore.md)
- [ADR-016: Define MVP10 as AI Clothing Registration Assist](adr/016-mvp10-ai-clothing-registration-assist.md)
- [ADR-017: Reintroduce Production Schema Migration](adr/017-production-schema-migration.md)
- [ADR-018: Introduce Observability Baseline](adr/018-observability-baseline.md)
- [ADR-019: Harden Docker Runtime and DB Operations](adr/019-docker-db-hardening.md)
- [ADR-020: Define Production Runtime Artifacts](adr/020-prod-runtime-artifacts.md)
- [ADR-021: Add CI Security and Image Gates](adr/021-ci-security-image-gates.md)
- [ADR-022: Add CI Quality and Coverage Gates](adr/022-ci-quality-coverage-gates.md)
- [ADR-023: Promote Spring AI Dependency to 2.0.0 GA](adr/023-spring-ai-ga-dependency.md)

# Architecture Decision Records

## 운영 규칙
- ADR 본문은 `docs/adr/` 아래에 둔다.
- 이 파일은 ADR 인덱스 역할만 유지한다.
- ADR은 결정을 바꾸기 전에 먼저 추가하거나 갱신한다.

## ADR 목록

ADR-008은 MVP-3 완료 baseline의 인증 사용자 기반 API 계약을 정의하며, 이전 ADR의 `userId` request parameter 기반 API 표현보다 우선한다.

MVP4에서 현재 결정을 바꾸려면 ADR-008을 직접 덮어쓰기보다 새 ADR을 추가하거나 명시적으로 갱신한다.

- [ADR-001: Use StaticWeatherProvider for MVP Weather](adr/001-static-weather-provider.md)
- [ADR-002: Share MVP with Docker Compose](adr/002-docker-compose-sharing.md)
- [ADR-003: MVP Scope and Implementation Constraints](adr/003-mvp-scope-decisions.md)
- [ADR-004: Use Spring Boot 4.0.6](adr/004-spring-boot-version.md)
- [ADR-005: Harness PR Autopilot Workflow](adr/005-harness-pr-autopilot-workflow.md)
- [ADR-006: Use KMA Vilage Forecast Weather Provider](adr/006-kma-vilage-forecast-weather-provider.md)
- [ADR-007: Use User Location Catalog and React TypeScript Frontend](adr/007-mvp2-user-location-and-react-frontend.md)
- [ADR-008: Use Authenticated User APIs and Preference Score](adr/008-mvp3-authenticated-user-personalization.md)

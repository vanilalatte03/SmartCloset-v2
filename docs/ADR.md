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

# MVP 1 Decisions

MVP 1의 상세 결정 기록은 현재 `docs/adr/`에 유지한다. 이 문서는 주요 결정만 요약한다.

## 주요 결정
- 날씨 연동은 1차에서 제외하고 `StaticWeatherProvider` 고정 날씨로 추천 도메인을 검증했다. 자세한 내용: ../../docs/adr/001-static-weather-provider.md
- 공유 방식은 AWS 배포가 아니라 Docker Compose로 고정했다. 자세한 내용: ../../docs/adr/002-docker-compose-sharing.md
- 1차 MVP 범위는 추천 백엔드, API 계약, Swagger/Demo UI 검증으로 제한했다. 자세한 내용: ../../docs/adr/003-mvp-scope-decisions.md
- Spring Boot 버전은 `4.0.6`으로 고정했다. 자세한 내용: ../../docs/adr/004-spring-boot-version.md
- Harness step 단위 자동 PR 루프를 운영 흐름으로 정했다. 자세한 내용: ../../docs/adr/005-harness-pr-autopilot-workflow.md

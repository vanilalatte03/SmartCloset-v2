# Use Spring Boot 4.0.6

## Status
Accepted

## Context
SmartCloset 1차 MVP는 Java 21 기반 Spring Boot 백엔드로 구현한다. 구현 전에 Spring Boot 버전을 명확히 고정하지 않으면 Gradle 스캐폴드, 의존성 호환성, 테스트/빌드 결과가 작업자마다 달라질 수 있다.

## Decision
1차 MVP의 Spring Boot 버전은 `4.0.6`으로 고정한다.

Step 1 `project-scaffold`에서 `build.gradle` 또는 동등한 Gradle 설정을 만들 때 Spring Boot plugin/version은 `4.0.6`을 사용한다.

## Consequences
- 구현 단계의 Spring Boot 의존성 해석 기준이 명확해진다.
- Swagger/OpenAPI, JPA, Validation, MySQL driver 등 관련 의존성은 Spring Boot `4.0.6`과의 호환성을 기준으로 선택한다.
- 버전 변경이 필요하면 이 ADR을 먼저 갱신한 뒤 PRD, ARCHITECTURE, phase step 문서를 함께 동기화한다.

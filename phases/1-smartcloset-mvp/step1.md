# 단계 1: project-scaffold

범위: Must-have / P0

## 읽어야 할 파일
- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/ARCHITECTURE.md`
- `docs/API.md`
- `docs/adr/004-spring-boot-version.md`
- `docs/adr/003-mvp-scope-decisions.md`
- `docs/COMMANDS.md`
- `phases/1-smartcloset-mvp/step0.md`

## 작업
Java 21 Spring Boot 4.0.6 백엔드 프로젝트의 최소 실행 골격과 공통 API 응답/예외 처리 기반을 만든다.

## 변경 예상 파일
- `settings.gradle`
- `build.gradle`
- `gradlew`, `gradlew.bat`, `gradle/wrapper/*`
- `src/main/java/com/smartcloset/SmartClosetApplication.java`
- `src/main/java/com/smartcloset/common/**`
- `src/main/resources/application.yml`
- `src/test/java/com/smartcloset/**`

## 구현 메모
- 패키지 루트는 `com.smartcloset`로 둔다.
- Spring Boot 버전은 `4.0.6`으로 고정한다.
- 의존성은 Spring Web, Spring Data JPA, Validation, MySQL driver, Springdoc OpenAPI, JUnit 기반 테스트를 포함한다.
- Lombok은 ADR 정책에 맞게 제한적으로 사용한다. Entity에는 `@Data`와 무분별한 `@Setter`를 쓰지 않는다.
- 공통 성공 응답은 `{ "data": ... }`, 공통 실패 응답은 `{ "code": "...", "message": "...", "details": [] }` 형태로 준비한다.
- Swagger UI와 OpenAPI JSON을 사용할 수 있게 구성한다.
- 인증, Spring Security, 외부 Weather API, Redis, AI/GPT 관련 의존성을 추가하지 않는다.

## 검증 절차
```bash
./gradlew test
./gradlew build
```

## 인수 기준
- Spring Boot 4.0.6 애플리케이션이 컴파일된다.
- 기본 context load 테스트가 통과한다.
- 공통 응답/예외 구조가 이후 API step에서 재사용 가능한 형태로 준비된다.
- 금지 범위 의존성이 추가되지 않았다.

## 금지사항
- Spring Security를 추가하지 마라. 이유: 1차 MVP는 `userId` request parameter 기반이다.
- 외부 Weather API client를 추가하지 마라. 이유: 1차 MVP는 StaticWeatherProvider만 사용한다.

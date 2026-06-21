# Spring AI 의존성을 2.0.0 GA로 전환

## 상태

Accepted

## 배경

MVP10은 Spring Boot 4.0.6 baseline을 유지하기 위해 Spring AI 2.0 preview 계열로 옷 등록 후보 제안 기능을 도입했다. 운영 준비 이슈 #206에서는 milestone 의존성을 그대로 운영에 둘지, GA 또는 지원 버전으로 전환할지 결정해야 한다.

2026-06-21 기준 Maven Central metadata에서 `org.springframework.ai:spring-ai-bom` 최신 release는 `2.0.0`이다. 따라서 preview 의존성 유지보다 GA 버전 전환이 운영 안정성 기준에 맞다.

로컬 dependency graph 확인 결과 Spring AI 2.0.0 starter는 일부 Spring Boot 4.1.0 module을 요청할 수 있다. SmartCloset의 현재 baseline은 Spring Boot 4.0.6이므로, GA 전환과 별개로 Boot dependency management를 더 강하게 유지해야 한다.

## 결정

- `springAiVersion`은 `2.0.0`으로 고정한다.
- Spring Boot baseline은 `4.0.6`으로 유지한다.
- Gradle은 Spring Boot BOM을 `enforcedPlatform`으로 적용해 Spring AI starter의 transitive Boot module 요청보다 Boot 4.0.6 baseline을 우선한다.
- OpenAI starter는 기존 `org.springframework.ai:spring-ai-starter-model-openai`를 유지한다.
- AI 옷 등록 보조 기본 모델은 `gpt-5.4-nano`를 유지한다.
- `CLOTHING_ANALYSIS_ENABLED=false`, `SPRING_AI_MODEL_CHAT=none`, 빈 `OPENAI_API_KEY` 기본값은 유지해 local/demo 실행을 깨뜨리지 않는다.
- Provider adapter, timeout, malformed output 처리, disabled analyzer 계약은 기존 테스트로 계속 검증한다.
- Public API, DB schema, 추천 점수, 추천 이유, AI 분석 결과 비저장 정책은 변경하지 않는다.

## 결과

- 운영 의존성 기준에서 Spring AI milestone suffix를 제거한다.
- Runtime classpath에서 Spring AI module은 `2.0.0`, Spring Boot module은 `4.0.6`으로 정렬된다.
- Spring AI API 변화 리스크는 `ClothingImageAnalyzer` provider boundary 안에 계속 격리된다.
- #197의 provider resilience 작업은 GA 전환 이후의 provider timeout/retry/circuit breaker 개선에 집중할 수 있다.
- 향후 Spring AI patch release로 올릴 때는 동일하게 dependency graph와 AI 분석 config/provider 테스트를 먼저 확인한다.

## 검증

- `./gradlew dependencyInsight --configuration runtimeClasspath --dependency org.springframework.ai:spring-ai-starter-model-openai`
- `./gradlew dependencyInsight --configuration runtimeClasspath --dependency org.springframework.boot:spring-boot-starter-webmvc`
- `./gradlew dependencyInsight --configuration runtimeClasspath --dependency org.springframework.boot:spring-boot-starter-restclient`
- `./gradlew test --tests com.smartcloset.clothing.infrastructure.analysis.*`
- `./gradlew test --tests com.smartcloset.clothing.application.ClothingAnalysisServiceTest`
- `./gradlew test`

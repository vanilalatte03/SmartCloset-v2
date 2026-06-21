# Structured logging과 tracing baseline 도입

## 상태

Accepted

## 배경

운영 준비 이슈 #201은 일반 텍스트 로그만으로는 장애 시 request 단위 추적, provider 장애 상관관계 분석, auth/recommendation failure 조사에 필요한 신호가 부족하다는 점을 다룬다. #200에서 Prometheus metric과 alert/dashboard baseline을 도입했으므로, 이번 결정은 log/trace correlation을 보강한다.

Spring Boot 4 baseline에서는 structured logging과 OpenTelemetry starter를 사용할 수 있다. 별도 log encoder나 vendor SDK를 먼저 추가하기보다 Spring Boot 기본 기능을 사용하면 local/demo 실행을 단순하게 유지하면서 운영 collector로 확장할 수 있다.

## 결정

- Console log 기본 format은 Spring Boot built-in ECS JSON으로 둔다.
- API/security error log는 SLF4J key-value field로 `code`, `status`, `method`, `path`, `exception`, 고정 `error_message`를 남긴다.
- Request body, Authorization header, cookie, query string, raw exception message, token/action token/API key/password는 로그에 남기지 않는다.
- Local/demo 이메일 인증과 비밀번호 재설정 action token은 SLF4J 로그가 아니라 `SMARTCLOSET_EMAIL_OUTBOX_PATH` local outbox 파일에만 남긴다.
- `spring-boot-starter-opentelemetry`를 추가해 Micrometer tracing과 OTLP export 경계를 마련한다.
- Sampling 기본값은 `1.0`이며 `MANAGEMENT_TRACING_SAMPLING_PROBABILITY`로 조정한다.
- OTLP trace export는 `MANAGEMENT_TRACING_EXPORT_OTLP_ENABLED`로 켜고, endpoint는 `MANAGEMENT_OPENTELEMETRY_TRACING_EXPORT_OTLP_ENDPOINT`로 주입한다. 기본 local/demo 값은 trace/log/metrics OTLP export 비활성이라 collector 없이 실행된다.
- 현재 span이 있으면 모든 HTTP 응답에 `X-Trace-Id` header를 추가한다.
- Sentry는 이번 PR에서 도입하지 않는다. 특정 SaaS SDK, DSN secret, PII scrubbing 정책은 운영 backend 선택이 필요하므로 후속 ADR에서 결정한다.

## 결과

- local 실행에서 JSON structured log를 확인할 수 있다.
- API/auth/provider failure log는 trace/log aggregation에서 field 검색이 가능한 형태로 남는다.
- 사용자는 `X-Trace-Id`를 포함해 장애를 제보할 수 있고, 운영자는 해당 trace id로 로그와 trace를 연결할 수 있다.
- OTLP collector가 없는 local/demo 환경은 기존 실행 흐름을 유지한다.
- Public API response body, DB schema, 추천 규칙, AI 옷 등록 보조 계약은 변경하지 않는다.

## 검증

- `./gradlew test --tests com.smartcloset.common.observability.* --tests com.smartcloset.common.exception.GlobalExceptionHandlerTest --tests com.smartcloset.security.SecurityErrorResponseWriterTest`
- `./gradlew test`
- `./gradlew build`
- `python3 scripts/checks.py --docs-check --include-final-docs`

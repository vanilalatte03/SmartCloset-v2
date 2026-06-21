# 운영 관측성 baseline 도입

## Status

Accepted

## Context

운영 준비 이슈 #200은 추천 생성, KMA 날씨 provider, OpenAI 옷 분석 provider, DB pool, JVM heap의 상태를 운영자가 빠르게 확인할 수 있는 최소 관측성 baseline을 요구한다.

기존 애플리케이션은 오류 응답 로그와 테스트 중심 검증은 갖고 있었지만, 운영 중 실패율과 latency, provider fallback/장애, 리소스 saturation을 metric으로 분리해 볼 수 있는 endpoint와 dashboard/alert 기준이 없었다.

## Decision

Spring Boot Actuator와 Micrometer Prometheus registry를 사용한다.

- `/actuator/health`와 `/actuator/prometheus`를 노출한다.
- local 기본 노출은 `health,info,prometheus`, prod profile 기본 노출은 `health,prometheus`로 둔다.
- health detail은 기본적으로 숨긴다.
- `SmartClosetMetrics`에서 custom metric 이름과 low-cardinality tag를 중앙 관리한다.
- 추천 생성에는 요청 counter와 duration timer를 기록하고 `situation`, `forecast_period`, `outcome` tag를 사용한다.
- KMA provider에는 요청 counter와 duration timer를 기록하고 `provider=kma_vilage_forecast`, `forecast_period`, `outcome` tag를 사용한다.
- OpenAI 옷 분석 provider에는 요청 counter와 duration timer를 기록하고 `provider=openai`, `outcome` tag를 사용한다.
- `monitoring/prometheus/alerts.yml`에 추천 실패율, 추천 p99 latency, KMA fallback/failure 비율, OpenAI 분석 장애 비율, HikariCP pool saturation, JVM heap 사용률 alert rule baseline을 둔다.
- `monitoring/alertmanager/alertmanager.yml`은 실제 외부 알림이 없는 local null receiver baseline으로 둔다.
- `monitoring/grafana/smartcloset-dashboard.json`은 Prometheus datasource로 import 가능한 dashboard baseline으로 둔다.

## Consequences

- 운영자는 health endpoint로 기동 상태를, Prometheus endpoint로 JVM/HikariCP와 SmartCloset custom metric을 확인할 수 있다.
- 추천 실패, 외부 provider 장애/fallback, AI 분석 비활성/장애, 리소스 saturation을 서로 다른 metric과 alert rule로 분리해 볼 수 있다.
- Metric tag에는 user id, token, image path, raw exception message, provider secret을 넣지 않는다.
- 실제 운영 배포에서는 Prometheus endpoint를 공개 인터넷에 직접 노출하지 않고 내부 네트워크나 proxy allowlist 뒤에 둔다.
- 현재 범위에는 외부 webhook, Slack/PagerDuty, Prometheus/Grafana container compose 서비스, tracing, log aggregation, AWS/RDS/Secrets Manager 통합을 추가하지 않는다.

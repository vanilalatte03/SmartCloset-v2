# Issue Triage

이 문서는 SmartCloset 운영 준비 이슈를 만들 때 사용하는 기준이다. 리뷰어 의견은 그대로 복사하지 않고, 현재 코드에서 확인한 사실과 판단을 함께 남긴다.

## Production Readiness 이슈 생성

운영 준비 작업은 GitHub의 `Production Readiness` issue form으로 만든다.

form이 요구하는 핵심 항목:

- `Priority`: 운영 출시 차단 정도
- `Areas`: 필터링할 작업 영역
- `Type`: 구현 작업인지 조사 작업인지
- `재현 명령`: 실제 확인에 사용한 명령
- `핵심 에러`: 코드 기준으로 확인한 문제와 기각한 리뷰 주장
- `Review Findings`: 파일/라인 또는 명확한 근거
- `수정 방향`: 구현 방향
- `완료 기준`: 검증과 문서 동기화 기준
- `Tracked by`: 기본값 `#207`
- `Depends on`: 선행 이슈가 있으면 `#200`처럼 작성

## Priority 기준

`priority:P0`는 운영 시작 전 반드시 끝내야 하는 launch blocker다.

예시:

- prod secret, schema migration, health/readiness, 운영 runtime 정의
- 외부 provider 장애가 서비스 전체 장애로 번질 수 있는 resilience 문제
- 운영에서 데이터 손실이나 복구 불능으로 이어질 수 있는 문제

`priority:P1`은 운영 품질을 위해 중요하지만, 명확한 우회나 제한된 blast radius가 있는 작업이다.

예시:

- CI 보안/품질 게이트
- 구조화 로그와 tracing
- Dockerfile hardening
- quota/throttle 보강
- 운영 dependency 안정화 검토

`priority:P2`는 출시 후에도 계획적으로 처리 가능한 polish 또는 follow-up hardening이다.

## Area 라벨 기준

- `area:resilience`: timeout, retry, circuit breaker, fallback, stale cache
- `area:ci`: GitHub Actions, build/test/lint/coverage/security scan
- `area:observability`: actuator, metrics, dashboards, alerts, tracing, structured logs
- `area:security`: secret, cookie, auth, vulnerability gate, production exposure
- `area:db`: migration, schema, backup, restore, DB 운영 정책
- `area:deploy`: production compose, Kubernetes, release artifact, runtime env
- `area:docker`: Dockerfile, image hardening, container healthcheck, JVM flags
- `area:rate-limit`: quota, throttle, abuse protection
- `area:dependencies`: version pinning, milestone/GA 전환, dependency support policy

여러 영역에 걸치면 필요한 라벨을 모두 선택한다.

## Type 기준

`type:ops`는 바로 구현 가능한 운영 준비 작업이다.

`type:spike`는 먼저 조사나 의사결정이 필요한 작업이다. 예를 들어 Spring AI milestone 의존성을 GA로 전환할 수 있는지 확인해야 하는 경우가 여기에 해당한다.

`type:epic`은 여러 이슈를 순서대로 묶는 트래킹 이슈에만 사용한다.

## 의존성 작성 규칙

강한 선후관계가 있으면 본문에 GitHub issue reference를 명시한다.

예시:

```markdown
## Dependencies
- Tracked by #207.
- Depends on #200 before app/container healthcheck wiring.
- Related to #203 for DB migration policy.
```

트래커 이슈에는 체크리스트로 순서를 남긴다.

```markdown
- [ ] #203 스키마 마이그레이션 도구 재도입
- [ ] #200 운영 모니터링과 alerting baseline 도입
- [ ] #202 Dockerfile과 DB 운영 하드닝
```

## 자동 triage

`.github/workflows/issue-triage.yml`은 issue form 본문을 읽어 다음 작업을 자동 수행한다.

- 선택한 `priority:*` 라벨 적용
- 선택한 `area:*` 라벨 적용
- 선택한 `type:*` 라벨 적용
- `Production Readiness` 마일스톤 생성 또는 적용

workflow는 관리 대상 라벨만 정리한다. 사람이 붙인 다른 라벨은 유지한다.

`Tracked by`와 `Depends on`도 자동 처리한다.

- `Tracked by`에 적은 트래커 이슈 본문에는 새 이슈가 priority 섹션의 체크리스트로 추가된다.
- `Depends on`에 적은 `#123` 참조는 새 이슈 하단의 `## Dependencies` 섹션으로 정규화된다.
- `area:*` 값은 허용된 area 라벨 목록에 있는 값만 적용된다.
- `Production Readiness` 마일스톤은 열린 마일스톤만 재사용한다.

주의: 이 자동화는 default branch의 workflow가 기준이다. workflow 변경은 `main`에 머지된 뒤 새 이슈부터 적용된다.

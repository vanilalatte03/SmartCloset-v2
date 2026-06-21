# CI 품질 게이트와 커버리지 기준

## 상태

Accepted

## 배경

운영 준비 이슈 #205는 기존 PR CI가 backend test/build와 frontend build만 실행해 정적 분석, frontend lint/test, coverage 하한을 강제하지 못하는 위험을 다룬다. #198에서 보안 스캔과 image gate를 추가했으므로, 품질 게이트는 별도 실패 기준으로 분리한다.

## 결정

- Backend 정적 분석은 Gradle Checkstyle plugin으로 시작한다.
- 초기 Checkstyle rule은 star import, redundant import, unused import, tab character를 차단하는 보수적 rule set으로 제한한다.
- Backend coverage는 Gradle JaCoCo plugin으로 측정한다.
- JaCoCo XML/HTML report를 생성하고 line coverage `0.60` 미만이면 `jacocoTestCoverageVerification`을 실패시킨다.
- Gradle `check`는 JaCoCo coverage verification에 의존한다.
- Frontend는 ESLint flat config를 사용하고 `npm run lint`를 CI에 연결한다.
- Frontend는 Vitest를 사용하고 `npm test`를 CI에 연결한다.
- Frontend 첫 테스트 범위는 UI runtime을 변경하지 않는 utility regression test로 시작한다.
- Coverage threshold는 현재 테스트 자산을 기준으로 한 초기 운영 하한이며, 기능/테스트가 늘어날 때 별도 PR에서 상향한다.

## 결과

- PR CI가 backend static analysis, backend coverage verification, frontend lint, frontend test를 실행한다.
- Coverage report는 `build/reports/jacoco/test/` 아래에 생성된다.
- 품질 게이트 실패 기준은 보안 스캔과 분리되어 원인을 구분할 수 있다.
- Public API, DB schema, 추천 규칙, MVP10 AI 옷 등록 보조 계약은 변경하지 않는다.

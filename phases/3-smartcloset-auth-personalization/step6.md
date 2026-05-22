# 단계 6: recommendation-current-user-api

범위: Must-have / 3차 P0

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/API.md`
- `docs/ARCHITECTURE.md`
- `docs/ERD.md`
- `docs/RECOMMENDATION_RULES.md`
- `docs/COMMANDS.md`
- `src/main/java/com/smartcloset/recommendation/**`
- `src/main/java/com/smartcloset/weather/**`
- `src/main/java/com/smartcloset/location/**`
- `src/main/java/com/smartcloset/clothing/**`
- `src/main/java/com/smartcloset/user/**`
- `src/test/java/com/smartcloset/recommendation/**`
- `src/test/java/com/smartcloset/weather/**`

이전 단계에서 만들어진 코드를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업
추천 생성, 추천 이력 조회, 착용 완료 API를 현재 인증 사용자 기준으로 전환한다. 기존 `userId` query parameter와 today 추천 GET 경로를 사용하지 않는다. 이전 step에서 교체한 `preferenceScore` score model을 유지한 채 HTTP/API와 weather 위치 연동만 전환한다.

이 단계가 끝나면 백엔드 주요 API는 모두 인증 사용자 기준으로 전환되어 있어야 한다. 다만 최종 공개/보호 API 회귀 테스트와 임시 permit rule 제거는 Step 7에서 수행한다.

## 변경 예상 파일
- `src/main/java/com/smartcloset/recommendation/presentation/**`
- `src/main/java/com/smartcloset/recommendation/application/**`
- `src/main/java/com/smartcloset/recommendation/domain/**`
- `src/main/java/com/smartcloset/recommendation/dto/**`
- `src/main/java/com/smartcloset/recommendation/repository/**`
- `src/main/java/com/smartcloset/weather/application/**`
- `src/main/java/com/smartcloset/weather/infrastructure/**`
- `src/test/java/com/smartcloset/recommendation/**`
- `src/test/java/com/smartcloset/weather/**`

## 구현 메모
- 대상 API:
  - `POST /api/recommendations`
  - `GET /api/recommendations?limit={limit}`
  - `PATCH /api/recommendations/{recommendationId}/worn`
- 추천 생성 request body는 없다.
- 추천 생성은 현재 인증 사용자 위치 `nx`, `ny`로 KMA `getVilageFcst` JSON을 조회한다.
- KMA 요청의 `nx`, `ny` source of truth는 현재 인증 사용자의 저장 위치다.
- 기존 `KMA_NX`, `KMA_NY`는 compatibility/default helper로만 둔다.
- fallback 정책은 기존 1.5차 기준을 유지한다.
- 추천 결과, 추천 이력, 착용 완료 응답에는 `userId`를 넣지 않는다.
- 추천 score 응답과 저장 snapshot은 `preferenceScore`를 사용하고 기존 다양성 점수 필드를 되살리지 않는다.
- 추천 이력 limit 정책:
  - 기본값 `20`
  - 최소 `1`
  - 최대 `50`
  - 최신순
  - invalid value는 `400 INVALID_REQUEST`
- 착용 완료는 idempotent해야 한다.
- 다른 사용자의 추천 결과를 착용 완료 처리하면 `RECOMMENDATION_NOT_FOUND`로 실패한다.
- 추천 business failure 5종은 HTTP 422로 유지한다.
- `SecurityConfig`는 추천 생성, 추천 이력, 착용 완료 API를 Bearer token 필수로 만든다.
- 이 단계에서 발견한 남은 임시 permit rule은 제거하지 말고 Step 7 검증 대상으로 남긴다. 단, 추천 API가 임시 허용에 남아 있으면 이 단계에서 수정한다.

## 검증 절차
```bash
git diff --check
! rg -n 'GET /api/recommendations/(today)' src/main/java/com/smartcloset/recommendation src/test/java/com/smartcloset/recommendation
! rg -n -F -e 'POST /api/recommendations?userId' src/main/java/com/smartcloset/recommendation src/test/java/com/smartcloset/recommendation
! rg -n -e '@RequestParam.*userId' -e 'RequestParam Long userId' -e '\\.param\\("userId"' src/main/java/com/smartcloset/recommendation src/test/java/com/smartcloset/recommendation
rg -n 'preferenceScore' src/main/java src/test/java
rg -n '/api/recommendations' src/main/java/com/smartcloset/security src/test/java/com/smartcloset/security src/test/java/com/smartcloset/recommendation
./gradlew test
```

## 인수 기준
- token 없이 추천 API를 호출하면 401로 실패한다.
- 유효 token으로 `POST /api/recommendations`가 현재 사용자 옷장/위치/선호도 기준으로 추천을 생성한다.
- 추천 생성 request mapping에 `userId` query parameter가 없다.
- KMA provider 또는 weather 조회 경로가 현재 사용자 위치 `nx`, `ny`를 사용한다.
- `GET /api/recommendations`가 현재 사용자 추천 이력만 최신순으로 반환한다.
- 추천 이력 limit default/min/max/invalid test가 있다.
- `PATCH /api/recommendations/{recommendationId}/worn`은 idempotent하며 중복 `WearHistory`를 만들지 않는다.
- 사용자 A token으로 사용자 B의 추천 이력과 착용 완료를 처리할 수 없다.
- today 추천 GET 경로가 코드와 테스트에 새로 생기지 않는다.
- 추천 응답 score에는 `preferenceScore`가 있고 기존 다양성 점수 필드는 없다.
- 추천 API가 Step 6 종료 시점에 임시 permit 대상에 남아 있지 않다.

## 금지사항
- today 추천 GET endpoint를 추가하지 마라. 이유: 3차 추천 생성 API는 `POST /api/recommendations`만 사용한다.
- 추천 이력 조회를 사용자 전체 조회로 구현하지 마라. 이유: 현재 인증 사용자 기준 최신순이어야 한다.
- 착용 완료 중복 호출 때 wear history를 중복 생성하지 마라. 이유: idempotent 계약이다.
- KMA 원본 DTO를 추천 domain service에 넘기지 마라. 이유: 추천 도메인은 내부 `WeatherCondition`에만 의존해야 한다.
- 추천 실패 코드를 500으로 뭉개지 마라. 이유: 추천 business failure는 422다.
- 기존 다양성 점수 필드를 되살리지 마라. 이유: 이전 step에서 `preferenceScore`로 교체했다.
- 최종 security cleanup을 이 단계에 섞지 마라. 이유: Step 7에서 전체 공개/보호 API 회귀 테스트와 함께 제거해야 한다.

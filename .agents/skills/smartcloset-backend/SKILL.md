---
name: smartcloset-backend
description: Use when implementing or reviewing the SmartCloset 1차 MVP Spring Boot 4.0.6 backend, including rule-based outfit recommendations, StaticWeatherProvider, JPA entities, APIs, tests, Docker Compose sharing, and documentation sync.
---

# SmartCloset Backend Skill

## Purpose
Use this skill before implementing SmartCloset 1차 MVP backend work.

The project is a Spring Boot 4.0.6 backend centered on rule-based outfit recommendation and Docker Compose sharing. Keep implementation consistent with `docs/PRD.md`, `docs/API.md`, `docs/ERD.md`, and `docs/RECOMMENDATION_RULES.md`.

## Scope

### P0
- Seed user 기준 동작
- Spring Boot 4.0.6 기반 프로젝트 구성
- Clothing 등록/목록 API
- `StaticWeatherProvider`
- 추천 생성 API
- `RecommendationResult` 저장
- 추천 결과 착용 완료 처리
- Swagger/OpenAPI
- Docker Compose 실행 기준
- README와 `docs/DEMO_SCENARIO.md` 기준 충족

### P1
- Spring Boot static resource 기반 최소 Demo UI
- 옷 상세/수정/보관 API
- GitHub Actions test/build

### Out of Scope
- 외부 Weather API
- AWS 수동 배포
- CD 자동화
- 로그인/회원가입
- Spring Security
- AI/GPT 추천
- 이미지 업로드
- Redis
- 쇼핑몰 추천
- 관리자 기능
- 정식 프론트엔드 앱

## API Rules
- 추천 생성은 반드시 `POST /api/recommendations?userId={userId}`를 사용한다.
- `GET /api/recommendations/today`는 사용하지 않는다.
- `userId`는 request parameter로 전달한다.
- 성공 응답은 `{ "data": ... }` 형태를 따른다.
- 실패 응답은 `{ "code": "...", "message": "...", "details": [] }` 형태를 따른다.
- 추천 실패는 HTTP `422 Unprocessable Entity`로 응답한다.
- `/worn` 처리는 idempotent하게 성공해야 한다.
- archive 처리는 idempotent하게 성공해야 한다.

## Domain Rules
- 추천 로직은 Controller에 두지 않는다.
- Repository에는 추천 점수 계산 로직을 두지 않는다.
- `RecommendationService`는 유스케이스 조합과 트랜잭션 경계를 담당한다.
- 추천 세부 계산은 도메인 서비스로 분리한다:
  - `WeatherSuitabilityFilter`
  - `OutfitCandidateGenerator`
  - `RecommendationScorer`
  - `RecommendationReasonGenerator`
- `OutfitCandidate`는 DB Entity가 아니라 계산용 도메인 모델 또는 value object다.

## Weather Rules
- `WeatherProvider` 인터페이스에 의존한다.
- 구현체는 `StaticWeatherProvider` 하나로 둔다.
- 기본 날씨는 `temperature=12`, `weatherType=CLOUDY`, `rainy=false`, `windy=false`다.
- 외부 Weather API를 호출하지 않는다.

## Recommendation Rules
- 추천 규칙은 `docs/RECOMMENDATION_RULES.md`를 기준으로 구현한다.
- 총점은 100점이다.
- `weatherScore`는 35점이다.
- `colorScore`는 25점이다.
- `wearHistoryScore`는 20점이다.
- `recommendationHistoryScore`는 10점이다.
- `diversityScore`는 10점이다.
- 추천 이유는 3개 이상 5개 이하로 생성한다.
- tie-break는 문서 기준으로 결정 가능하게 구현한다.

## Entity and JPA Rules
- Entity와 테이블은 `docs/ERD.md`를 기준으로 구현한다.
- 모든 Entity는 `BaseTimeEntity`를 사용한다.
- enum은 `VARCHAR`로 저장한다.
- `reasons_json`은 DB JSON 컬럼으로 두고, Entity는 `String reasonsJson`으로 보관한다.
- setter를 남발하지 않는다.
- Entity에 `@Data`를 사용하지 않는다.
- Entity에 `@Setter`를 남용하지 않는다.
- Entity는 `@Getter`와 protected no-args constructor 중심으로 둔다.
- 변경은 의도가 드러나는 메서드로 제한한다:
  - `updateDetails`
  - `archive`
  - `markWorn`

## Test Rules
- 추천 점수 계산 단위 테스트는 필수다.
- 날씨 필터링 테스트는 필수다.
- 색상, material, 온도 규칙 테스트는 필수다.
- 추천 실패 코드 5종 테스트는 필수다.
- 동일 입력에서 동일 추천 결과가 나오는지 테스트한다.
- P0 API는 통합 테스트 또는 controller/service 테스트로 검증한다.

## Documentation Sync Rules
- API 변경 시 `docs/API.md`, `README.md`, `docs/DEMO_SCENARIO.md`를 함께 확인한다.
- `GET /api/recommendations/today` 표현이 생기면 제거한다.
- `StaticWeatherProvider` 값이 문서 간 다르면 수정한다.
- Docker Compose가 유일한 필수 공유 방식인지 확인한다.

## Implementation Attitude
- P0를 먼저 끝낸 뒤 P1을 진행한다.
- 문서와 충돌하는 구현을 하지 않는다.
- 과도한 추상화를 피한다.
- 복잡한 확장보다 동작하는 MVP를 우선한다.
- 구현 후 테스트와 README 시나리오를 확인한다.

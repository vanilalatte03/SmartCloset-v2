# MVP Scope and Implementation Constraints

## Status
Accepted

## Context
SmartCloset 1차 MVP는 추천 도메인과 API 구현을 빠르게 검증해야 한다. 인증, AI 추천, 정식 프론트엔드, 이미지 업로드 같은 기능은 제품적으로 중요할 수 있지만, 1차 MVP의 일정과 테스트 가능성을 크게 흔들 수 있다.

따라서 1차 MVP에서는 추천 도메인, API 계약, Docker Compose 공유, Swagger/API 기반 검증을 우선한다.

## Decision
1차 MVP 범위와 구현 제약을 아래처럼 확정한다.

- 회원가입/로그인은 구현하지 않는다.
- 사용자 식별은 `userId` request parameter로 처리한다.
- AI/GPT 추천은 구현하지 않는다.
- 추천은 규칙 기반 점수 계산으로 구현한다.
- 정식 프론트엔드 앱은 구현하지 않는다.
- 최소 데모 UI는 Spring Boot static resource 단일 페이지로만 제공한다.
- 이미지 업로드, Redis, AWS 배포, CD 자동화는 제외한다.

Lombok 정책은 아래처럼 제한한다.

- DTO에는 Lombok 제한 사용을 허용한다.
- Entity에는 `@Getter`와 protected no-args constructor 정도만 허용한다.
- Entity에 `@Data`, `@Setter`를 남용하지 않는다.
- Entity 변경은 `archive()`, `markWorn()`, `updateDetails(...)`처럼 의도가 드러나는 메서드로 수행한다.

## Consequences
- 인증과 권한 설계 없이 seed/test user 기준으로 빠르게 추천 흐름을 검증할 수 있다.
- 추천 로직은 AI 결과가 아니라 테스트 가능한 규칙으로 검증된다.
- 정식 프론트엔드 기술 선택은 2차 MVP로 미룬다.
- Entity의 무분별한 상태 변경을 줄이고 도메인 메서드 중심으로 구현할 수 있다.
- 로그인, AI/GPT 추천, 이미지 업로드, Redis, AWS 배포, CD 자동화는 후속 MVP 후보로 이동한다.

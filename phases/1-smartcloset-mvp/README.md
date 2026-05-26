# Phase: SmartCloset 1차 MVP

> 상태: 완료된 과거 phase 문서다. 현재 구현 source of truth는 루트 `README.md`와 `docs/` 아래 현재 문서이며, 이 phase/step의 과거 API 또는 범위 표현이 현재 문서와 충돌하면 현재 문서를 우선한다. 완료 phase를 재실행할 때만 당시 step-local 기준으로 참고한다.

## 목표
SmartCloset 1차 MVP를 Docker Compose로 공유 가능한 Spring Boot 4.0.6 백엔드로 구현한다. P0는 Swagger 기준 공유 성공을 목표로 하고, P1은 P0 완료 후 공유 품질을 높이는 작업으로만 진행한다.

## 작업 범위
- Must-have / P0: Spring Boot 4.0.6 프로젝트 구성, MySQL JPA 모델, seed user/data, Clothing 등록/목록 API, StaticWeatherProvider, 규칙 기반 추천 생성, RecommendationResult 저장, 착용 완료 처리, Swagger/OpenAPI, Docker Compose 공유 기준 검증
- Should-have / P1: Clothing 상세/수정/보관 API, GitHub Actions test/build, Spring Boot static resource 기반 Demo UI

## 제외 범위
- 외부 Weather API
- AWS 배포
- CD 자동화
- 로그인/회원가입
- Spring Security
- AI/GPT 추천
- 이미지 업로드
- Redis
- 쇼핑몰 추천
- 관리자 기능
- 정식 프론트엔드 앱

## Steps
| Step | Name | Range |
| ---: | --- | --- |
| 0 | documentation-contract-sync | Must-have / P0 |
| 1 | project-scaffold | Must-have / P0 |
| 2 | persistence-and-seed | Must-have / P0 |
| 3 | clothing-p0-api | Must-have / P0 |
| 4 | recommendation-domain | Must-have / P0 |
| 5 | recommendation-p0-api | Must-have / P0 |
| 6 | p0-sharing-verification | Must-have / P0 |
| 7 | clothing-p1-api-ci | Should-have / P1 |
| 8 | demo-ui-p1 | Should-have / P1 |

## 완료 기준
- Docker Compose로 Spring Boot 4.0.6 앱과 MySQL이 실행된다.
- Swagger UI에서 P0 API를 호출할 수 있다.
- `userId=1` seed user 기준 옷 목록 조회, 옷 등록, 추천 생성, 착용 완료 흐름이 동작한다.
- 추천 생성 응답에 `top`, `bottom`, `outer`, score breakdown, 3개 이상 5개 이하의 reasons가 포함된다.
- `POST /api/recommendations?userId={userId}` 계약만 사용한다.

## 검증 명령
```bash
git diff --check
./gradlew test
./gradlew build
docker compose up --build
curl -s http://localhost:8080/v3/api-docs
```

## 리스크
- Spring Boot 신규 스캐폴드와 Docker Compose 설정이 한 번에 커지면 P0 공유 일정이 흔들릴 수 있다.
- 추천 규칙을 API 작업과 섞으면 테스트 가능한 도메인 경계가 흐려질 수 있다.
- Demo UI를 빨리 붙이면 P0 API 안정화가 늦어질 수 있다.

## 축소 또는 롤백 방안
- 일정 리스크가 커지면 Step 8 Demo UI를 제외한다.
- 추가 축소가 필요하면 Step 7 P1 API/CI를 제외한다.
- P0는 Swagger 기반 데모만 유지한다.
- Docker Compose, seed data, 옷 등록, 옷 목록 조회, 추천 생성, 착용 완료 처리는 유지한다.
- 추천 규칙, 점수 breakdown, 추천 이유, StaticWeatherProvider는 제거하지 않는다.

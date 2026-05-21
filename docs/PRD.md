# PRD: SmartCloset 1차 MVP

## 한 줄 정의
SmartCloset은 사용자의 옷장 데이터와 고정 테스트 날씨 정보를 기반으로 입을 수 있는 옷 후보를 좁히고, 색상 조합과 최근 이력을 점수화해 설명 가능한 코디를 추천하는 Spring Boot 4.0.6 백엔드 서비스다.

## 해결하려는 문제
사용자는 날씨에 맞는 옷을 고르는 데 매일 반복적인 판단 비용을 쓴다. 특히 "지금 입을 수 있는가", "색이 어울리는가", "최근에 너무 자주 입지 않았는가"를 동시에 고려해야 한다.

SmartCloset 1차 MVP는 AI 생성 추천이 아니라, 검증 가능한 규칙 기반 추천으로 이 판단을 빠르고 설명 가능하게 만든다.

## 핵심 사용자 시나리오
1. 사용자는 seed user 또는 테스트용 `userId`로 서비스를 사용한다.
2. 사용자는 옷을 등록하고 목록을 조회한다.
3. 서비스는 `StaticWeatherProvider`가 제공하는 고정 테스트 날씨를 내부 `WeatherCondition`으로 사용한다.
4. 서비스는 날씨상 입기 어려운 옷을 먼저 제외한다.
5. 남은 옷으로 TOP/BOTTOM 또는 TOP/BOTTOM/OUTER 조합을 만든다.
6. 각 조합은 날씨 적합도, 색상 조합, 최근 착용 이력, 최근 추천 이력, 다양성 보정으로 점수화된다.
7. 사용자는 생성된 오늘의 추천 결과에서 총점, 세부 점수, 추천 이유를 확인한다.
8. 사용자가 추천 결과를 착용 완료 처리하면 이후 추천에 이력이 반영된다.
9. 공유 대상자는 Docker Compose 실행 후 P0에서는 Swagger로 핵심 흐름을 확인하고, P1 Demo UI가 구현된 경우에는 최소 데모 UI로도 같은 흐름을 확인한다.

## 1차 MVP 우선순위

### P0: 공유 가능한 핵심 백엔드
- Docker Compose로 Spring Boot 4.0.6 + MySQL 실행
- seed user와 seed data 제공
- `StaticWeatherProvider` 기반 고정 날씨 제공
- 옷 등록 API
- 옷 목록 조회 API
- 오늘의 추천 생성 API
- 추천 결과 착용 완료 처리 API
- Swagger/OpenAPI에서 P0 API 호출 가능
- 추천 결과에 총점, 세부 점수, 추천 이유 포함
- README에 실행 방법과 테스트 시나리오 작성

### P1: 공유 품질 강화
- Spring Boot 정적 리소스 기반 최소 데모 UI
- GitHub Actions test/build
- 옷 상세 조회 API
- 옷 수정 API
- 옷 보관 처리 API

### P2: 1차 이후 또는 시간이 남을 경우
- 상세 조회/수정/보관 처리 UI
- UI 스타일 개선
- 외부 Weather API 연동
- AWS 수동 배포

## 1차 MVP 포함 범위
- Java 21 기반 Spring Boot 4.0.6 백엔드
- Spring Web 기반 REST API
- Spring Data JPA 기반 MySQL 저장
- Validation 기반 요청 검증
- JUnit 기반 추천 로직 테스트
- Docker Compose 기반 실행 환경
- Swagger/OpenAPI 기반 API 문서
- seed user와 seed data
- `StaticWeatherProvider` 기반 고정 테스트 날씨
- 내부 `WeatherCondition` 기반 추천 로직
- 규칙 기반 추천 점수 계산
- 추천 이유 제공
- 추천 결과 저장
- 추천 결과 착용 완료 처리
- README 실행 가이드와 공유용 테스트 시나리오

## 1차 MVP 제외 범위
- AI/GPT 추천
- 옷 이미지 업로드
- 이미지 자동 분석/태깅
- Redis 캐싱
- 캘린더 연동
- 쇼핑몰 추천
- 관리자 기능
- 소셜/공유 기능
- 회원가입/로그인
- 복잡한 권한 시스템
- 정식 프론트엔드 앱 구현
- React/Next/Vue 등 정식 프론트 기술 결정
- 외부 Weather API 실제 연동
- AWS 수동 배포
- CD 자동화

## 공유 방식
1차 MVP 공유 방식은 Docker Compose로 고정한다.

필수 제공 파일은 다음과 같다.
- `Dockerfile`
- `docker-compose.yml`
- `.env.example`
- `README.md`
- seed data

README에는 다음 정보를 포함한다.
- Docker Compose 실행 방법
- seed user 정보
- seed data 설명
- Swagger 접속 경로
- P1 Demo UI 구현 시 데모 UI 접속 경로
- 공유용 테스트 시나리오

AWS 수동 배포는 1차 MVP 범위에서 제외하고, 공유 이후 후보로 이동한다.

## Weather 정책
1차 MVP는 외부 Weather API를 연동하지 않는다.

`WeatherProvider` 인터페이스를 정의하고, 기본 구현은 `StaticWeatherProvider`로 고정한다. `StaticWeatherProvider`는 고정된 테스트 날씨 데이터를 반환한다.

추천 로직은 외부 API 응답이 아니라 내부 `WeatherCondition` 기준으로만 동작한다. 외부 Weather API 실제 연동은 1.5차 또는 2차 MVP 후보로 둔다.

## 사용자 모델
1차 MVP에서는 회원가입과 로그인을 구현하지 않는다.

API는 테스트용 `userId`를 request parameter로 전달받는다. seed data에는 기본 seed user를 포함한다.

## 옷 관리 범위
API 기준으로는 다음 기능을 제공한다.
- 옷 등록
- 옷 목록 조회
- 옷 상세 조회
- 옷 수정
- 옷 보관 처리

최소 데모 UI 기준으로는 다음 기능만 제공한다.
- 옷 등록
- 옷 목록 조회

옷 상세 조회, 수정, 보관 처리는 Swagger/API 호출로 검증한다.

## ClothingItem 최소 속성
`ClothingItem`은 1차 MVP에서 아래 속성을 가진다.

- `category`: `TOP` / `BOTTOM` / `OUTER`
- `color`
- `material`
- `minTemperature`
- `maxTemperature`
- `rainSuitable`
- `archived`

`material` enum은 1차 MVP에서 아래 값으로 제한한다.

- `COTTON`
- `DENIM`
- `KNIT`
- `WOOL`
- `POLYESTER`
- `NYLON`
- `UNKNOWN`

`material`은 필수 입력값으로 두되, 사용자가 확신하지 못하는 경우 `UNKNOWN`을 선택할 수 있다.

## 최소 데모 UI
정식 프론트엔드 앱은 1차 MVP에서 제외한다.

최소 데모 UI는 제품용 프론트가 아니라 API 흐름 공유용 단일 페이지다. Spring Boot 정적 리소스로 제공하며, React/Next/Vue 등 정식 프론트 기술 결정은 2차 MVP로 넘긴다.

최소 데모 UI 범위는 다음으로 제한한다.
- 옷 등록
- 옷 목록 조회
- 오늘의 추천 생성
- 추천 결과 착용 완료 처리

## 핵심 도메인
- `ClothingItem`: 사용자가 등록한 옷. 카테고리, 색상, 재질, 온도 적합 범위, 비 적합 여부, 보관 여부를 가진다.
- `WeatherCondition`: 추천 로직에서 사용하는 내부 날씨 상태.
- `WeatherProvider`: 현재 날씨를 내부 `WeatherCondition`으로 제공하는 인터페이스.
- `StaticWeatherProvider`: 고정 테스트 날씨를 `WeatherCondition`으로 제공하는 1차 MVP 기본 날씨 제공자.
- `OutfitCandidate`: TOP/BOTTOM 또는 TOP/BOTTOM/OUTER로 구성된 추천 후보 조합.
- `RecommendationScore`: 후보 조합의 총점과 세부 점수.
- `RecommendationReason`: 점수 근거를 사용자에게 설명하는 문장 목록.
- `RecommendationResult`: 저장된 추천 결과와 선택된 옷 조합, 점수, 이유, 착용 완료 여부.
- `WearHistory`: 사용자가 실제 착용 완료한 옷/코디 이력.
- `User`: 인증 없는 테스트용 사용자 식별자.

## 추천 점수 기준
추천 총점은 100점 기준이다.

- 날씨 적합도: 35점
- 색상 조합: 25점
- 최근 착용 이력: 20점
- 최근 추천 이력: 10점
- 다양성 보정: 10점

추천 응답에는 최종 코디, 총점, 세부 점수, 추천 이유 문장을 포함한다. 추천 이유는 AI 생성 문장이 아니라 점수 규칙의 결과를 사람이 읽기 쉬운 문장으로 변환한 것이다.

`material`은 독립 점수 항목으로 분리하지 않는다. `material`은 날씨 적합도 계산과 추천 이유 생성에 보조적으로 사용한다.

소재 기반 초기 규칙은 다음과 같다.
- 더운 날씨에 `WOOL` 또는 `KNIT` 소재는 감점 또는 제외한다.
- 추운 날씨에 `KNIT` 또는 `WOOL` 소재는 가산점으로 반영한다.
- 비 오는 날 `NYLON`은 가산점으로 반영한다.
- 비 오는 날 `WOOL`은 감점으로 반영한다.
- `UNKNOWN`은 소재 기반 가산/감점을 적용하지 않는다.

소재 기반 추천 이유 예시는 다음과 같다.
- "추운 날씨에 적합한 KNIT 소재가 날씨 점수에 긍정적으로 반영되었습니다."
- "비 오는 날 WOOL 소재는 젖었을 때 불편할 수 있어 날씨 점수가 낮아졌습니다."
- "NYLON 소재는 비 오는 날 착용에 유리해 날씨 점수에 긍정적으로 반영되었습니다."

## 추천 실패 케이스
추천 가능한 조합이 없으면 임의 추천을 만들지 않고 명시적 실패 코드를 반환한다.

- `NO_TOP_AVAILABLE`
- `NO_BOTTOM_AVAILABLE`
- `NO_WEATHER_SUITABLE_ITEM`
- `OUTER_REQUIRED_BUT_NOT_AVAILABLE`
- `INSUFFICIENT_CLOSET_ITEMS`

## 주요 API
- `POST /api/clothes?userId={userId}`: 옷 등록
- `GET /api/clothes?userId={userId}`: 옷 목록 조회
- `GET /api/clothes/{clothingId}?userId={userId}`: 옷 상세 조회
- `PUT /api/clothes/{clothingId}?userId={userId}`: 옷 수정
- `PATCH /api/clothes/{clothingId}/archive?userId={userId}`: 옷 보관 처리
- `POST /api/recommendations?userId={userId}`: 오늘의 추천 생성
- `PATCH /api/recommendations/{recommendationId}/worn?userId={userId}`: 추천 결과 착용 완료 처리

## 주요 기능별 완료 기준
- Docker Compose로 Spring Boot 4.0.6 앱과 MySQL이 실행된다.
- seed user와 seed data가 제공된다.
- `StaticWeatherProvider`가 고정 테스트 `WeatherCondition`을 제공한다.
- 사용자는 API로 옷을 등록하고 목록을 조회할 수 있다.
- 옷 등록 요청에서 `material`을 저장하고 조회 응답에 포함한다.
- 사용자는 API로 오늘의 추천을 생성할 수 있다.
- 추천 결과에는 총점, 세부 점수, 추천 이유가 포함된다.
- `material` 기반 규칙은 날씨 적합도 세부 이유에 포함될 수 있다.
- 사용자는 추천 결과를 착용 완료 처리할 수 있다.
- Swagger/OpenAPI에서 P0 API를 호출할 수 있다.
- README만 보고 Docker Compose 실행과 테스트 시나리오 재현이 가능하다.
- P1 완료 시 최소 데모 UI에서 핵심 추천 흐름을 확인할 수 있다.

## 테스트/검증 기준
- 추천 스코어링은 순수 도메인 서비스로 분리하고 JUnit 단위 테스트를 작성한다.
- 날씨 필터링, 색상 점수, 최근 착용 패널티, 최근 추천 패널티, 다양성 보정은 각각 독립 테스트를 가진다.
- 추천 실패 코드 5종을 테스트한다.
- 추천 이유는 점수 근거와 문장이 어긋나지 않는지 테스트한다.
- `StaticWeatherProvider` 기준 seed data 추천 흐름이 재현되어야 한다.
- 옷 등록 요청에서 `material`을 저장하고 조회 응답에 포함되는지 검증한다.
- `material=UNKNOWN`일 때 소재 기반 가산/감점이 적용되지 않는지 검증한다.
- 더운 날 `WOOL`/`KNIT`, 추운 날 `WOOL`/`KNIT`, 비 오는 날 `NYLON`/`WOOL` 규칙을 weatherScore 테스트에 포함한다.
- 추천 이유에 소재 기반 weatherScore 근거가 포함되는 케이스를 테스트한다.
- 기존 총점 배점 100점 구조가 변경되지 않았는지 검증한다.
- Swagger에서 P0 API를 호출할 수 있어야 한다.
- Docker Compose 실행 후 README의 공유용 테스트 시나리오가 성공해야 한다.
- P1 완료 시 GitHub Actions에서 test/build가 실행된다.

## 향후 MVP 후보
- 1.5차 MVP: 외부 Weather API 실제 연동
- 2차 MVP: 정식 프론트엔드 앱, React/Next/Vue 등 기술 결정, 사용자 UX 검증
- 3차 MVP: 옷 이미지 업로드, S3 연동, 이미지 기반 수동 태깅 보조
- 4차 MVP: 개인화 추천 고도화, 계절/선호도/스타일 태그, 추천 피드백 반영
- 5차 MVP: AI/GPT 설명 보조, 캘린더 연동, 쇼핑몰 추천, Redis 캐싱, 관리자 기능, AWS 배포, CD 자동화

## 결정된 사항
- Lombok 사용 정책은 `docs/adr/003-mvp-scope-decisions.md`를 따른다.
- 색상 규칙의 세부 매핑은 `docs/RECOMMENDATION_RULES.md`를 따른다.
- OUTER 필수 조건은 `temperature <= 12` 기준이며 `StaticWeatherProvider`의 기본 `temperature=12`에서 OUTER 필수 흐름을 검증한다.

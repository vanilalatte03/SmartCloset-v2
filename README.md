# SmartCloset

SmartCloset은 사용자의 옷장 데이터와 고정 테스트 날씨 정보를 기반으로, 날씨에 맞는 옷 후보를 필터링하고 색상 조합과 최근 이력을 점수화해 설명 가능한 코디를 추천하는 Spring Boot 4.0.6 백엔드 서비스입니다.

1차 MVP는 AI/GPT 추천이 아니라 규칙 기반 추천을 구현합니다. 옷 이미지 업로드, 외부 Weather API 실제 연동, AWS 배포, 정식 프론트엔드 앱은 1차 MVP 범위가 아닙니다.

## 기술 스택
- Java 21
- Spring Boot 4.0.6
- Spring Web
- Spring Data JPA
- MySQL
- Validation
- Swagger/OpenAPI
- Docker Compose
- JUnit
- GitHub Actions

## 1차 MVP 핵심 기능

### P0
- 옷 등록
- 옷 목록 조회
- `StaticWeatherProvider` 기반 고정 날씨 제공
- 추천 생성
- 추천 결과 착용 완료 처리
- Swagger/OpenAPI API 호출
- Docker Compose 실행

### P1
- Spring Boot static resource 기반 최소 데모 UI
- 옷 상세 조회/수정/보관 처리 API
- GitHub Actions test/build

## 실행 전 요구사항
- Docker
- Docker Compose
- Git
- Java 21: 로컬에서 직접 Gradle 명령을 실행할 때 필요

## 개발 전 준비
Harness 운영 스크립트 테스트와 Git pre-commit hook을 사용하려면 아래 명령을 먼저 실행합니다.

```bash
python3 -m pip install -r requirements-dev.txt
git config core.hooksPath .githooks
```

## 환경 변수 기준
실제 `.env.example` 파일은 구현 단계에서 작성합니다. 1차 MVP 로컬 공유용 기본값은 아래 기준을 따릅니다.

```env
MYSQL_DATABASE=smartcloset
MYSQL_USER=smartcloset
MYSQL_PASSWORD=smartcloset
MYSQL_ROOT_PASSWORD=root
SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/smartcloset
SPRING_DATASOURCE_USERNAME=smartcloset
SPRING_DATASOURCE_PASSWORD=smartcloset
SPRING_PROFILES_ACTIVE=local
```

위 값은 실제 운영 비밀번호가 아니라 Docker Compose 로컬 공유용 값입니다. 민감정보를 코드와 문서에 커밋하지 않습니다.

## Docker Compose 실행

```bash
docker compose up --build
```

중지:

```bash
docker compose down
```

DB까지 초기화:

```bash
docker compose down -v
```

## 접속 경로
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Demo UI(P1 구현 후): http://localhost:8080/demo/index.html
- Health Check 후보: http://localhost:8080/actuator/health

Health Check는 Actuator 사용 여부 또는 별도 endpoint 제공 여부를 구현 단계에서 확정합니다.

## StaticWeatherProvider 기준
1차 MVP는 외부 Weather API를 사용하지 않습니다. 추천 로직은 아래 고정 테스트 날씨를 사용합니다.

| Field | Value |
| --- | --- |
| `temperature` | `12` |
| `weatherType` | `CLOUDY` |
| `rainy` | `false` |
| `windy` | `false` |

`temperature=12`이므로 OUTER 필수 조합이 생성되어야 합니다.

## Seed Data 기준
구현 완료 후 seed data는 아래 조건을 만족해야 합니다.

- seed user 1명 제공
- 기본 사용자: `userId=1`, `name=demo-user`
- 추천이 가능하도록 TOP, BOTTOM, OUTER가 최소 1개 이상 포함
- `StaticWeatherProvider`의 `temperature=12` 조건에서 OUTER 필수 추천이 가능
- 색상, 재질, 온도 범위가 다양한 옷 포함

Seed data 예시:

| Type | Name | category | color | material | minTemperature | maxTemperature | rainSuitable |
| --- | --- | --- | --- | --- | ---: | ---: | --- |
| User | demo-user | - | - | - | - | - | - |
| TOP | 아이보리 니트 | `TOP` | `WHITE` | `KNIT` | `0` | `16` | `false` |
| BOTTOM | 블랙 데님 | `BOTTOM` | `BLACK` | `DENIM` | `0` | `22` | `false` |
| OUTER | 네이비 코트 | `OUTER` | `NAVY` | `WOOL` | `-10` | `12` | `false` |
| OUTER | 블랙 나일론 자켓 | `OUTER` | `BLACK` | `NYLON` | `5` | `18` | `true` |

## 빠른 테스트 흐름
Swagger UI에서 P0 흐름을 아래 순서로 확인합니다.

1. 옷 목록 조회: `GET /api/clothes?userId=1`
2. 옷 등록: `POST /api/clothes?userId=1`
3. 추천 생성: `POST /api/recommendations?userId=1`
4. 추천 결과의 총점, 세부 점수, 추천 이유 확인
5. 추천 결과 착용 완료 처리: `PATCH /api/recommendations/{recommendationId}/worn?userId=1`
6. 추천 재생성 후 최근 착용 이력 반영 확인

## 테스트 명령어
Step 1 전에는 Harness 운영 스크립트 검증을 먼저 사용합니다.

```bash
python3 -m compileall scripts
python3 -m pytest scripts/test_checks.py scripts/test_guard.py scripts/test_execute.py scripts/test_autopilot.py
```

Gradle wrapper가 생성된 뒤에는 아래 명령을 사용합니다.

```bash
./gradlew test
./gradlew build
```

Docker 환경에서 실행할지, 로컬 Gradle로 실행할지는 구현 단계에서 확정합니다.

## 문서
- [PRD](docs/PRD.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Recommendation Rules](docs/RECOMMENDATION_RULES.md)
- [API](docs/API.md)
- [ERD](docs/ERD.md)
- [Demo Scenario](docs/DEMO_SCENARIO.md)
- [Sharing Guide](docs/SHARING_GUIDE.md)
- [ADR](docs/adr/)

## 문서 동기화 체크리스트
- 예전 GET 기반 today 추천 경로 표현이 남아 있으면 `POST /api/recommendations?userId={userId}`로 수정한다.
- `StaticWeatherProvider` 기본 날씨가 모든 문서에서 `temperature=12`, `weatherType=CLOUDY`, `rainy=false`, `windy=false`인지 확인한다.
- Docker Compose가 유일한 필수 공유 방식인지 확인한다.
- AWS 수동 배포가 1차 MVP 필수처럼 보이지 않는지 확인한다.
- 외부 Weather API가 1차 MVP 필수처럼 보이지 않는지 확인한다.
- Demo UI가 정식 프론트엔드 앱처럼 보이지 않는지 확인한다.

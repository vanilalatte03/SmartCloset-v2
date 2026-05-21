# SmartCloset

SmartCloset은 사용자의 옷장 데이터와 날씨 정보를 기반으로, 입을 수 있는 옷 후보를 필터링하고 색상 조합과 최근 이력을 점수화해 설명 가능한 코디를 추천하는 Spring Boot 4.0.6 백엔드 서비스입니다.

1.5차 MVP는 1차 규칙 기반 추천 API 위에 기상청 단기예보 조회서비스 `getVilageFcst` JSON 연동을 추가합니다. 서비스키가 없거나 외부 API 호출에 실패하면 `StaticWeatherProvider` fallback 날씨로 추천 흐름을 유지합니다.

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

## 1.5차 MVP 핵심 기능

### 유지 기능
- 옷 등록/목록/상세/수정/보관 API
- 규칙 기반 추천 생성
- 추천 결과 착용 완료 처리
- Swagger/OpenAPI API 호출
- Spring Boot static resource 기반 Demo UI
- Docker Compose 실행

### 1.5차 추가 기준
- 기상청 단기예보 조회서비스 `getVilageFcst` JSON 연동
- KMA `TMP`, `SKY`, `PTY`, `PCP`, `WSD`를 내부 `WeatherCondition`으로 매핑
- 서비스키 미설정, KMA 오류, `NODATA`, 필수 category 누락 시 fallback
- 환경변수 기반 기본 격자 설정
- 추천 API 계약 유지: `POST /api/recommendations?userId={userId}`

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
로컬 공유용 기본값은 [.env.example](.env.example)를 기준으로 합니다.

```env
MYSQL_DATABASE=smartcloset
MYSQL_USER=smartcloset
MYSQL_PASSWORD=smartcloset
MYSQL_ROOT_PASSWORD=root
MYSQL_PORT=3307

SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/smartcloset?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
SPRING_DATASOURCE_USERNAME=smartcloset
SPRING_DATASOURCE_PASSWORD=smartcloset
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_PROFILES_ACTIVE=local

KMA_SERVICE_KEY=
KMA_NX=60
KMA_NY=127
KMA_BASE_URL=http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0
WEATHER_FALLBACK_ENABLED=true

APP_PORT=8080
```

위 값은 Docker Compose 로컬 공유용 기본값입니다. `KMA_SERVICE_KEY`에는 공공데이터포털에서 발급받은 실제 인증키를 로컬 `.env`에만 설정하고, 코드와 문서에 커밋하지 않습니다.

서비스키가 비어 있어도 fallback이 활성화되어 있으면 추천 생성은 성공해야 합니다.

## Docker Compose 실행

```bash
cp .env.example .env
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

MySQL 컨테이너 내부 포트는 `3306`이고, 호스트 공개 포트 기본값은 충돌을 줄이기 위해 `3307`입니다. 필요하면 `.env`의 `MYSQL_PORT`를 조정합니다.

## 접속 경로
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Demo UI: http://localhost:8080/demo/index.html

## Weather 기준
1.5차 기본 weather source는 기상청 단기예보 JSON입니다.

```text
GET {KMA_BASE_URL}/getVilageFcst
```

요청 parameter는 `serviceKey`, `pageNo=1`, `numOfRows=1000`, `dataType=JSON`, `base_date`, `base_time`, `nx`, `ny`입니다.

기본 위치는 첨부 격자 위경도 XLSX 기준 서울특별시 대표 격자입니다.

| Location | nx | ny |
| --- | ---: | ---: |
| 서울특별시 | `60` | `127` |

KMA category 매핑:

| Category | WeatherCondition |
| --- | --- |
| `TMP` | `temperature` |
| `PTY` | `weatherType`, `rainy` |
| `SKY` | `weatherType` |
| `PCP` | `rainy` |
| `WSD` | `windy` |

Fallback 날씨:

| Field | Value |
| --- | --- |
| `temperature` | `12` |
| `weatherType` | `CLOUDY` |
| `rainy` | `false` |
| `windy` | `false` |

## Seed Data 기준
애플리케이션 시작 시 seed initializer가 아래 조건을 만족하는 데이터를 생성합니다.

- seed user 1명 제공
- 기본 사용자: `userId=1`, `name=demo-user`
- 추천이 가능하도록 TOP, BOTTOM, OUTER가 최소 1개 이상 포함
- fallback `temperature=12` 조건에서 OUTER 필수 추천이 가능
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
Swagger UI에서 아래 순서로 확인합니다.

1. 옷 목록 조회: `GET /api/clothes?userId=1`
2. 옷 등록: `POST /api/clothes?userId=1`
3. 추천 생성: `POST /api/recommendations?userId=1`
4. 추천 결과의 weather, 총점, 세부 점수, 추천 이유 확인
5. 추천 결과 착용 완료 처리: `PATCH /api/recommendations/{recommendationId}/worn?userId=1`
6. 추천 재생성 후 최근 착용 이력 반영 확인

OpenAPI JSON 확인:

```bash
curl -s http://localhost:8080/v3/api-docs
```

Demo UI 확인:

```text
http://localhost:8080/demo/index.html
```

Demo UI에서는 `userId=1` 기준으로 옷 목록 조회, 옷 등록, 추천 생성, 착용 완료 처리를 실행할 수 있습니다.

## KMA 연동 확인
실제 기상청 API 연동을 확인하려면 `.env`에 `KMA_SERVICE_KEY`를 설정합니다.

```bash
cp .env.example .env
# .env에서 KMA_SERVICE_KEY만 로컬 값으로 채움
docker compose up --build
```

그 다음 Swagger 또는 Demo UI에서 추천 생성을 실행하고 응답 또는 화면의 `weather`를 확인합니다. 공개 API 계약은 `POST /api/recommendations?userId=1`이며, Demo UI도 이 백엔드 API만 호출합니다.

주의:
- 실제 서비스키는 커밋하지 않습니다.
- KMA 호출이 실패하거나 데이터가 없으면 fallback이 활성화된 상태에서 추천이 계속 성공할 수 있습니다.

## 테스트 명령어
Harness 운영 스크립트 검증:

```bash
python3 -m compileall scripts
python3 -m pytest scripts/test_checks.py scripts/test_guard.py scripts/test_execute.py scripts/test_autopilot.py
```

Gradle 검증:

```bash
./gradlew test
./gradlew build
```

Docker Compose 공유 검증:

```bash
cp .env.example .env
docker compose up --build
curl -s http://localhost:8080/v3/api-docs
docker compose down -v
```

## 문서
- [PRD](docs/PRD.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Recommendation Rules](docs/RECOMMENDATION_RULES.md)
- [API](docs/API.md)
- [ERD](docs/ERD.md)
- [Demo Scenario](docs/DEMO_SCENARIO.md)
- [Sharing Guide](docs/SHARING_GUIDE.md)
- [ADR](docs/adr/)
- [1차 MVP Archive](archive/mvp-1/README.md)

## 문서 동기화 체크리스트
- today 추천 GET 경로가 API 계약처럼 보이지 않는지 확인한다.
- 추천 생성 API가 `POST /api/recommendations?userId={userId}`인지 확인한다.
- KMA 기본 격자가 `KMA_NX=60`, `KMA_NY=127`인지 확인한다.
- fallback 값이 `temperature=12`, `weatherType=CLOUDY`, `rainy=false`, `windy=false`인지 확인한다.
- Docker Compose가 필수 공유 방식인지 확인한다.
- 실제 API key가 문서나 코드에 들어가지 않았는지 확인한다.
- AWS 수동 배포가 1.5차 MVP 필수처럼 보이지 않는지 확인한다.
- Demo UI가 정식 프론트엔드 앱처럼 보이지 않는지 확인한다.

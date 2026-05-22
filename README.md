# SmartCloset

SmartCloset은 사용자의 옷장 데이터와 사용자별 위치 날씨를 기반으로, 입을 수 있는 옷 후보를 필터링하고 색상 조합과 최근 이력을 점수화해 설명 가능한 코디를 추천하는 서비스입니다.

2차 MVP는 1.5차에서 구현한 기상청 단기예보 `getVilageFcst` JSON 연동 위에 사용자별 위치 저장, 내장 대표 격자 위치 선택 API, React+Vite+TypeScript 프론트엔드 앱을 추가합니다.

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
- React
- Vite
- TypeScript

## 2차 MVP 핵심 기능

### 유지 기능
- 옷 등록/목록/상세/수정/보관 API
- 기상청 단기예보 `getVilageFcst` JSON 기반 weather provider
- `StaticWeatherProvider` fallback
- 규칙 기반 추천 생성
- 추천 결과 착용 완료 처리
- Swagger/OpenAPI API 호출
- Docker Compose 실행

### 2차 추가 기준
- 사용자별 위치 저장
- 내장 대표 격자 catalog 조회와 검색
- 위치 선택 API
- 추천 생성 시 사용자 위치 `nx`, `ny`로 KMA 호출
- React+Vite+TypeScript 프론트엔드 앱
- typed API client와 명시적 DTO 타입

## 실행 전 요구사항
- Docker
- Docker Compose
- Git
- Java 21: 로컬에서 직접 Gradle 명령을 실행할 때 필요
- Node.js와 npm: 프론트엔드를 로컬에서 직접 실행할 때 필요

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
FRONTEND_PORT=5173
VITE_API_BASE_URL=http://localhost:8080
```

`KMA_SERVICE_KEY`에는 공공데이터포털에서 발급받은 실제 인증키를 로컬 `.env`에만 설정하고, 코드와 문서에 커밋하지 않습니다. `KMA_NX`, `KMA_NY`는 기존 구현과 마이그레이션 호환을 위한 기본값이며, 2차 추천 기준은 사용자별 위치입니다.

서비스키가 비어 있어도 fallback이 활성화되어 있으면 추천 생성은 성공해야 합니다.

프론트는 `FRONTEND_PORT=5173`, `VITE_API_BASE_URL=http://localhost:8080` 기본값으로 실행됩니다.

## Docker Compose 실행

Docker Compose는 `mysql`, `app`, `frontend` 세 서비스를 함께 실행합니다. 기존 로컬 `.env`가 있으면 덮어쓰지 않습니다.

```bash
test -f .env || cp .env.example .env
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
- Frontend: http://localhost:5173
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- 보조 Demo UI: http://localhost:8080/demo/index.html

2차의 주 사용 화면은 React 프론트엔드입니다. Swagger 또는 보조 Demo UI는 API 흐름을 분리해 확인할 때 사용합니다.

## 사용자 위치 기준
2차는 외부 지도/주소 API 없이 서버 내장 대표 격자 catalog를 사용합니다.

seed user 기본 위치:

| userId | Location | code | nx | ny |
| ---: | --- | --- | ---: | ---: |
| 1 | 서울특별시 | `SEOUL` | 60 | 127 |

최소 catalog:

| Code | Name | nx | ny |
| --- | --- | ---: | ---: |
| `SEOUL` | 서울특별시 | 60 | 127 |
| `BUSAN` | 부산광역시 | 98 | 76 |
| `DAEGU` | 대구광역시 | 89 | 90 |
| `INCHEON` | 인천광역시 | 55 | 124 |
| `GWANGJU` | 광주광역시 | 58 | 74 |
| `DAEJEON` | 대전광역시 | 67 | 100 |
| `ULSAN` | 울산광역시 | 102 | 84 |
| `SEJONG` | 세종특별자치시 | 66 | 103 |
| `JEJU` | 제주특별자치도 | 52 | 38 |

## Weather 기준
기본 weather source는 기상청 단기예보 JSON입니다.

```text
GET {KMA_BASE_URL}/getVilageFcst
```

요청 parameter는 `serviceKey`, `pageNo=1`, `numOfRows=1000`, `dataType=JSON`, `base_date`, `base_time`, 사용자 위치 `nx`, 사용자 위치 `ny`입니다.

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

## 빠른 테스트 흐름
React 앱(`http://localhost:5173`)에서 아래 순서로 확인합니다. API만 분리해 확인해야 할 때는 Swagger UI에서 같은 경로를 호출합니다.

1. 사용자 위치 조회: `GET /api/users/location?userId=1`
2. 위치 catalog 검색: `GET /api/locations?keyword=부산`
3. 사용자 위치 선택: `PUT /api/users/location?userId=1`
4. 옷 목록 조회: `GET /api/clothes?userId=1`
5. 옷 등록: `POST /api/clothes?userId=1`
6. 추천 생성: `POST /api/recommendations?userId=1`
7. 추천 결과의 weather, 총점, 세부 점수, 추천 이유 확인
8. 추천 결과 착용 완료 처리: `PATCH /api/recommendations/{recommendationId}/worn?userId=1`
9. 추천 재생성 후 최근 착용 이력 반영 확인

today 추천 GET 경로는 사용하지 않습니다.

## 프론트엔드 개발
프론트 기준 문서는 [docs/FRONTEND.md](docs/FRONTEND.md)를 따릅니다. `frontend/`는 React+Vite+TypeScript SPA이며 Docker Compose `frontend` 서비스로 함께 실행됩니다.

```bash
cd frontend
npm run dev
npm run build
```

2차 프론트엔드는 TypeScript `strict` 기준을 사용하고, API 요청/응답 DTO를 명시적 타입으로 관리합니다.

## KMA 연동 확인
실제 기상청 API 연동을 확인하려면 `.env`에 `KMA_SERVICE_KEY`를 설정합니다.

```bash
cp .env.example .env
# .env에서 KMA_SERVICE_KEY만 로컬 값으로 채움
docker compose up --build
```

그 다음 React 앱에서 위치를 선택하고 추천 생성을 실행합니다. 공개 API 계약은 `POST /api/recommendations?userId=1`입니다.

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

```bash
cd frontend
npm run build
```

Docker Compose 공유 검증:

```bash
test -f .env || cp .env.example .env
docker compose up --build
curl -s http://localhost:8080/v3/api-docs
docker compose down -v
```

## 문서
- [PRD](docs/PRD.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Frontend](docs/FRONTEND.md)
- [API](docs/API.md)
- [ERD](docs/ERD.md)
- [Recommendation Rules](docs/RECOMMENDATION_RULES.md)
- [Demo Scenario](docs/DEMO_SCENARIO.md)
- [Sharing Guide](docs/SHARING_GUIDE.md)
- [ADR](docs/ADR.md)

과거 MVP archive는 참고용이며 현재 구현 기준은 아닙니다.

- [1차 MVP Archive](archive/mvp-1/README.md)
- [1.5차 MVP Archive](archive/mvp-1-5/README.md)

## 문서 동기화 체크리스트
- today 추천 GET 경로가 API 계약처럼 보이지 않는지 확인한다.
- 추천 생성 API가 `POST /api/recommendations?userId={userId}`인지 확인한다.
- seed user 기본 위치가 서울특별시 `SEOUL`, `60`, `127`인지 확인한다.
- fallback 값이 `temperature=12`, `weatherType=CLOUDY`, `rainy=false`, `windy=false`인지 확인한다.
- Docker Compose가 필수 공유 방식인지 확인한다.
- React+Vite+TypeScript 앱 기준이 `docs/FRONTEND.md`와 일치하는지 확인한다.
- 실제 API key가 문서나 코드에 들어가지 않았는지 확인한다.
- AWS 배포가 2차 MVP 필수처럼 보이지 않는지 확인한다.

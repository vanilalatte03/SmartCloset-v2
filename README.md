# SmartCloset

SmartCloset은 사용자의 옷장 데이터와 사용자별 위치 날씨를 기반으로, 입을 수 있는 옷 후보를 필터링하고 색상 조합, 최근 이력, 선호 색상/소재를 점수화해 설명 가능한 코디를 추천하는 서비스입니다.

3차 MVP는 테스트용 `userId=1` 흐름을 벗어나 Spring Security와 JWT Bearer token 기반 인증 사용자 서비스로 전환합니다. 사용자는 회원가입/로그인 후 자신의 옷장, 위치, 선호도, 추천 이력, 착용 완료 이력을 관리합니다.

## 기술 스택
- Java 21
- Spring Boot 4.0.6
- Spring Web
- Spring Security
- Spring Data JPA
- MySQL
- Validation
- Swagger/OpenAPI
- Docker Compose
- JUnit
- React
- Vite
- TypeScript

## 3차 MVP 핵심 기능

### 유지 기능
- 기상청 단기예보 `getVilageFcst` JSON 기반 weather provider
- `StaticWeatherProvider` fallback
- 사용자별 위치 저장
- 내장 대표 격자 catalog 조회와 검색
- 규칙 기반 추천 생성
- 추천 결과 착용 완료 처리
- React+Vite+TypeScript 프론트엔드 앱
- Docker Compose 실행

### 3차 추가 기준
- 회원가입/로그인
- Spring Security + JWT Bearer access token
- 공개 API와 보호 API 분리
- 공개 HTTP API에서 `?userId=` query parameter 제거
- 현재 사용자 전용 response DTO에서 `userId` 필드 제거
- 사용자별 옷장, 위치, 추천 이력, 착용 이력 분리
- 사용자 선호도 저장/조회
- 기존 다양성 점수를 `preferenceScore`로 교체
- 추천 이력 조회 API
- 프론트 access token `sessionStorage` 저장

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

## MVP 3 전환 시 DB 초기화
MVP 3 전환 시 로컬 Docker Compose DB는 기존 2차 schema/seed data와 충돌할 수 있으므로 초기화를 권장합니다.

```bash
docker compose down -v
docker compose up --build
```

운영 DB migration은 3차 문서 범위에서 다루지 않습니다. 로컬 공유/데모 기준은 volume 초기화로 정리합니다.

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

JWT_SECRET=change-me-local-development-only

KMA_SERVICE_KEY=
KMA_NX=60
KMA_NY=127
KMA_BASE_URL=http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0
WEATHER_FALLBACK_ENABLED=true

APP_PORT=8080
FRONTEND_PORT=5173
VITE_API_BASE_URL=http://localhost:8080
```

`JWT_SECRET` 예시는 로컬 개발용 placeholder입니다. 운영 secret, 실제 API key, token, password는 코드와 문서에 커밋하지 않습니다.

`KMA_SERVICE_KEY`에는 공공데이터포털에서 발급받은 실제 인증키를 로컬 `.env`에만 설정하고, 코드와 문서에 커밋하지 않습니다. `KMA_NX`, `KMA_NY`는 기존 구현과 마이그레이션 호환을 위한 기본값이며, 3차 추천 기준은 인증 사용자별 위치입니다.

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

3차의 주 사용 화면은 React 프론트엔드입니다. Swagger 또는 보조 Demo UI는 API 흐름을 분리해 확인할 때 사용합니다.

## 인증 흐름
공개 API:

- `POST /api/auth/signup`
- `POST /api/auth/login`

보호 API는 `Authorization: Bearer {accessToken}` header가 필요합니다. 프론트는 access token을 `sessionStorage`에 저장합니다. 새로고침 시 `GET /api/users/me`로 로그인 상태를 복구하고, 로그아웃 시 token과 사용자 상태를 제거합니다. JWT access token은 `HS256` + `JWT_SECRET`으로 서명하고 만료 시간은 2시간으로 고정합니다.

## 사용자 위치 기준
3차 위치 선택은 외부 지도/주소 API 없이 서버 내장 대표 격자 catalog를 사용합니다. `GET /api/locations`는 보호 API이며 로그인 후 위치 선택 화면에서만 호출합니다.

신규 사용자 기본 위치:

| Location | code | nx | ny |
| --- | --- | ---: | ---: |
| 서울특별시 | `SEOUL` | 60 | 127 |

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

## 사용자 선호도 기준
선호도는 `users` 테이블의 JSON 문자열 컬럼에 저장합니다.

- `preferred_colors_json`
- `preferred_materials_json`
- `style_tags_json`

API 계약에서는 배열로 주고받습니다.

```json
{
  "preferredColors": [],
  "preferredMaterials": [],
  "styleTags": []
}
```

`preferredColors`와 `preferredMaterials`는 `preferenceScore`에 반영합니다. `styleTags`는 저장/조회/표시만 하며 추천 점수와 추천 이유에는 반영하지 않습니다.

## 추천 점수 기준
추천 총점은 100점입니다.

| Score | Max |
| --- | ---: |
| `weatherScore` | 35 |
| `colorScore` | 25 |
| `wearHistoryScore` | 20 |
| `recommendationHistoryScore` | 10 |
| `preferenceScore` | 10 |

`preferenceScore` 계산:

- 선호 색상/소재가 모두 비어 있으면 0점
- 추천 후보 옷 중 `preferredColors`와 일치하는 색상이 하나 이상 있으면 5점
- 추천 후보 옷 중 `preferredMaterials`와 일치하는 소재가 하나 이상 있으면 5점

## Weather 기준
기본 weather source는 기상청 단기예보 JSON입니다.

```text
GET {KMA_BASE_URL}/getVilageFcst
```

요청 parameter는 `serviceKey`, `pageNo=1`, `numOfRows=1000`, `dataType=JSON`, `base_date`, `base_time`, 현재 인증 사용자 위치 `nx`, 현재 인증 사용자 위치 `ny`입니다.

Fallback 날씨:

| Field | Value |
| --- | --- |
| `temperature` | `12` |
| `weatherType` | `CLOUDY` |
| `rainy` | `false` |
| `windy` | `false` |

## 빠른 테스트 흐름
React 앱(`http://localhost:5173`)에서 아래 순서로 확인합니다. API만 분리해 확인해야 할 때는 Swagger UI에서 같은 경로를 호출합니다.

1. 회원가입: `POST /api/auth/signup`
2. 로그인: `POST /api/auth/login`
3. 현재 사용자 조회: `GET /api/users/me`
4. 사용자 위치 조회: `GET /api/users/me/location`
5. 위치 catalog 검색: `GET /api/locations?keyword=부산`
6. 사용자 위치 선택: `PUT /api/users/me/location`
7. 선호도 조회/저장: `GET /api/users/me/preferences`, `PUT /api/users/me/preferences`
8. 옷 목록 조회: `GET /api/clothes`
9. 옷 등록: `POST /api/clothes`
10. 추천 생성: `POST /api/recommendations`
11. 추천 이력 조회: `GET /api/recommendations?limit=20`
12. 추천 결과 착용 완료 처리: `PATCH /api/recommendations/{recommendationId}/worn`

today 추천 GET 경로는 사용하지 않습니다.

## 프론트엔드 개발
프론트 기준 문서는 [docs/FRONTEND.md](docs/FRONTEND.md)를 따릅니다. `frontend/`는 React+Vite+TypeScript SPA이며 Docker Compose `frontend` 서비스로 함께 실행됩니다.

```bash
cd frontend
npm run dev
npm run build
```

3차 프론트엔드는 TypeScript `strict` 기준을 사용하고, API 요청/응답 DTO를 명시적 타입으로 관리합니다. 보호 API 호출에는 Bearer token을 붙이며, token 저장 위치는 `sessionStorage`입니다.

## KMA 연동 확인
실제 기상청 API 연동을 확인하려면 `.env`에 `KMA_SERVICE_KEY`를 설정합니다.

```bash
cp .env.example .env
# .env에서 KMA_SERVICE_KEY만 로컬 값으로 채움
docker compose up --build
```

그 다음 React 앱에서 로그인하고 위치를 선택한 뒤 추천 생성을 실행합니다. 공개 추천 생성 API 계약은 `POST /api/recommendations`입니다.

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
docker compose down -v
test -f .env || cp .env.example .env
docker compose up --build
curl -s http://localhost:8080/v3/api-docs
docker compose down
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
- [2차 MVP Archive](archive/mvp-2/README.md)

## 문서 동기화 체크리스트
- 공개 API와 보호 API 표가 분리되어 있는지 확인한다.
- `GET /api/locations`가 보호 API와 로그인 후 위치 선택 흐름에만 등장하는지 확인한다.
- 공개 API 계약에서 `?userId=`가 제거됐는지 확인한다.
- 현재 사용자 전용 응답 예시에서 `userId` 필드가 제거됐는지 확인한다.
- 기존 다양성 점수 표현이 활성 문서에서 `preferenceScore`로 교체됐는지 확인한다.
- `styleTags`가 추천 점수/추천 이유에 반영된다는 표현이 없는지 확인한다.
- 추천 생성 API가 `POST /api/recommendations`인지 확인한다.
- today 추천 GET 경로가 API 계약처럼 보이지 않는지 확인한다.
- Docker Compose DB 초기화 권장 명령이 반영됐는지 확인한다.
- 실제 API key, token, password, private key가 문서나 코드에 들어가지 않았는지 확인한다.

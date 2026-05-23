# Sharing Guide: SmartCloset MVP4

## 공유 방식
SmartCloset MVP4 공유 방식은 Docker Compose로 유지한다.

공유 대상자는 Docker Compose로 MySQL, Spring Boot 4.0.6 백엔드, React+Vite+TypeScript 프론트엔드를 함께 실행한다. AWS 배포, native mobile app, PWA 배포는 MVP4 공유 범위가 아니다.

기상청 API key가 없어도 앱은 실행되어야 한다. 이 경우 추천은 `StaticWeatherProvider` fallback 날씨로 생성된다. 실제 기상청 단기예보 JSON 연동을 확인하려면 `.env`에 `KMA_SERVICE_KEY`를 설정한다.

## P0 Release Cut 공유 기준
Step 7 기준 공유 후보는 P0 release cut이다. Docker Compose로 `mysql`, `app`, `frontend`가 함께 실행되고, 신규 사용자가 로그인 후 Today 화면에서 위치/날씨 요약, 첫 추천 체크리스트, 옷장 빠른 등록, 추천 생성, 실패 CTA, 이유 우선 추천 결과, 착용 완료, 최근 추천 preview를 확인할 수 있어야 한다.

Step 8, 9, 10은 P1 polish tail이다. 선호도 화면 저장 상태 문구, 위치 catalog 선택 polish, 전용 History view의 모바일 이력 카드와 착용 완료 polish는 후속 P1 기준이며 P0 공유 성공 여부를 막지 않는다.

## MVP4 데모 전 DB 초기화
로컬 Docker Compose DB는 기존 schema/seed data와 충돌할 수 있으므로 MVP4 데모 전 초기화를 권장한다.

```bash
docker compose down -v
docker compose up --build
```

운영 DB migration은 MVP4 문서 범위에서 다루지 않는다. 로컬 공유/데모 기준은 volume 초기화로 정리한다.

## 전달해야 할 파일/경로
공유 시 아래 항목을 포함해야 한다.

- GitHub repository URL
- `README.md`
- `Dockerfile`
- `docker-compose.yml`
- `.env.example`
- `frontend/`: React+Vite+TypeScript SPA
- seed data 또는 데모 계정 생성 안내
- Frontend 경로: http://localhost:5173
- Swagger UI 경로: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON 경로: http://localhost:8080/v3/api-docs

## 실행 명령

```bash
git clone <repository-url>
cd SmartCloset-v2
test -f .env || cp .env.example .env
docker compose down -v
docker compose up --build
```

Frontend 접속:

```text
http://localhost:5173
```

Swagger UI 접속:

```text
http://localhost:8080/swagger-ui/index.html
```

중지:

```bash
docker compose down
```

DB까지 초기화:

```bash
docker compose down -v
```

## 환경변수
`.env.example`은 실제 비밀값을 포함하지 않는다. 실제 서비스키와 운영 JWT secret은 로컬 `.env` 또는 배포 환경에만 넣는다.

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

| Variable | Description |
| --- | --- |
| `JWT_SECRET` | JWT access token 서명용 secret. 예시는 로컬 개발용 placeholder |
| `KMA_SERVICE_KEY` | 공공데이터포털에서 발급받은 인증키. 커밋 금지 |
| `KMA_NX` | 기존 구현과 마이그레이션 호환을 위한 기본 격자 X |
| `KMA_NY` | 기존 구현과 마이그레이션 호환을 위한 기본 격자 Y |
| `KMA_BASE_URL` | 기상청 단기예보 조회서비스 base URL |
| `WEATHER_FALLBACK_ENABLED` | KMA 실패 시 fallback 사용 여부. 기본 `true` |
| `APP_PORT` | 백엔드 API host port |
| `FRONTEND_PORT` | 프론트엔드 host port |
| `VITE_API_BASE_URL` | 브라우저에서 접근할 백엔드 API base URL |

`KMA_NX`, `KMA_NY`는 사용자별 위치 저장 전환 이후 source of truth가 아니다. 기존 환경변수가 남아 있다면 마이그레이션/기본값 보조 용도로만 사용한다.

## 공유 성공 기준

### fallback 공유 성공 기준
- Docker Compose로 MySQL, 백엔드, 프론트엔드가 함께 실행된다.
- 서비스키 없이도 Swagger UI에 접속할 수 있다.
- Frontend에도 접속할 수 있다.
- 회원가입 또는 로그인할 수 있다.
- 로그인 성공 후 access token이 `sessionStorage`에 저장된다.
- `GET /api/users/me`로 로그인 상태가 복구된다.
- 로그인 후 기본 view가 `오늘`이다.
- 첫 추천 준비 체크리스트가 위치, 선호도, 상의, 하의, 아우터 상태를 보여준다.
- 신규 사용자 기본 위치가 서울특별시로 표시된다.
- `GET /api/weather/current`로 현재 사용자 위치 기준 날씨 요약이 표시된다.
- 현재 날씨 요약은 추천 결과를 생성하거나 추천 이력을 만들지 않는다.
- 위치 catalog 검색과 위치 선택이 동작한다.
- 선호도 저장/조회가 동작한다.
- `styleTags`가 화면에 표시되지만 추천 점수와 추천 이유에는 반영되지 않는다.
- 현재 인증 사용자 기준 옷 목록 조회가 된다.
- 색상은 swatch, 소재는 chip, enum은 한국어 라벨로 표시된다.
- 옷 등록, 수정, 보관 처리가 가능하다.
- `POST /api/recommendations`로 추천이 생성된다.
- 추천 결과의 `weather`는 fallback 값일 수 있다.
- 추천 결과에 `top`, `bottom`, `score`, `reasons`, `preferenceScore`가 포함된다.
- 추천 결과는 `reasons`를 "오늘 입기 좋은 이유"로 먼저 보여준다.
- 추천 실패 코드는 한국어 메시지와 CTA로 표시된다.
- 추천 이력 `GET /api/recommendations?limit=20`이 최신순으로 조회된다.
- `PATCH /api/recommendations/{recommendationId}/worn`으로 착용 완료 처리된다.
- Today 최근 추천 preview에서 추천 옷 조합과 착용 여부를 확인할 수 있다.
- 모바일 375px에서 하단 탭 `오늘`, `옷장`, `선호도`, `위치`, `이력`이 겹치지 않는다.

### KMA 연동 공유 성공 기준
- `.env`에 유효한 `KMA_SERVICE_KEY`를 설정한 뒤 앱이 실행된다.
- React 앱에서 로그인하고 사용자 위치를 선택할 수 있다.
- 현재 날씨 요약 조회 시 선택한 사용자 위치의 `nx`, `ny`로 KMA `getVilageFcst` JSON 호출이 수행된다.
- 추천 생성 시 선택한 사용자 위치의 `nx`, `ny`로 KMA `getVilageFcst` JSON 호출이 수행된다.
- KMA 호출이 성공하면 `weather`가 KMA 기반 값으로 반환된다.
- KMA `NODATA` 또는 장애가 발생해도 fallback이 활성화되어 있으면 추천 생성은 성공한다.

## 인증 기준
공개 API는 아래 둘뿐이다.

- `POST /api/auth/signup`
- `POST /api/auth/login`

그 외 API는 보호 API이며 `Authorization: Bearer {accessToken}` header가 필요하다.

프론트 access token 저장 위치는 `sessionStorage`다. JWT access token은 `HS256` + `JWT_SECRET`으로 서명하고 만료 시간은 2시간으로 고정한다. refresh token은 MVP4 공유 범위가 아니다.

## 위치 기준
현재 위치 선택은 외부 지도/주소 API 없이 서버 내장 대표 격자 catalog를 사용한다. `GET /api/locations`는 보호 API이며 로그인 후 위치 선택 화면에서만 호출한다.

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

## 선호도 기준
선호도는 `users` 테이블 JSON 문자열 컬럼에 저장한다.

- `preferred_colors_json`
- `preferred_materials_json`
- `style_tags_json`

API는 배열로 주고받는다.

- `preferredColors`
- `preferredMaterials`
- `styleTags`

`preferredColors`와 `preferredMaterials`는 `preferenceScore`에 반영한다. `styleTags`는 저장/조회/표시만 하며 점수와 추천 이유에는 반영하지 않는다.

## Weather 기준
현재 기본 weather source는 기상청 단기예보 JSON이다.

사용 endpoint:

```text
GET {KMA_BASE_URL}/getVilageFcst
```

요청 parameter:

| Parameter | Value |
| --- | --- |
| `serviceKey` | `KMA_SERVICE_KEY` |
| `pageNo` | `1` |
| `numOfRows` | `1000` |
| `dataType` | `JSON` |
| `base_date` | 최신 제공 가능 발표일자 |
| `base_time` | 최신 제공 가능 발표시각 |
| `nx` | 현재 인증 사용자 위치 `locationNx` |
| `ny` | 현재 인증 사용자 위치 `locationNy` |

fallback 값:

| Field | Value |
| --- | --- |
| `temperature` | `12` |
| `weatherType` | `CLOUDY` |
| `rainy` | `false` |
| `windy` | `false` |

## 자주 발생할 수 있는 문제

| Problem | Resolution |
| --- | --- |
| 8080 포트 충돌 | 기존 8080 사용 프로세스를 종료하거나 `.env`의 `APP_PORT`를 변경한다. |
| 5173 포트 충돌 | 기존 Vite dev server를 종료하거나 `.env`의 `FRONTEND_PORT`를 변경한다. |
| MySQL host port 충돌 | 로컬 MySQL을 중지하거나 `.env`의 `MYSQL_PORT`를 변경한다. 기본값은 `3307`이다. |
| 2차 DB 데이터가 남아 있음 | `docker compose down -v`로 volume을 제거한 뒤 다시 실행한다. |
| `.env` 누락 | `.env.example`을 `.env`로 복사한다. |
| `JWT_SECRET` 누락 | `.env.example`의 로컬 placeholder를 사용하거나 로컬 `.env`에 개발용 값을 설정한다. |
| `KMA_SERVICE_KEY` 누락 | fallback이 활성화되어 있으면 정상이며, 실제 KMA 연동 확인 시에만 키를 설정한다. |
| 보호 API가 401 반환 | 로그인하거나 `sessionStorage` token이 만료/삭제됐는지 확인한다. |
| KMA `NODATA_ERROR` | 발표 직후 또는 요청 시간 계산 문제일 수 있다. fallback이 활성화되어 있으면 데모는 계속 가능하다. |
| 추천 생성 시 OUTER가 없음 | fallback 또는 현재 KMA 날씨가 OUTER 필수 조건이면 seed data에 해당 온도 범위 OUTER가 있는지 확인한다. |

## 비범위
아래 항목은 MVP4 공유 범위가 아니다.

- AWS 배포
- Native mobile app 배포
- PWA install/push notification
- Refresh token
- 소셜 로그인
- 이메일 인증
- 비밀번호 재설정
- 외부 주소/지도 API
- 사용자 현재 위치 자동 감지
- Redis 캐싱
- 이미지 업로드
- AI/GPT 추천
- styleTags 기반 개인화 고도화
- 선호도 별도 테이블 정규화

## 문서 동기화 체크리스트
공유 전 아래 항목을 확인한다.

- MVP4 목표가 "회원가입 또는 로그인 후 2분 안에 첫 추천 성공"으로 유지되는지 확인한다.
- 공개 API와 보호 API 표가 분리되어 있는지 확인한다.
- `GET /api/locations`가 보호 API와 로그인 후 위치 선택 흐름에만 등장하는지 확인한다.
- `GET /api/weather/current`가 보호 API이고 추천 생성/이력 조회처럼 쓰이지 않았는지 확인한다.
- 공개 API 계약에서 `?userId=`가 제거됐는지 확인한다.
- 현재 사용자 전용 응답 예시에서 `userId` 필드가 제거됐는지 확인한다.
- 기존 다양성 점수 표현이 활성 문서에서 `preferenceScore`로 교체됐는지 확인한다.
- `styleTags`가 추천 점수/추천 이유에 반영된다는 표현이 없는지 확인한다.
- 추천 생성 API가 `POST /api/recommendations`인지 확인한다.
- today 추천 GET 경로가 API 계약처럼 보이지 않는지 확인한다.
- 이미지 업로드, AI/GPT 추천, 소셜 로그인, 외부 지도 API가 MVP4 포함 범위처럼 쓰이지 않았는지 확인한다.
- Docker Compose DB 초기화 권장 명령이 반영됐는지 확인한다.
- 실제 API key, token, password, private key가 문서나 코드에 들어가지 않았는지 확인한다.

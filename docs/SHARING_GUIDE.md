# Sharing Guide: SmartCloset 2차 MVP

## 공유 방식
SmartCloset 2차 MVP 공유 방식은 Docker Compose로 유지한다.

공유 대상자는 Docker Compose로 MySQL, Spring Boot 4.0.6 백엔드, React+Vite+TypeScript 프론트엔드를 함께 실행한다. AWS 배포는 제공하지 않는다.

기상청 API key가 없어도 앱은 실행되어야 한다. 이 경우 추천은 `StaticWeatherProvider` fallback 날씨로 생성된다. 실제 기상청 단기예보 JSON 연동을 확인하려면 `.env`에 `KMA_SERVICE_KEY`를 설정한다.

## 전달해야 할 파일/경로
구현 완료 후 공유 시 아래 항목을 포함해야 한다.

- GitHub repository URL
- `README.md`
- `Dockerfile`
- `docker-compose.yml`
- `.env.example`
- `frontend/`: React+Vite+TypeScript SPA
- seed data
- Frontend 경로: http://localhost:5173
- Swagger UI 경로: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON 경로: http://localhost:8080/v3/api-docs

## 실행 명령

```bash
git clone <repository-url>
cd SmartCloset-v2
cp .env.example .env
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
`.env.example`은 실제 비밀값을 포함하지 않는다. 실제 서비스키는 로컬 `.env`에만 넣는다.

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

| Variable | Description |
| --- | --- |
| `KMA_SERVICE_KEY` | 공공데이터포털에서 발급받은 인증키. 커밋 금지 |
| `KMA_NX` | 기존 구현과 마이그레이션 호환을 위한 기본 격자 X |
| `KMA_NY` | 기존 구현과 마이그레이션 호환을 위한 기본 격자 Y |
| `KMA_BASE_URL` | 기상청 단기예보 조회서비스 base URL |
| `WEATHER_FALLBACK_ENABLED` | KMA 실패 시 fallback 사용 여부. 기본 `true` |
| `APP_PORT` | 백엔드 API host port |
| `FRONTEND_PORT` | 프론트엔드 host port |
| `VITE_API_BASE_URL` | 브라우저에서 접근할 백엔드 API base URL |

`KMA_NX`, `KMA_NY`는 2차에서 사용자별 위치 저장으로 대체된다. 기존 환경변수가 남아 있다면 마이그레이션/기본값 보조 용도로만 사용한다.

## 공유 성공 기준

### fallback 공유 성공 기준
- Docker Compose로 MySQL, 백엔드, 프론트엔드가 함께 실행된다.
- 서비스키 없이도 Swagger UI에 접속할 수 있다.
- Frontend에도 접속할 수 있다.
- `userId=1`의 기본 위치가 서울특별시로 표시된다.
- 위치 catalog 검색과 위치 선택이 동작한다.
- `userId=1` 기준 옷 목록 조회가 된다.
- `POST /api/recommendations?userId=1`로 추천이 생성된다.
- 추천 결과의 `weather`는 fallback 값일 수 있다.
- 추천 결과에 `top`, `bottom`, `score`, `reasons`가 포함된다.
- `PATCH /api/recommendations/{recommendationId}/worn?userId=1`로 착용 완료 처리된다.

### KMA 연동 공유 성공 기준
- `.env`에 유효한 `KMA_SERVICE_KEY`를 설정한 뒤 앱이 실행된다.
- React 앱에서 사용자 위치를 선택할 수 있다.
- 추천 생성 시 선택한 사용자 위치의 `nx`, `ny`로 KMA `getVilageFcst` JSON 호출이 수행된다.
- KMA 호출이 성공하면 `weather`가 KMA 기반 값으로 반환된다.
- KMA `NODATA` 또는 장애가 발생해도 fallback이 활성화되어 있으면 추천 생성은 성공한다.

## 위치 기준
2차 위치 선택은 외부 지도/주소 API 없이 서버 내장 대표 격자 catalog를 사용한다.

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
2차 기본 weather source는 기상청 단기예보 JSON이다.

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
| `nx` | 사용자 위치 `locationNx` |
| `ny` | 사용자 위치 `locationNy` |

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
| 이전 DB 데이터가 남아 있음 | `docker compose down -v`로 volume을 제거한 뒤 다시 실행한다. |
| `.env` 누락 | `.env.example`을 `.env`로 복사한다. |
| `KMA_SERVICE_KEY` 누락 | fallback이 활성화되어 있으면 정상이며, 실제 KMA 연동 확인 시에만 키를 설정한다. |
| KMA `NODATA_ERROR` | 발표 직후 또는 요청 시간 계산 문제일 수 있다. fallback이 활성화되어 있으면 데모는 계속 가능하다. |
| 위치가 서울로 보임 | seed user 기본값이다. React 앱에서 위치를 선택한다. |
| 추천 생성 시 OUTER가 없음 | fallback 또는 현재 KMA 날씨가 OUTER 필수 조건이면 seed data에 해당 온도 범위 OUTER가 있는지 확인한다. |

## 비범위
아래 항목은 2차 MVP 공유 범위가 아니다.

- AWS 배포
- 로그인/회원가입
- 외부 주소/지도 API
- 사용자 현재 위치 자동 감지
- Redis 캐싱
- 이미지 업로드
- AI/GPT 추천

## 문서 동기화 체크리스트
공유 전 아래 항목을 확인한다.

- today 추천 GET 경로가 API 계약처럼 보이지 않는지 확인한다.
- 추천 생성 API가 `POST /api/recommendations?userId={userId}`인지 확인한다.
- seed user 기본 위치가 서울특별시 `SEOUL`, `60`, `127`인지 확인한다.
- fallback 값이 `temperature=12`, `weatherType=CLOUDY`, `rainy=false`, `windy=false`인지 확인한다.
- Docker Compose가 필수 공유 방식인지 확인한다.
- React+Vite+TypeScript 앱 접속 경로가 README와 일치하는지 확인한다.
- 실제 API key가 문서나 코드에 들어가지 않았는지 확인한다.
- AWS 배포가 2차 MVP 필수처럼 보이지 않는지 확인한다.

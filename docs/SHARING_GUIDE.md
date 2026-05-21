# Sharing Guide: SmartCloset 1.5차 MVP

## 공유 방식
SmartCloset 1.5차 MVP 공유 방식은 Docker Compose로 유지한다.

AWS 배포는 제공하지 않는다. 공유 대상자는 Docker Compose로 Spring Boot 4.0.6 애플리케이션과 MySQL을 함께 실행하고, Swagger 또는 Demo UI에서 흐름을 확인한다.

기상청 API key가 없어도 앱은 실행되어야 한다. 이 경우 추천은 `StaticWeatherProvider` fallback 날씨로 생성된다. 실제 기상청 단기예보 JSON 연동을 확인하려면 `.env`에 `KMA_SERVICE_KEY`를 설정한다.

## 전달해야 할 파일/경로
구현 완료 후 공유 시 아래 항목을 포함해야 한다.

- GitHub repository URL
- `README.md`
- `Dockerfile`
- `docker-compose.yml`
- `.env.example`
- seed data
- Swagger UI 경로: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON 경로: http://localhost:8080/v3/api-docs
- Demo UI 경로: http://localhost:8080/demo/index.html

## 실행 명령

```bash
git clone <repository-url>
cd SmartCloset-v2
cp .env.example .env
docker compose up --build
```

Swagger UI 접속:

```text
http://localhost:8080/swagger-ui/index.html
```

Demo UI 접속:

```text
http://localhost:8080/demo/index.html
```

중지:

```bash
docker compose down
```

DB까지 초기화:

```bash
docker compose down -v
```

## KMA 환경변수
`.env.example`은 실제 비밀값을 포함하지 않는다. 실제 서비스키는 로컬 `.env`에만 넣는다.

```env
KMA_SERVICE_KEY=
KMA_NX=60
KMA_NY=127
KMA_BASE_URL=http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0
WEATHER_FALLBACK_ENABLED=true
```

| Variable | Description |
| --- | --- |
| `KMA_SERVICE_KEY` | 공공데이터포털에서 발급받은 인증키. 커밋 금지 |
| `KMA_NX` | 기상청 격자 X. 기본 서울특별시 `60` |
| `KMA_NY` | 기상청 격자 Y. 기본 서울특별시 `127` |
| `KMA_BASE_URL` | 기상청 단기예보 조회서비스 base URL |
| `WEATHER_FALLBACK_ENABLED` | KMA 실패 시 fallback 사용 여부. 기본 `true` |

## 공유 성공 기준

### fallback 공유 성공 기준
- Docker Compose로 앱과 MySQL이 실행된다.
- 서비스키 없이도 Swagger UI에 접속할 수 있다.
- `userId=1` 기준 옷 목록 조회가 된다.
- `POST /api/recommendations?userId=1`로 추천이 생성된다.
- 추천 결과의 `weather`는 fallback 값일 수 있다.
- 추천 결과에 `top`, `bottom`, `score`, `reasons`가 포함된다.
- `PATCH /api/recommendations/{recommendationId}/worn?userId=1`로 착용 완료 처리된다.

### KMA 연동 공유 성공 기준
- `.env`에 유효한 `KMA_SERVICE_KEY`를 설정한 뒤 앱이 실행된다.
- Swagger 또는 Demo UI에서 추천 생성 시 KMA `getVilageFcst` JSON 호출이 성공하면 `weather`가 KMA 기반 값으로 반환된다.
- KMA `NODATA` 또는 장애가 발생해도 fallback이 활성화되어 있으면 추천 생성은 성공한다.

## Weather 기준
1.5차 기본 weather source는 기상청 단기예보 JSON이다.

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
| `nx` | `KMA_NX`, 기본 `60` |
| `ny` | `KMA_NY`, 기본 `127` |

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
| MySQL host port 충돌 | 로컬 MySQL을 중지하거나 `.env`의 `MYSQL_PORT`를 변경한다. 기본값은 `3307`이다. |
| 이전 DB 데이터가 남아 있음 | `docker compose down -v`로 volume을 제거한 뒤 다시 실행한다. |
| MySQL 준비 전에 Spring Boot가 먼저 뜸 | Docker Compose의 MySQL healthcheck와 `depends_on` 설정을 확인한다. |
| Swagger 접속 경로 오타 | `http://localhost:8080/swagger-ui/index.html`로 접속한다. |
| `.env` 누락 | `.env.example`을 `.env`로 복사한다. |
| `KMA_SERVICE_KEY` 누락 | fallback이 활성화되어 있으면 정상이며, 실제 KMA 연동 확인 시에만 키를 설정한다. |
| KMA `NODATA_ERROR` | 발표 직후 또는 요청 시간 계산 문제일 수 있다. fallback이 활성화되어 있으면 데모는 계속 가능하다. |
| 추천 생성 시 OUTER가 없음 | fallback 또는 현재 KMA 날씨가 OUTER 필수 조건이면 seed data에 해당 온도 범위 OUTER가 있는지 확인한다. |

## 비범위
아래 항목은 1.5차 MVP 공유 범위가 아니다.

- AWS 배포
- 로그인/회원가입
- 사용자별 위치 저장
- 위치 변경 API
- Redis 캐싱
- 이미지 업로드
- AI/GPT 추천
- 정식 프론트엔드 앱

## 문서 동기화 체크리스트
공유 전 아래 항목을 확인한다.

- today 추천 GET 경로가 API 계약처럼 보이지 않는지 확인한다.
- 추천 생성 API가 `POST /api/recommendations?userId={userId}`인지 확인한다.
- KMA 기본 격자가 `KMA_NX=60`, `KMA_NY=127`인지 확인한다.
- fallback 값이 `temperature=12`, `weatherType=CLOUDY`, `rainy=false`, `windy=false`인지 확인한다.
- Docker Compose가 필수 공유 방식인지 확인한다.
- 실제 API key가 문서나 코드에 들어가지 않았는지 확인한다.
- AWS 배포가 1.5차 MVP 필수처럼 보이지 않는지 확인한다.
- Demo UI가 정식 프론트엔드 앱처럼 보이지 않는지 확인한다.

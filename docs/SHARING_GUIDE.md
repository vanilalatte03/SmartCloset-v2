# Sharing Guide: SmartCloset 1차 MVP

## 공유 방식
SmartCloset 1차 MVP 공유 방식은 Docker Compose로 고정한다.

AWS 배포는 제공하지 않는다. 외부 Weather API를 사용하지 않으므로 별도 Weather API key도 필요 없다. 공유 대상자는 Docker Compose로 Spring Boot 4.0.6 애플리케이션과 MySQL을 함께 실행하고 P0는 Swagger에서 흐름을 확인한다. P1 Demo UI가 구현된 경우에는 Demo UI에서도 같은 흐름을 확인한다.

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

## 공유 성공 기준

### P0 공유 성공 기준
- Docker Compose로 앱과 MySQL이 실행된다.
- Swagger UI에 접속할 수 있다.
- `userId=1` 기준 옷 목록 조회가 된다.
- `POST /api/recommendations?userId=1`로 추천이 생성된다.
- 추천 결과에 `top`, `bottom`, `outer`, `score`, `reasons`가 포함된다.
- `PATCH /api/recommendations/{recommendationId}/worn?userId=1`로 착용 완료 처리된다.

P0만 완료되어도 1차 MVP 공유 조건을 만족한다.

### P1 공유 성공 기준
- Demo UI에서 옷 목록 조회, 옷 등록, 추천 생성, 착용 완료가 가능하다.
- GitHub Actions test/build가 통과한다.

## StaticWeatherProvider 기준
1차 MVP는 고정 테스트 날씨를 사용한다.

| Field | Value |
| --- | --- |
| `temperature` | `12` |
| `weatherType` | `CLOUDY` |
| `rainy` | `false` |
| `windy` | `false` |

`temperature=12`이므로 OUTER 필수 추천 흐름이 재현되어야 한다.

## 자주 발생할 수 있는 문제

| Problem | Resolution |
| --- | --- |
| 8080 포트 충돌 | 기존 8080 사용 프로세스를 종료하거나 `.env`의 `APP_PORT`를 변경한다. |
| MySQL host port 충돌 | 로컬 MySQL을 중지하거나 `.env`의 `MYSQL_PORT`를 변경한다. 기본값은 `3307`이다. |
| 이전 DB 데이터가 남아 있음 | `docker compose down -v`로 volume을 제거한 뒤 다시 실행한다. |
| MySQL 준비 전에 Spring Boot가 먼저 뜸 | Docker Compose의 MySQL healthcheck와 `depends_on` 설정을 확인한다. |
| Swagger 접속 경로 오타 | `http://localhost:8080/swagger-ui/index.html`로 접속한다. |
| `.env` 누락 | `.env.example`을 `.env`로 복사한다. |
| 추천 생성 시 OUTER가 없음 | seed data에 `temperature=12`에 맞는 OUTER가 포함되어 있는지 확인한다. |

## 비범위
아래 항목은 1차 MVP 공유 범위가 아니다.

- AWS 배포
- 외부 Weather API
- 회원가입/로그인
- 이미지 업로드
- AI/GPT 추천
- 정식 프론트엔드 앱

## 문서 동기화 체크리스트
공유 전 아래 항목을 확인한다.

- 예전 GET 기반 today 추천 경로 표현이 남아 있으면 `POST /api/recommendations?userId={userId}`로 수정한다.
- `StaticWeatherProvider` 기본 날씨가 모든 문서에서 `temperature=12`, `weatherType=CLOUDY`, `rainy=false`, `windy=false`인지 확인한다.
- Docker Compose가 유일한 필수 공유 방식인지 확인한다.
- AWS 수동 배포가 1차 MVP 필수처럼 보이지 않는지 확인한다.
- 외부 Weather API가 1차 MVP 필수처럼 보이지 않는지 확인한다.
- Demo UI가 정식 프론트엔드 앱처럼 보이지 않는지 확인한다.

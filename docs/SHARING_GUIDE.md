# Sharing Guide: SmartCloset MVP7

## 공유 방식

SmartCloset MVP7 공유 방식은 Docker Compose로 유지한다.

공유 대상자는 Docker Compose로 MySQL, Spring Boot 4.0.6 백엔드, React+Vite+TypeScript 프론트엔드, 이미지 저장 volume을 함께 실행한다. AWS 배포, S3, CDN, native mobile app, PWA 배포는 MVP7 공유 범위가 아니다.

## MVP7 공유 기준

Docker Compose로 `mysql`, `app`, `frontend`가 함께 실행되고, 신규 사용자가 로그인 후 KMA catalog 위치를 검색하거나 현재 위치 후보를 선택하고, 예보 시간대를 골라 추천을 만들며, 추천 결과와 History에서 위치/날씨 source snapshot을 확인할 수 있어야 한다.

## 전달해야 할 파일/경로

- GitHub repository URL
- `README.md`
- `Dockerfile`
- `docker-compose.yml`
- `.env.example`
- `frontend/`
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

DB와 이미지 volume까지 초기화:

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

CLOTHING_IMAGE_STORAGE_DIR=/data/smartcloset/clothing-images
CLOTHING_IMAGE_MAX_SIZE_BYTES=5242880

APP_PORT=8080
FRONTEND_PORT=5173
VITE_API_BASE_URL=http://localhost:8080
```

| Variable | Description |
| --- | --- |
| `JWT_SECRET` | JWT access token 서명용 secret. 예시는 로컬 개발용 placeholder |
| `KMA_SERVICE_KEY` | 공공데이터포털에서 발급받은 인증키. 커밋 금지 |
| `KMA_NX`, `KMA_NY` | legacy/default KMA grid 값. MVP7 위치 선택 흐름은 사용자 저장 위치의 grid를 우선 사용 |
| `KMA_BASE_URL` | KMA 단기예보 조회서비스 base URL |
| `WEATHER_FALLBACK_ENABLED` | KMA 실패 시 fallback 사용 여부. 기본 `true` |
| `CLOTHING_IMAGE_STORAGE_DIR` | app container 내부 이미지 저장 경로 |
| `CLOTHING_IMAGE_MAX_SIZE_BYTES` | 이미지 업로드 최대 크기 bytes. 기본 5MB |
| `APP_PORT` | 백엔드 API host port |
| `FRONTEND_PORT` | 프론트엔드 host port |
| `VITE_API_BASE_URL` | 브라우저에서 접근할 백엔드 API base URL |

MVP7은 사용자 위치를 KMA catalog에서 선택하므로 고정 `KMA_NX`, `KMA_NY`가 새 위치 선택 흐름의 기준이 아니다. 기존 환경 호환을 위해 `.env.example`에는 기본값을 남긴다.

## 공유 성공 기준

### 기본 앱 기준

- Docker Compose로 MySQL, 백엔드, 프론트엔드가 함께 실행된다.
- 서비스키 없이도 Swagger UI에 접속할 수 있다.
- Frontend에도 접속할 수 있다.
- 회원가입 또는 로그인할 수 있다.
- 로그인 성공 후 access token이 `sessionStorage`에 저장된다.
- 로그인 후 Today, Closet, Preferences, Location, History view를 탐색할 수 있다.

### MVP7 위치/날씨 신뢰 기준

- Location에서 동네 단위 KMA catalog 검색을 사용할 수 있다.
- `일산동`처럼 동명이인이 있는 검색어는 구분 가능한 후보를 보여준다.
- 현재 위치로 찾기 버튼은 브라우저 권한 요청 뒤 후보를 표시한다.
- 현재 위치 후보는 자동 저장되지 않고 사용자가 선택해야 저장된다.
- Today에서 `CURRENT`, `MORNING`, `AFTERNOON`, `EVENING` 중 하나를 선택할 수 있다.
- body 없는 추천 생성은 `CASUAL`, `CURRENT`로 성공한다.
- 추천 결과에 위치, KMA/fallback 여부, base/forecast 시각이 표시된다.
- History에서 과거 추천별 위치/날씨 source snapshot을 확인할 수 있다.
- 사용자 현재 위치 변경 후에도 과거 추천 snapshot은 바뀌지 않는다.

### MVP6 개인화 기준

- Closet에서 옷별 styleTags를 저장하고 확인할 수 있다.
- Today에서 상황을 선택할 수 있다.
- 추천 결과에서 착용 완료를 저장할 수 있다.
- 추천 결과에서 마음에 들어요, 별로예요, 추웠어요, 더웠어요 피드백을 저장할 수 있다.
- 피드백을 전체 교체하거나 clear할 수 있다.
- styleTags와 최근 피드백이 추천 점수와 이유에 반영된다.

### 이미지 기준

- 옷 등록 후 이미지 없이도 카드가 정상 표시된다.
- `PUT /api/clothes/{clothingId}/image`로 이미지를 업로드할 수 있다.
- 옷 카드에 썸네일이 표시된다.
- `DELETE /api/clothes/{clothingId}/image`로 이미지를 삭제할 수 있다.
- `GET /api/clothes/{clothingId}/image`는 인증 없이 접근할 수 없다.
- app container를 재시작해도 업로드 이미지가 유지된다.

## 인증 기준

공개 API는 아래 둘뿐이다.

- `POST /api/auth/signup`
- `POST /api/auth/login`

그 외 API는 보호 API이며 `Authorization: Bearer {accessToken}` header가 필요하다. 이미지 조회 API, 위치 resolve API, 추천 피드백 API도 보호 API다.

프론트 access token 저장 위치는 `sessionStorage`다. JWT access token은 `HS256` + `JWT_SECRET`으로 서명하고 만료 시간은 2시간으로 고정한다.

## 제외 범위 확인

MVP7 공유 문서와 데모에는 아래 기능을 포함하지 않는다.

- 외부 주소/지도 API
- 지도 렌더링
- raw KMA 응답 JSON 저장
- GPS 좌표 원문 DB 저장
- KMA `getVilageFcst` 외 weather API
- AI 자동 태깅
- AI/GPT 추천
- 피드백 analytics dashboard
- 다중 이미지 업로드
- 이미지 편집
- S3/CDN
- refresh token
- 소셜 로그인
- 이메일 인증
- 비밀번호 재설정
- AWS 배포와 CD 자동화

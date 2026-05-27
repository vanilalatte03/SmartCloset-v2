# Sharing Guide: SmartCloset MVP8

## 공유 방식

SmartCloset MVP8 공유 방식은 Docker Compose local 실행으로 유지한다.

공유 대상자는 Docker Compose로 MySQL, Spring Boot 4.0.6 백엔드, React+Vite+TypeScript 프론트엔드, 이미지 저장 volume을 함께 실행한다. AWS 배포, S3, SES, Secrets Manager, CD 자동화는 MVP8 공유 범위가 아니다.

## MVP8 공유 기준

Docker Compose로 `mysql`, `app`, `frontend`가 함께 실행되고, 신규 사용자가 이메일 인증 후 로그인하고, refresh cookie로 세션을 복구하며, 비밀번호 재설정과 계정 삭제 흐름을 확인할 수 있어야 한다.

MVP5 이미지, MVP6 피드백/개인화, MVP7 위치/날씨 신뢰도 흐름은 계속 확인 가능해야 한다.

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

`.env.example`은 실제 비밀값을 포함하지 않는다. 실제 서비스키와 운영 JWT secret, OAuth client secret은 로컬 `.env` 또는 배포 환경에만 넣는다.

MVP8에서 필요한 환경변수 범위:

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
REFRESH_TOKEN_COOKIE_NAME=smartcloset.refreshToken
REFRESH_TOKEN_COOKIE_SECURE=false
REFRESH_TOKEN_COOKIE_SAME_SITE=Lax
REFRESH_TOKEN_COOKIE_DOMAIN=
REFRESH_TOKEN_COOKIE_PATH=/api/auth
REFRESH_TOKEN_TTL_DAYS=14

KMA_SERVICE_KEY=
KMA_NX=60
KMA_NY=127
KMA_BASE_URL=http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0
WEATHER_FALLBACK_ENABLED=true

CLOTHING_IMAGE_STORAGE_DIR=/data/smartcloset/clothing-images
CLOTHING_IMAGE_MAX_SIZE_BYTES=5242880

GOOGLE_OAUTH_CLIENT_ID=
GOOGLE_OAUTH_CLIENT_SECRET=
GOOGLE_OAUTH_REDIRECT_URI=http://localhost:8080/api/auth/oauth2/callback/google
FRONTEND_AUTH_CALLBACK_URL=http://localhost:5173/auth/callback
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173
CORS_ALLOW_CREDENTIALS=true

APP_PORT=8080
FRONTEND_PORT=5173
VITE_API_BASE_URL=http://localhost:8080
```

| Variable | Description |
| --- | --- |
| `JWT_SECRET` | JWT access token 서명용 secret. 예시는 로컬 개발용 placeholder |
| `REFRESH_TOKEN_COOKIE_*` | refresh cookie name, secure, SameSite, domain, path 설정 |
| `REFRESH_TOKEN_TTL_DAYS` | refresh session 만료 기준 |
| `GOOGLE_OAUTH_CLIENT_ID`, `GOOGLE_OAUTH_CLIENT_SECRET` | Google OAuth 설정. 비어 있으면 provider disabled |
| `GOOGLE_OAUTH_REDIRECT_URI` | backend Google callback URL |
| `FRONTEND_AUTH_CALLBACK_URL` | OAuth 성공 후 frontend callback URL |
| `OAUTH_STATE_COOKIE_*` | Google OAuth state cookie name, secure, SameSite, domain, path, max age 설정 |
| `CORS_ALLOWED_ORIGINS` | credential 요청을 허용할 frontend origin 목록 |
| `CORS_ALLOW_CREDENTIALS` | refresh cookie 요청을 위한 CORS credentials 허용 여부 |
| `KMA_SERVICE_KEY` | 공공데이터포털에서 발급받은 인증키. 커밋 금지 |
| `WEATHER_FALLBACK_ENABLED` | KMA 실패 시 fallback 사용 여부. 기본 `true` |
| `CLOTHING_IMAGE_STORAGE_DIR` | app container 내부 이미지 저장 경로 |
| `VITE_API_BASE_URL` | 브라우저에서 접근할 백엔드 API base URL |

## 공유 성공 기준

### 기본 앱 기준

- Docker Compose로 MySQL, 백엔드, 프론트엔드가 함께 실행된다.
- 서비스키 없이도 Swagger UI에 접속할 수 있다.
- Frontend에도 접속할 수 있다.
- 회원가입 후 이메일 인증 필요 안내가 표시된다.
- backend console/log에서 인증 token 또는 링크를 확인할 수 있다.
- 이메일 인증 후 로그인할 수 있다.
- refresh cookie로 새로고침 후 세션을 복구할 수 있다.
- 로그인 후 Today, Closet, Preferences, Location, History, Account settings를 탐색할 수 있다.

### MVP8 계정 안정성 기준

- 미인증 password 계정 login이 차단된다.
- `POST /api/auth/refresh`가 refresh cookie로 access token을 재발급한다.
- `POST /api/auth/logout`이 refresh session을 revoke하고 cookie를 만료한다.
- 비밀번호 재설정 요청/확인이 가능하다.
- Google provider 설정이 없으면 disabled 상태가 표시된다.
- 계정 삭제가 현재 사용자 데이터와 이미지 파일을 삭제한다.
- 세션 만료 시 refresh retry-once 후 실패 안내가 표시된다.

### 기존 기능 기준

- Location에서 동네 단위 KMA catalog 검색을 사용할 수 있다.
- Today에서 `CURRENT`, `MORNING`, `AFTERNOON`, `EVENING` 중 하나를 선택할 수 있다.
- 추천 결과와 History에서 위치/날씨 source snapshot을 확인할 수 있다.
- Closet에서 옷별 styleTags를 저장하고 확인할 수 있다.
- 추천 결과에서 착용 완료와 피드백 저장/clear를 사용할 수 있다.
- 옷 이미지 업로드/조회/삭제가 보호 API로 동작한다.

## 인증 기준

MVP8 공개 API는 auth/account bootstrap endpoint만 허용한다. 그 외 `/api/**` endpoint는 보호 API이며 `Authorization: Bearer {accessToken}` header가 필요하다.

Access token은 JSON 응답으로 받고 frontend memory state에 저장한다. Refresh token은 HttpOnly cookie로만 전달한다.

## AWS-ready 기준

MVP8 공유 문서에는 AWS 구현을 포함하지 않는다. 다만 MVP9를 위해 아래 경계는 유지해야 한다.

- `EmailSender`는 MVP9에서 SES/SMTP 구현체로 교체 가능해야 한다.
- `ClothingImageStorage`는 MVP9에서 S3 구현체를 추가할 수 있어야 한다.
- Cookie/CORS/OAuth redirect/base URL은 env로 바꿀 수 있어야 한다.
- local Docker Compose 실행은 prod profile 추가 후에도 유지되어야 한다.

## 제외 범위 확인

MVP8 공유 문서와 데모에는 아래 기능을 포함하지 않는다.

- AWS 배포
- S3 storage 구현
- SES/SMTP 실제 발송 구현
- Secrets Manager
- CD 자동화
- Redis
- admin 계정 관리
- soft delete/복구 정책
- production DB migration 도구 전환
- AI/GPT 추천
- AI 자동 태깅

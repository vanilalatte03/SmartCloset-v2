# Sharing Guide: SmartCloset MVP10

## 공유 방식

SmartCloset MVP10 공유 방식은 Docker Compose local 실행으로 유지한다.

공유 대상자는 Docker Compose로 MySQL, 이미지 저장 volume 권한 helper, Spring Boot 4.0.6 백엔드, React+Vite+TypeScript 프론트엔드, 이미지 저장 volume을 함께 실행한다. MVP10 AI 옷 등록 보조는 기본 비활성 상태이므로 OpenAI API key 없이도 기존 기능과 앱 데모가 동작해야 한다.

운영 준비용 prod-like runtime은 `docker-compose.prod.yml`과 Nginx frontend image로 별도 검증한다. AWS 배포, S3, SES, Secrets Manager, CD 자동화는 MVP10 공유 범위가 아니다.

## MVP10 공유 기준

Docker Compose로 `mysql`, `app`, `frontend`가 함께 실행되고, 사용자가 MVP8 계정 안정성 흐름, MVP9 화면 구조, MVP10 옷 등록 AI 후보 체크 UI를 브라우저에서 확인할 수 있어야 한다.

MVP5 이미지, MVP6 피드백/개인화, MVP7 위치/날씨 신뢰도, MVP8 계정 안정성 흐름은 계속 확인 가능해야 한다.

## 전달해야 할 파일/경로

- GitHub repository URL
- `README.md`
- `Dockerfile`
- `docker-compose.yml`
- `docker-compose.prod.yml`
- `.env.example`
- `.env.prod.example`
- `frontend/`
- `frontend/Dockerfile`
- `frontend/nginx.conf`
- Frontend 경로: http://localhost:5173
- Swagger UI 경로: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON 경로: http://localhost:8080/v3/api-docs
- Actuator health 경로: http://localhost:8080/actuator/health
- Prometheus metrics 경로: http://localhost:8080/actuator/prometheus
- 운영 관측성 baseline: `monitoring/`

## 실행 명령

```bash
git clone <repository-url>
cd smart-closet
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

Prod-like runtime smoke:

```bash
scripts/prod-compose-smoke.sh
```

`docker-compose.prod.yml`은 local/demo compose와 분리된 운영 준비 산출물이다. Top-level project name은 `smartcloset-prod`이고 prod volume은 기본값 `smartcloset-prod-mysql-data`, `smartcloset-prod-clothing-image-data`로 명시한다. Prod smoke는 volume name을 임시 project prefix로 override해 실제 prod volume을 건드리지 않는다. `.env.prod.example`은 실제 secret 값을 비워 둔 checklist이므로 그대로 실행하지 않는다. 운영 또는 prod smoke env는 최소한 `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `FRONTEND_AUTH_CALLBACK_URL`, `VITE_API_BASE_URL`을 채워야 한다. Prod smoke project override는 `SMARTCLOSET_PROD_SMOKE_PROJECT`만 사용하며 `smartclosetprodsmoke` prefix로 제한한다.

## 환경변수

`.env.example`은 실제 비밀값을 포함하지 않는다. 실제 서비스키, 운영 JWT secret, OAuth client secret, OpenAI API key는 로컬 `.env` 또는 배포 환경에만 넣는다.

MVP10 기본 공유 환경:

```env
MYSQL_DATABASE=smartcloset
MYSQL_USER=smartcloset
MYSQL_PASSWORD=smartcloset
MYSQL_ROOT_PASSWORD=root
MYSQL_PORT=3307

SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/smartcloset?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
SPRING_DATASOURCE_USERNAME=smartcloset
SPRING_DATASOURCE_PASSWORD=smartcloset
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
SPRING_FLYWAY_ENABLED=true
SPRING_PROFILES_ACTIVE=local
SMARTCLOSET_SEED_ENABLED=true
JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75.0
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,prometheus
MANAGEMENT_PROMETHEUS_METRICS_EXPORT_ENABLED=true
SMARTCLOSET_HEALTHCHECK_URL=http://127.0.0.1:8080/actuator/health

JWT_SECRET=change-me-local-development-only
REFRESH_TOKEN_COOKIE_NAME=smartcloset.refreshToken
REFRESH_TOKEN_COOKIE_SECURE=false
REFRESH_TOKEN_COOKIE_SAME_SITE=Lax
REFRESH_TOKEN_COOKIE_DOMAIN=
REFRESH_TOKEN_COOKIE_PATH=/api/auth
REFRESH_TOKEN_COOKIE_MAX_AGE=14d
REFRESH_TOKEN_TTL_DAYS=14

KMA_SERVICE_KEY=
KMA_NX=60
KMA_NY=127
KMA_BASE_URL=http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0
KMA_CACHE_TTL=2m
KMA_CACHE_MAX_SIZE=256
WEATHER_FALLBACK_ENABLED=true

CLOTHING_IMAGE_STORAGE_DIR=/data/smartcloset/clothing-images
CLOTHING_IMAGE_MAX_SIZE_BYTES=5242880

CLOTHING_ANALYSIS_ENABLED=false
SPRING_AI_MODEL_CHAT=none
OPENAI_API_KEY=
CLOTHING_ANALYSIS_MODEL=gpt-5.4-nano
CLOTHING_ANALYSIS_LOW_CONFIDENCE_THRESHOLD=0.75
CLOTHING_ANALYSIS_DAILY_LIMIT=20
CLOTHING_ANALYSIS_TIMEOUT_SECONDS=10

GOOGLE_OAUTH_CLIENT_ID=
GOOGLE_OAUTH_CLIENT_SECRET=
GOOGLE_OAUTH_REDIRECT_URI=http://localhost:8080/api/auth/oauth2/callback/google
GOOGLE_OAUTH_CONNECT_TIMEOUT=3s
GOOGLE_OAUTH_READ_TIMEOUT=5s
FRONTEND_AUTH_CALLBACK_URL=http://localhost:5173/auth/callback
OAUTH_STATE_COOKIE_NAME=smartcloset.oauth2State
OAUTH_STATE_COOKIE_SECURE=false
OAUTH_STATE_COOKIE_SAME_SITE=Lax
OAUTH_STATE_COOKIE_DOMAIN=
OAUTH_STATE_COOKIE_PATH=/api/auth/oauth2
OAUTH_STATE_COOKIE_MAX_AGE=5m
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
| `REFRESH_TOKEN_COOKIE_MAX_AGE` | refresh cookie Max-Age. local 기본값은 `14d` |
| `REFRESH_TOKEN_TTL_DAYS` | refresh session 만료 기준 |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Hibernate schema action. local/prod 기본값은 `validate`이며 schema 생성/변경은 Flyway가 담당 |
| `SPRING_FLYWAY_ENABLED` | Flyway migration 실행 여부. 기본값은 `true` |
| `SPRING_FLYWAY_BASELINE_ON_MIGRATE` | 기존 non-empty schema를 baseline으로 편입할지 여부. local/demo 기본값은 `true`, prod 기본값은 `false` |
| `SPRING_PROFILES_ACTIVE` | Docker Compose 기본값은 `local`. local/demo profile에서만 demo seed initializer가 bean 후보가 됨 |
| `SMARTCLOSET_SEED_ENABLED` | local/demo profile의 demo user와 최소 옷장 seed 활성 여부. local 기본값은 `true`, default/prod profile에서는 initializer가 비활성 |
| `SMARTCLOSET_EMAIL_OUTBOX_PATH` | local/demo 이메일 인증과 비밀번호 재설정 token outbox 파일 경로. Docker Compose 기본값은 `/tmp/smartcloset-email-outbox.log` |
| `JAVA_TOOL_OPTIONS` | JVM container option. local 기본값은 `-XX:MaxRAMPercentage=75.0` |
| `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE` | Actuator web endpoint 노출 목록. local 기본값은 `health,info,prometheus`, prod profile 기본값은 `health,prometheus` |
| `MANAGEMENT_PROMETHEUS_METRICS_EXPORT_ENABLED` | Prometheus metric export 활성 여부. 기본값은 `true` |
| `LOGGING_STRUCTURED_FORMAT_CONSOLE` | Console log 구조화 format. 기본 `ecs`, 빈 값으로 override하면 Spring Boot 기본 console log 사용 |
| `SMARTCLOSET_SERVICE_VERSION` | ECS structured log service version. 기본 `APP_VERSION` 또는 `0.0.1-SNAPSHOT` |
| `SMARTCLOSET_SERVICE_ENVIRONMENT` | ECS structured log service environment. 기본 `SPRING_PROFILES_ACTIVE` 또는 `local` |
| `MANAGEMENT_LOGGING_EXPORT_OTLP_ENABLED` | OTLP log export 활성화 여부. 기본 `false` |
| `MANAGEMENT_OTLP_METRICS_EXPORT_ENABLED` | OTLP metrics push export 활성화 여부. 기본 `false` |
| `MANAGEMENT_TRACING_SAMPLING_PROBABILITY` | Micrometer tracing sampling 확률. 기본 `1.0` |
| `MANAGEMENT_TRACING_EXPORT_OTLP_ENABLED` | OTLP trace export 활성화 여부. 기본 `false` |
| `MANAGEMENT_OPENTELEMETRY_TRACING_EXPORT_OTLP_ENDPOINT` | OTLP trace collector endpoint. 기본 `http://localhost:4318/v1/traces` |
| `SMARTCLOSET_HEALTHCHECK_URL` | Dockerfile healthcheck가 호출할 app 내부 URL. 기본값은 `http://127.0.0.1:8080/actuator/health` |
| `GOOGLE_OAUTH_CLIENT_ID`, `GOOGLE_OAUTH_CLIENT_SECRET` | Google OAuth 설정. 비어 있으면 provider disabled |
| `GOOGLE_OAUTH_REDIRECT_URI` | backend Google callback URL |
| `GOOGLE_OAUTH_CONNECT_TIMEOUT`, `GOOGLE_OAUTH_READ_TIMEOUT` | Google token/userinfo provider 호출 timeout |
| `FRONTEND_AUTH_CALLBACK_URL` | OAuth 성공 후 frontend callback URL |
| `OAUTH_STATE_COOKIE_*` | Google OAuth state cookie name, secure, SameSite, domain, path, max age 설정 |
| `CORS_ALLOWED_ORIGINS` | credential 요청을 허용할 frontend origin 목록 |
| `CORS_ALLOW_CREDENTIALS` | refresh cookie 요청을 위한 CORS credentials 허용 여부 |
| `SPRINGDOC_API_DOCS_ENABLED` | OpenAPI JSON 노출 여부. local 기본값은 `true`, prod profile 기본값은 `false` |
| `SPRINGDOC_SWAGGER_UI_ENABLED` | Swagger UI 노출 여부. local 기본값은 `true`, prod profile 기본값은 `false` |
| `KMA_SERVICE_KEY` | 공공데이터포털에서 발급받은 인증키. 커밋 금지 |
| `KMA_CACHE_TTL`, `KMA_CACHE_MAX_SIZE` | KMA 날씨 process-local cache의 TTL과 entry 상한. 기본 `2m`, `256` |
| `WEATHER_FALLBACK_ENABLED` | KMA 실패 시 fallback 사용 여부. 기본 `true` |
| `CLOTHING_IMAGE_STORAGE_DIR` | app container 내부 이미지 저장 경로 |
| `CLOTHING_ANALYSIS_ENABLED` | 옷 사진 AI 분석 기능 활성 여부. 기본 `false` |
| `SPRING_AI_MODEL_CHAT` | Spring AI chat model 활성화. 기본 `none`, 실제 호출 시 `openai` |
| `OPENAI_API_KEY` | OpenAI API key. 실제 값 커밋 금지 |
| `CLOTHING_ANALYSIS_MODEL` | 기본 분석 모델. MVP10 기준 `gpt-5.4-nano` |
| `CLOTHING_ANALYSIS_LOW_CONFIDENCE_THRESHOLD` | 확인 필요 기준 confidence. 기본 `0.75` |
| `CLOTHING_ANALYSIS_DAILY_LIMIT` | user별 일일 분석 제한. 기본 `20` |
| `CLOTHING_ANALYSIS_TIMEOUT_SECONDS` | 분석 provider timeout. 기본 `10` |
| `VITE_API_BASE_URL` | 브라우저에서 접근할 백엔드 API base URL |

Prod runtime 필수/보안 env:

| Variable | Prod 기준 |
| --- | --- |
| `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`, `SPRING_DATASOURCE_PASSWORD` | 실제 운영 secret으로만 주입. `.env.prod.example`에는 값을 두지 않음 |
| `MYSQL_DATA_VOLUME_NAME`, `CLOTHING_IMAGE_DATA_VOLUME_NAME` | prod volume 이름. 기본값은 local/demo volume과 충돌하지 않는 `smartcloset-prod-*` |
| `JWT_SECRET` | local placeholder 금지. prod profile safety guard가 빈 값과 local placeholder를 차단 |
| `SPRING_PROFILES_ACTIVE` | `docker-compose.prod.yml`에서 `prod`로 고정 |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | 기본 `validate`. `update/create/create-drop` 계열은 prod safety guard가 차단 |
| `SMARTCLOSET_SEED_ENABLED` | 기본 `false` |
| `REFRESH_TOKEN_COOKIE_SECURE`, `OAUTH_STATE_COOKIE_SECURE` | 기본 `true`. prod safety guard가 `false`를 차단 |
| `REFRESH_TOKEN_COOKIE_SAME_SITE`, `OAUTH_STATE_COOKIE_SAME_SITE` | 기본 `None`; Secure cookie와 함께 사용 |
| `CORS_ALLOWED_ORIGINS` | 운영 frontend origin을 명시. wildcard credential 조합을 사용하지 않음 |
| `FRONTEND_AUTH_CALLBACK_URL`, `VITE_API_BASE_URL` | 운영 frontend/backend public URL 기준으로 명시 |
| `SPRINGDOC_API_DOCS_ENABLED`, `SPRINGDOC_SWAGGER_UI_ENABLED` | 기본 `false` |

Prod 관측성 기준:

- Console log는 기본 ECS JSON 구조화 log다.
- `X-Trace-Id` response header로 사용자가 보고한 API 실패를 trace/log와 연결한다.
- OTLP trace/log/metrics push export는 명시적으로 활성화한 환경에서만 켠다. 공유 local/demo 기본값은 collector 없이 실행되도록 비활성이다.
- Sentry/Datadog 같은 vendor-specific error tracking SDK와 secret은 이번 공유 baseline에 포함하지 않는다.

## AI 옷 등록 보조 활성화

기본 공유 데모는 OpenAI 호출 없이 진행한다. 실제 분석을 확인할 때만 로컬 `.env`에 아래처럼 설정한다.

```env
CLOTHING_ANALYSIS_ENABLED=true
SPRING_AI_MODEL_CHAT=openai
OPENAI_API_KEY=<local-only-openai-api-key>
CLOTHING_ANALYSIS_MODEL=gpt-5.4-nano
```

주의:

- 실제 API key는 `.env.example`, 문서, 코드, 커밋에 넣지 않는다.
- 분석은 사용자가 `AI 후보 체크`를 누를 때만 실행한다.
- provider 실패 시 자동으로 더 비싼 fallback model을 호출하지 않는다.
- 기능을 끄면 기존 manual 옷 등록/수정 flow는 계속 동작해야 한다.

## 공유 성공 기준

### 기본 앱 기준

- Docker Compose로 MySQL, 백엔드, 프론트엔드가 함께 실행된다.
- app container는 non-root `10001:10001` user로 실행된다.
- `clothing-image-volume-permissions` service가 app 시작 전에 이미지 저장 volume 소유권을 `10001:10001`로 보정한다.
- app image는 Actuator health 기반 Dockerfile healthcheck를 가진다.
- frontend service는 app healthcheck가 healthy가 된 뒤 시작된다.
- `GET /actuator/health`가 `UP`을 반환한다.
- `GET /actuator/prometheus`에서 JVM/HikariCP metric과 요청 후 SmartCloset custom metric을 확인할 수 있다.
- `monitoring/prometheus/alerts.yml`, `monitoring/alertmanager/alertmanager.yml`, `monitoring/grafana/smartcloset-dashboard.json`이 실제 secret이나 webhook 없이 baseline으로 제공된다.
- OpenAI API key 없이도 Swagger UI에 접속할 수 있다.
- Frontend에도 접속할 수 있다.
- Auth 화면이 데스크톱/모바일에서 form을 읽을 수 있게 표시된다.
- 회원가입 후 이메일 인증 필요 안내가 표시된다.
- backend local outbox 파일에서 인증 token을 확인할 수 있다. Docker Compose에서는 `docker compose exec app tail -n 20 /tmp/smartcloset-email-outbox.log`를 사용한다.
- 이메일 인증 후 로그인할 수 있다.
- refresh cookie로 새로고침 후 세션을 복구할 수 있다.
- 로그인 후 `추천`, `옷장`, `내 취향`, `위치`, `기록`을 탐색할 수 있다.
- 계정 설정은 profile pill/menu에서 진입할 수 있다.
- local profile에서 demo user와 최소 옷장 seed가 준비된다. default/prod profile 기동만으로 seed 데이터가 생성되지 않는다.

### Prod-like runtime 기준

- `docker-compose.prod.yml`이 local/demo compose와 분리되어 있다.
- prod compose는 `smartcloset-prod` project name과 명시적인 prod volume name을 사용해 local/demo Docker volume과 충돌하지 않는다.
- prod app은 `SPRING_PROFILES_ACTIVE=prod`, `SMARTCLOSET_SEED_ENABLED=false`, Hibernate `ddl-auto=validate`, Flyway enabled로 실행된다.
- prod app은 local JWT placeholder, unsafe ddl-auto, insecure refresh/OAuth state cookie 설정으로 기동하지 않는다.
- Swagger UI와 OpenAPI JSON은 prod 기본값에서 비활성이다.
- frontend는 Vite dev server가 아니라 Nginx static image로 서빙된다.
- `scripts/prod-compose-smoke.sh`가 app health, 공개 auth provider API, disabled OpenAPI, frontend health/root HTML을 확인한다.

### MVP10 AI 옷 등록 보조 기준

- `CLOTHING_ANALYSIS_ENABLED=false` 상태에서 옷 등록 form이 manual 입력과 저장을 지원한다.
- 이미지 선택 후 `AI 후보 체크` 버튼이 보인다.
- 기능 비활성 상태의 분석 실패 안내가 manual 입력을 막지 않는다.
- 실제 OpenAI 설정을 켠 경우 `POST /api/clothes/analyze-image`가 후보값과 `fieldConfidence`, `reviewRequiredFields`, `lowConfidenceThreshold`를 반환한다.
- confidence 낮은 field가 흐림/확인 필요 상태로 표시된다.
- 사용자가 수정/확인한 값만 기존 옷 JSON API로 저장된다.
- 이미지 저장은 기존 옷 이미지 API로 분리된다.
- 데스크톱 1440px과 모바일 390px에서 preview, 버튼, 확인 필요 표시가 겹치거나 잘리지 않는다.

### 기존 기능 기준

- 미인증 password 계정 login이 차단된다.
- 같은 email/client key 또는 client key의 password login 실패가 반복되면 `LOGIN_ATTEMPT_LIMIT_EXCEEDED`로 제한된다.
- `POST /api/auth/refresh`가 refresh cookie로 access token을 재발급한다.
- 비밀번호 재설정 요청/확인이 가능하다.
- Google provider 설정이 없으면 disabled 상태가 표시된다.
- 계정 삭제가 현재 사용자 데이터와 이미지 파일을 삭제한다.
- Location에서 동네 단위 KMA catalog 검색을 사용할 수 있다.
- Recommendation에서 `CURRENT`, `MORNING`, `AFTERNOON`, `EVENING` 중 하나를 선택할 수 있다.
- 추천 결과와 History에서 위치/날씨 요약을 확인할 수 있고 내부 weather source 필드는 일반 화면에 노출하지 않는다.
- Closet에서 옷별 styleTags를 저장하고 확인할 수 있다.
- 추천 결과에서 착용 완료와 피드백 저장/clear를 사용할 수 있다.
- 옷 이미지 업로드/조회/삭제가 보호 API로 동작한다.

## 인증 기준

공개 API는 auth/account bootstrap endpoint만 허용한다. 그 외 `/api/**` endpoint는 보호 API이며 `Authorization: Bearer {accessToken}` header가 필요하다.

Access token은 JSON 응답으로 받고 frontend memory state에 저장한다. Refresh token은 HttpOnly cookie로만 전달한다.

## 후속 배포 기준

MVP10 공유 문서에는 AWS 구현을 포함하지 않는다. 후속 MVP에서 운영 배포를 진행할 때 아래 경계를 유지한다.

- `EmailSender`는 SES/SMTP 구현체로 교체 가능해야 한다. 현재 local `ConsoleEmailSender`는 token 원문을 로그가 아니라 local outbox 파일에만 남긴다.
- `ClothingImageStorage`는 S3 구현체를 추가할 수 있어야 한다.
- Cookie/CORS/OAuth redirect/base URL은 env로 바꿀 수 있어야 한다.
- AI 분석 설정과 secret은 env로만 주입해야 한다.
- `SPRING_PROFILES_ACTIVE=local`은 Docker Compose 기본값으로 유지하고, demo seed initializer는 `local`/`demo` profile과 `SMARTCLOSET_SEED_ENABLED=true` 조건에서만 활성화한다.
- Flyway migration이 깨끗한 DB schema를 생성하고 Hibernate `ddl-auto=validate`가 entity/schema drift를 검증한다.
- `prod` profile은 local JWT secret placeholder와 Hibernate `ddl-auto=update`를 허용하지 않으며, Swagger UI/API docs를 기본 비활성화한다.
- prod runtime은 `docker-compose.prod.yml`과 `frontend/Dockerfile`/`frontend/nginx.conf`를 기준으로 검증한다.
- prod runtime은 refresh cookie와 OAuth state cookie `Secure=true`를 유지해야 한다.
- future AWS 배포 profile은 별도 env와 운영 adapter bean으로 추가하며 demo seed initializer를 활성화하지 않는다.
- local Docker Compose 실행은 prod profile 추가 후에도 유지되어야 한다.
- app image는 non-root user, healthcheck, JVM memory env override를 유지해야 한다.
- 기존 local `clothing-image-data` volume은 app 시작 전 ownership helper로 보정되어야 한다.
- MySQL backup/restore는 `scripts/mysql-backup.sh`, `scripts/mysql-restore.sh` 절차를 기준으로 검증한다.

## 제외 범위 확인

MVP10 공유 문서와 데모에는 아래 기능을 포함하지 않는다.

- AWS 배포
- S3 storage 구현
- SES/SMTP 실제 발송 구현
- Secrets Manager
- CD 자동화
- Redis
- DB schema 변경
- 추천 점수/필터/tie-break 변경
- AI/GPT 옷차림 추천
- AI-generated 추천 이유
- 사용자 확인 없는 자동 저장
- 분석 결과 영속화
- 다중 이미지
- 이미지 편집/cropping/resizing/compression pipeline
- 이미지 EXIF 분석
- image moderation

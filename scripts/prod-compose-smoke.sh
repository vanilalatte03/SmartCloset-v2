#!/usr/bin/env sh
set -eu

PROJECT="${SMARTCLOSET_PROD_SMOKE_PROJECT:-smartclosetprodsmoke}"
APP_PORT="${SMARTCLOSET_PROD_SMOKE_APP_PORT:-18080}"
FRONTEND_PORT="${SMARTCLOSET_PROD_SMOKE_FRONTEND_PORT:-18081}"
ENV_FILE="$(mktemp)"

case "$PROJECT" in
  smartclosetprodsmoke*)
    ;;
  *)
    echo "SMARTCLOSET_PROD_SMOKE_PROJECT must start with smartclosetprodsmoke." >&2
    exit 2
    ;;
esac

unset COMPOSE_PROJECT_NAME

cleanup() {
  docker compose -p "$PROJECT" -f docker-compose.prod.yml --env-file "$ENV_FILE" down -v >/dev/null 2>&1 || true
  rm -f "$ENV_FILE"
}

trap cleanup EXIT INT TERM

docker compose -p "$PROJECT" -f docker-compose.prod.yml --env-file "$ENV_FILE" down -v >/dev/null 2>&1 || true

cat > "$ENV_FILE" <<EOF
MYSQL_DATABASE=smartcloset
MYSQL_USER=smartcloset
MYSQL_PASSWORD=prod-smoke-mysql-password
MYSQL_ROOT_PASSWORD=prod-smoke-root-password
SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/smartcloset?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
SPRING_DATASOURCE_USERNAME=smartcloset
SPRING_DATASOURCE_PASSWORD=prod-smoke-mysql-password
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
SPRING_FLYWAY_ENABLED=true
SPRING_FLYWAY_BASELINE_ON_MIGRATE=false
SMARTCLOSET_SEED_ENABLED=false
JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75.0
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,prometheus
MANAGEMENT_PROMETHEUS_METRICS_EXPORT_ENABLED=true
SMARTCLOSET_HEALTHCHECK_URL=http://127.0.0.1:8080/actuator/health
SPRINGDOC_API_DOCS_ENABLED=false
SPRINGDOC_SWAGGER_UI_ENABLED=false
JWT_SECRET=prod-smoke-jwt-secret-value-with-enough-length
REFRESH_TOKEN_COOKIE_NAME=smartcloset.refreshToken
REFRESH_TOKEN_COOKIE_SECURE=true
REFRESH_TOKEN_COOKIE_SAME_SITE=None
REFRESH_TOKEN_COOKIE_DOMAIN=
REFRESH_TOKEN_COOKIE_PATH=/api/auth
REFRESH_TOKEN_COOKIE_MAX_AGE=14d
REFRESH_TOKEN_TTL_DAYS=14
CORS_ALLOWED_ORIGINS=http://127.0.0.1:$FRONTEND_PORT,http://localhost:$FRONTEND_PORT
CORS_ALLOW_CREDENTIALS=true
MYSQL_DATA_VOLUME_NAME=$PROJECT-mysql-data
CLOTHING_IMAGE_DATA_VOLUME_NAME=$PROJECT-clothing-image-data
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
GOOGLE_OAUTH_REDIRECT_URI=
GOOGLE_OAUTH_CONNECT_TIMEOUT=3s
GOOGLE_OAUTH_READ_TIMEOUT=5s
FRONTEND_AUTH_CALLBACK_URL=http://127.0.0.1:$FRONTEND_PORT/auth/callback
OAUTH_STATE_COOKIE_NAME=smartcloset.oauth2State
OAUTH_STATE_COOKIE_SECURE=true
OAUTH_STATE_COOKIE_SAME_SITE=None
OAUTH_STATE_COOKIE_DOMAIN=
OAUTH_STATE_COOKIE_PATH=/api/auth/oauth2
OAUTH_STATE_COOKIE_MAX_AGE=5m
APP_PORT=$APP_PORT
FRONTEND_PORT=$FRONTEND_PORT
VITE_API_BASE_URL=http://127.0.0.1:$APP_PORT
EOF

docker compose -p "$PROJECT" -f docker-compose.prod.yml --env-file "$ENV_FILE" config --quiet
docker compose -p "$PROJECT" -f docker-compose.prod.yml --env-file "$ENV_FILE" up --build -d

for _ in $(seq 1 90); do
  if curl -fsS "http://127.0.0.1:$APP_PORT/actuator/health" | grep -q '"status":"UP"'; then
    break
  fi
  sleep 2
done

curl -fsS "http://127.0.0.1:$APP_PORT/actuator/health" | grep -q '"status":"UP"'
curl -fsS "http://127.0.0.1:$APP_PORT/api/auth/oauth2/providers" | grep -q '"data"'
if curl -fsS "http://127.0.0.1:$APP_PORT/v3/api-docs" >/dev/null 2>&1; then
  echo "prod smoke expected /v3/api-docs to be disabled" >&2
  exit 1
fi

for _ in $(seq 1 30); do
  if curl -fsS "http://127.0.0.1:$FRONTEND_PORT/healthz" | grep -q '^ok$'; then
    break
  fi
  sleep 1
done

curl -fsS "http://127.0.0.1:$FRONTEND_PORT/healthz" | grep -q '^ok$'
curl -fsS "http://127.0.0.1:$FRONTEND_PORT/" | grep -q '<div id="root">'

printf '%s\n' "prod-compose-smoke=ok app=http://127.0.0.1:$APP_PORT frontend=http://127.0.0.1:$FRONTEND_PORT"

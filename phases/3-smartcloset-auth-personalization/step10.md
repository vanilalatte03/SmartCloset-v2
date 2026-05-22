# 단계 10: sharing-verification-and-doc-sync

범위: Must-have / 3차 P0

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `README.md`
- `docs/PRD.md`
- `docs/ARCHITECTURE.md`
- `docs/FRONTEND.md`
- `docs/RECOMMENDATION_RULES.md`
- `docs/API.md`
- `docs/ERD.md`
- `docs/DEMO_SCENARIO.md`
- `docs/SHARING_GUIDE.md`
- `docs/COMMANDS.md`
- `docs/ADR.md`
- `docs/adr/`
- `.env.example`
- `docker-compose.yml`
- `archive/mvp-2/README.md`

이전 단계에서 만들어진 코드를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업
3차 MVP 구현 결과를 문서, 공유 설정, 데모 시나리오와 동기화하고 최종 검증을 수행한다. 구현 변경이 아니라 최종 contract 정합성, Docker Compose 공유, archive 최소 요약, 검색 기반 회귀 방지를 마무리하는 단계다.

## 변경 예상 파일
- `README.md`
- `docs/PRD.md`
- `docs/ARCHITECTURE.md`
- `docs/FRONTEND.md`
- `docs/RECOMMENDATION_RULES.md`
- `docs/API.md`
- `docs/ERD.md`
- `docs/DEMO_SCENARIO.md`
- `docs/SHARING_GUIDE.md`
- `docs/COMMANDS.md`
- `docs/ADR.md`
- `docs/adr/008-mvp3-authenticated-user-personalization.md`
- `.env.example`
- `docker-compose.yml`
- `archive/mvp-2/README.md`

## 구현 메모
- 공개 API와 보호 API 표를 분리해 유지한다.
- 공개 API는 `POST /api/auth/signup`, `POST /api/auth/login`만 문서화한다.
- 보호 API는 Bearer token 필요를 명시한다.
- `?userId=` query parameter 예시를 현재 문서에서 제거한다.
- 현재 사용자 전용 response DTO 예시에서 `userId`를 제거한다.
- 추천 생성은 `POST /api/recommendations`로만 문서화한다.
- today 추천 GET path가 현재 API 계약처럼 보이면 제거한다.
- 추천 이력은 `GET /api/recommendations?limit={limit}`로 문서화한다.
- 선호도 저장 방식은 `users` JSON 문자열 컬럼으로 유지한다.
- `styleTags`는 storage/display only로 문서화한다.
- `GET /api/locations`는 보호 API이며 로그인 후 위치 선택 흐름으로만 문서화한다.
- 프론트 access token 저장 위치는 `sessionStorage`로 문서화한다.
- MVP 3 전환용 Docker Compose DB reset 안내를 README/demo/sharing/commands에 유지한다.
- `.env.example`에는 실제 secret이나 API key를 넣지 않는다. `JWT_SECRET`은 로컬 placeholder만 둔다.
- `archive/`에는 과거 MVP 전체 문서 복사본이 아니라 최소 요약만 둔다.

## 검증 절차
```bash
git diff --check
! rg -n 'GET /api/recommendations/(today)' README.md docs/PRD.md docs/API.md docs/ARCHITECTURE.md docs/FRONTEND.md docs/RECOMMENDATION_RULES.md docs/ERD.md docs/DEMO_SCENARIO.md docs/SHARING_GUIDE.md docs/COMMANDS.md AGENTS.md .agents/skills/smartcloset-backend/SKILL.md
! rg -n 'POST /api/recommendations\\?userId|/api/clothes\\?userId|/api/users/location\\?userId' README.md docs/PRD.md docs/API.md docs/ARCHITECTURE.md docs/FRONTEND.md docs/RECOMMENDATION_RULES.md docs/ERD.md docs/DEMO_SCENARIO.md docs/SHARING_GUIDE.md docs/COMMANDS.md AGENTS.md .agents/skills/smartcloset-backend/SKILL.md
rg -n 'POST /api/recommendations' README.md docs/PRD.md docs/API.md docs/ARCHITECTURE.md docs/FRONTEND.md docs/RECOMMENDATION_RULES.md docs/ERD.md docs/DEMO_SCENARIO.md docs/SHARING_GUIDE.md docs/COMMANDS.md AGENTS.md .agents/skills/smartcloset-backend/SKILL.md
rg -n 'preferenceScore|preferred_colors_json|preferred_materials_json|style_tags_json' README.md docs/PRD.md docs/API.md docs/ARCHITECTURE.md docs/FRONTEND.md docs/RECOMMENDATION_RULES.md docs/ERD.md docs/DEMO_SCENARIO.md docs/SHARING_GUIDE.md docs/COMMANDS.md AGENTS.md .agents/skills/smartcloset-backend/SKILL.md
rg -n 'GET /api/locations' README.md docs/API.md docs/FRONTEND.md docs/DEMO_SCENARIO.md docs/SHARING_GUIDE.md
rg -n 'sessionStorage' README.md docs/PRD.md docs/API.md docs/ARCHITECTURE.md docs/FRONTEND.md docs/RECOMMENDATION_RULES.md docs/ERD.md docs/DEMO_SCENARIO.md docs/SHARING_GUIDE.md docs/COMMANDS.md AGENTS.md .agents/skills/smartcloset-backend/SKILL.md
rg -n 'docker compose down -v' README.md docs/PRD.md docs/API.md docs/ARCHITECTURE.md docs/FRONTEND.md docs/RECOMMENDATION_RULES.md docs/ERD.md docs/DEMO_SCENARIO.md docs/SHARING_GUIDE.md docs/COMMANDS.md
./gradlew test
./gradlew build
(cd frontend && npm run build)
docker compose config
docker compose down -v
test -f .env || cp .env.example .env
docker compose up --build -d
curl -fsS http://localhost:8080/v3/api-docs >/dev/null
docker compose down
```

## 인수 기준
- README와 docs가 3차 API/프론트/추천/공유 기준과 일치한다.
- 현재 문서에는 공개 `?userId=` API 예시가 없다.
- 현재 문서에는 today 추천 GET 경로가 API 계약처럼 남아 있지 않다.
- `preferenceScore`와 선호도 JSON 컬럼 기준이 모든 관련 문서에 반영되어 있다.
- `sessionStorage`, `JWT_SECRET`, JWT 2시간 만료, 공개 auth 2종 기준이 문서화되어 있다.
- Docker Compose로 `mysql`, `app`, `frontend`가 함께 실행 가능하다.
- 서비스키 없이 fallback 추천 데모가 가능하다.
- 최종 Gradle test/build, frontend build, compose config가 통과한다.

## 금지사항
- 실제 API key, token, password, private key를 문서나 코드에 넣지 마라. 이유: 민감정보는 커밋 금지다.
- `archive/`에 현재 문서 전체 복사본을 추가하지 마라. 이유: archive는 과거 MVP 최소 요약만 둔다.
- AWS 배포나 CD 자동화를 문서상 3차 제공 기능으로 쓰지 마라. 이유: 3차 제외 범위다.
- refresh token, 소셜 로그인, 이메일 인증, 비밀번호 재설정을 후속이 아니라 현재 기능처럼 문서화하지 마라. 이유: 3차 제외 범위다.

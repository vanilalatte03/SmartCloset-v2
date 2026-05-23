# 단계 7: demo-sharing-doc-sync

범위: Must-have / MVP4 P0

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
- `docs/design/mvp4/README.md`
- `.env.example`
- `docker-compose.yml`
- `frontend/`

이전 단계에서 만들어진 P0 코드와 UX를 꼼꼼히 확인한 뒤 작업하라.

## 작업
MVP4 P0 구현 결과를 문서, 데모 시나리오, 공유 가이드, 실행 명령과 동기화하고 P0 release candidate 검증을 수행한다. 구현 변경이 아니라 contract 정합성, Docker Compose 공유, 첫 추천 데모 검증을 마무리하는 단계다.

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
- `docs/adr/009-mvp4-usable-ux.md`
- `.env.example`
- `docker-compose.yml`

## 구현 메모
- README와 docs가 MVP4 P0 완료 기준, 남은 P1 polish 범위, 실제 구현을 일치하게 설명하는지 확인한다.
- 공개 API는 auth 2종만 문서화한다.
- 보호 API는 Bearer token 필요를 명시한다.
- `GET /api/weather/current`는 보호 API이며 추천 결과/이력 생성이 아니라 날씨 요약임을 문서화한다.
- `POST /api/recommendations`, `GET /api/recommendations?limit={limit}`, `PATCH /api/recommendations/{recommendationId}/worn` 기준을 유지한다.
- today 추천 GET path가 현재 API 계약처럼 보이면 제거한다.
- 공개 `?userId=` query parameter 예시를 제거한다.
- 현재 사용자 전용 response DTO 예시에 `userId`가 없어야 한다.
- 프론트 access token 저장 위치는 `sessionStorage`로 유지한다.
- MVP4 데모 전 Docker Compose DB reset 안내 `docker compose down -v`를 유지한다.
- 공유 방식은 Docker Compose 기준이며 AWS/PWA/native app을 현재 제공 기능처럼 쓰지 않는다.
- 실제 API key, token, password, private key가 문서나 코드에 들어가지 않았는지 확인한다.

## 검증 절차
```bash
git diff --check
! rg -n 'T[B]D|MVP4 기능 범위는 아직 확[정]|MVP4 작성 메[모]' README.md docs/PRD.md docs/API.md docs/ARCHITECTURE.md docs/FRONTEND.md docs/RECOMMENDATION_RULES.md docs/ERD.md docs/DEMO_SCENARIO.md docs/SHARING_GUIDE.md docs/COMMANDS.md
! rg -n 'GET /api/recommendations/(today)' README.md docs/PRD.md docs/API.md docs/ARCHITECTURE.md docs/FRONTEND.md docs/RECOMMENDATION_RULES.md docs/ERD.md docs/DEMO_SCENARIO.md docs/SHARING_GUIDE.md docs/COMMANDS.md AGENTS.md .agents/skills/smartcloset-backend/SKILL.md frontend/src
! rg -n -F -e 'POST /api/recommendations?userId' -e '/api/clothes?userId' -e '/api/users/location?userId' README.md docs/PRD.md docs/API.md docs/ARCHITECTURE.md docs/FRONTEND.md docs/RECOMMENDATION_RULES.md docs/ERD.md docs/DEMO_SCENARIO.md docs/SHARING_GUIDE.md docs/COMMANDS.md AGENTS.md .agents/skills/smartcloset-backend/SKILL.md frontend/src
rg -n 'GET /api/weather/current' README.md docs/PRD.md docs/API.md docs/ARCHITECTURE.md docs/FRONTEND.md docs/DEMO_SCENARIO.md docs/SHARING_GUIDE.md docs/COMMANDS.md AGENTS.md .agents/skills/smartcloset-backend/SKILL.md
rg -n '2분 안에 첫 추천 성공|오늘 입기 좋은 이유|하단 탭|색상 swatch|소재 chip' README.md docs/PRD.md docs/FRONTEND.md docs/DEMO_SCENARIO.md docs/SHARING_GUIDE.md
rg -n 'sessionStorage|preferenceScore|styleTags' README.md docs/PRD.md docs/API.md docs/ARCHITECTURE.md docs/FRONTEND.md docs/RECOMMENDATION_RULES.md docs/ERD.md docs/DEMO_SCENARIO.md docs/SHARING_GUIDE.md docs/COMMANDS.md AGENTS.md .agents/skills/smartcloset-backend/SKILL.md
./gradlew test
./gradlew build
(cd frontend && npm run build)
docker compose config
docker compose down -v
test -f .env || cp .env.example .env
docker compose up --build -d
curl -fsS http://localhost:8080/v3/api-docs >/dev/null
curl -fsS http://localhost:5173 >/dev/null
docker compose down
```

## 인수 기준
- README와 docs가 MVP4 P0 구현 결과와 남은 P1 범위를 구분해서 설명한다.
- 현재 문서와 frontend 코드에는 공개 `?userId=` API 예시가 없다.
- today 추천 GET 경로가 API 계약처럼 남아 있지 않다.
- `GET /api/weather/current`가 보호 API이자 날씨 요약 전용 API로 문서화되어 있다.
- `sessionStorage`, 공개 auth 2종, 보호 API Bearer token 기준이 문서화되어 있다.
- 추천 결과는 이유 중심, 실패는 한국어 CTA, enum은 한국어 라벨/swatch/chip으로 설명되어 있다.
- Docker Compose로 `mysql`, `app`, `frontend`가 함께 실행 가능하다.
- 서비스키 없이 fallback 날씨로 첫 추천 데모가 가능하다.
- 최종 Gradle test/build, frontend build, compose config, compose smoke가 통과한다.
- Step 8, 9, 10이 남아 있어도 P0 release cut의 완료/미완료 기준이 문서에서 분명하다.

## 금지사항
- 실제 API key, token, password, private key를 문서나 코드에 넣지 마라. 이유: 민감정보는 커밋 금지다.
- `archive/`에 현재 문서 전체 복사본을 추가하지 마라. 이유: archive는 과거 MVP 최소 요약만 둔다.
- AWS 배포, PWA, native app을 MVP4 제공 기능처럼 쓰지 마라. 이유: MVP4 공유 방식은 Docker Compose다.
- refresh token, 소셜 로그인, 이메일 인증, 비밀번호 재설정을 현재 기능처럼 문서화하지 마라. 이유: MVP4 제외 범위다.
- 선호도 정규화나 `styleTags` scoring을 후속 후보가 아니라 현재 구현으로 문서화하지 마라. 이유: 현재 baseline과 충돌한다.

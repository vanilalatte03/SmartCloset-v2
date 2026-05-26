# SmartCloset

현재 문서 기준은 **MVP7: 위치/날씨 신뢰도 MVP**입니다. MVP7은 MVP6 추천 피드백/개인화 완료 상태 위에 동네 단위 위치 선택, 브라우저 현재 위치 기반 후보 찾기, KMA 예보 기준 시각 표시, 추천 결과의 위치/날씨 source snapshot 저장을 추가하는 단계입니다.

현재 코드 출발점은 MVP6 추천 피드백/개인화 구현 완료 상태입니다. MVP7 구현 source of truth는 루트 `README.md`와 `docs/` 아래 문서, 그리고 ADR-012입니다.

## 현재 Baseline

- Spring Boot 4.0.6, Java 21, MySQL, React+Vite+TypeScript SPA를 사용한다.
- 공개 API는 `POST /api/auth/signup`, `POST /api/auth/login`뿐이다.
- 그 외 API는 `Authorization: Bearer {accessToken}` header를 요구한다.
- 공개 HTTP API는 `userId` query parameter를 받지 않는다.
- 현재 사용자 전용 response DTO는 `userId`를 노출하지 않는다.
- 사용자별 옷장, 위치, 선호도, 추천 이력, 착용 이력, 추천 피드백을 분리한다.
- 추천 생성은 `POST /api/recommendations`만 사용한다.
- 추천 이력은 `GET /api/recommendations?limit={limit}`이며 기본 20, 최소 1, 최대 50, 최신순이다.
- 현재 날씨 요약은 `GET /api/weather/current` 보호 API로 조회한다.
- 옷 이미지 업로드/교체/조회/삭제는 MVP5 보호 API 계약을 유지한다.
- 추천 상황, 옷별 `styleTags`, 추천 피드백, 최근 피드백 기반 `preferenceScore`는 MVP6 계약을 유지한다.
- Docker Compose 공유 방식을 유지한다.

## MVP7 목표

사용자가 "왜 이 위치와 날씨 기준으로 추천했지?"를 추천 결과와 이력에서 확인할 수 있게 만든다.

### 포함 범위

- KMA 격자 엑셀 기반 내부 읍/면/동 위치 catalog
- `GET /api/locations?keyword={keyword}`의 동네 단위 검색 확장
- 브라우저 Geolocation API 좌표를 서버 `POST /api/locations/resolve`로 전달해 KMA 격자와 후보 위치를 찾는 흐름
- 위경도 -> KMA 격자 변환 로직
- 위치 저장 source: `MANUAL_SEARCH`, `BROWSER_GEOLOCATION`
- 추천 요청의 예보 시간대 선택: `CURRENT`, `MORNING`, `AFTERNOON`, `EVENING`
- `WeatherResponse`의 위치 snapshot과 weather source metadata
- 추천 결과의 위치/날씨 source snapshot 저장과 이력 표시
- KMA 사용 여부, fallback 여부, KMA base date/time, forecast date/time 표시
- MVP7 phase 문서와 docs-check 규칙 작성

### 제외 범위

- 외부 지도/주소 검색 API
- raw KMA 응답 JSON 저장
- KMA `getVilageFcst` 외 weather API
- GPS 좌표 원문 DB 저장
- AI/GPT 추천
- AI 자동 태깅
- refresh token, social login, email verification, password reset
- Redis
- AWS 배포와 CD 자동화
- S3/CDN 이미지 hosting

## API 요약

공개 API:

- `POST /api/auth/signup`
- `POST /api/auth/login`

보호 API:

- `GET /api/users/me`
- `GET /api/locations?keyword={keyword}`
- `POST /api/locations/resolve`
- `GET /api/users/me/location`
- `PUT /api/users/me/location`
- `GET /api/users/me/preferences`
- `PUT /api/users/me/preferences`
- `GET /api/weather/current`
- `POST /api/clothes`
- `GET /api/clothes`
- `GET /api/clothes/{clothingId}`
- `PUT /api/clothes/{clothingId}`
- `PATCH /api/clothes/{clothingId}/archive`
- `PUT /api/clothes/{clothingId}/image`
- `GET /api/clothes/{clothingId}/image`
- `DELETE /api/clothes/{clothingId}/image`
- `POST /api/recommendations`
- `GET /api/recommendations?limit={limit}`
- `PATCH /api/recommendations/{recommendationId}/worn`
- `PUT /api/recommendations/{recommendationId}/feedback`

MVP7 API 변경:

- `LocationOptionResponse`는 `fullName`, `region1`, `region2`, `region3`, nullable `latitude`, nullable `longitude`를 포함한다.
- `POST /api/locations/resolve`는 `{ "latitude": 37.66, "longitude": 126.77 }`를 받아 KMA grid와 가까운 위치 후보를 반환한다.
- `PUT /api/users/me/location`은 기존 `locationCode`와 optional `source`를 받는다.
- `POST /api/recommendations`는 optional `forecastPeriod`를 받는다. 누락 시 `CURRENT`다.
- `WeatherResponse`는 기존 날씨 필드에 `location`과 `source`를 포함한다.
- `RecommendationResponse.weather`도 추천 생성 당시 저장된 location/source snapshot을 반환한다.

## 위치/날씨 신뢰 정책

MVP7의 신뢰 표시는 원본 API 응답을 노출하는 기능이 아니라, 추천에 사용된 핵심 판단 근거를 작고 안정적으로 보여주는 기능이다.

- 위치 검색은 프로젝트 내부 KMA 행정구역 catalog를 사용한다.
- 외부 주소/지도 API는 사용하지 않는다.
- 브라우저 좌표는 사용자가 위치 후보를 찾는 데만 쓰고 DB에 원문 좌표를 저장하지 않는다.
- 사용자가 선택한 catalog 위치 code/name/grid/source만 사용자 위치로 저장한다.
- KMA weather provider는 `getVilageFcst`만 사용한다.
- KMA 실패 또는 서비스키 미설정 시 fallback weather를 사용할 수 있다.
- 추천 결과에는 사용된 위치, grid, location source, KMA/fallback 여부, base/forecast 시각을 snapshot으로 저장한다.

## 추천 규칙

추천은 계속 AI/GPT가 아닌 설명 가능하고 테스트 가능한 규칙 기반 추천이다.

- 총점은 100점이며 기존 score field를 유지한다.
- `weatherScore=35`, `colorScore=25`, `wearHistoryScore=20`, `recommendationHistoryScore=10`, `preferenceScore=10`이다.
- MVP6의 상황, styleTags, 최근 피드백 기반 `preferenceScore` 계약은 유지한다.
- MVP7의 `forecastPeriod`는 어떤 예보 시각의 날씨를 추천 입력으로 사용할지 결정한다.
- 위치/source snapshot은 추천 점수 항목을 새로 만들지 않고, 추천 근거 표시와 이력 신뢰도를 위해 저장한다.
- 이미지 존재 여부는 계속 추천 점수, 후보 필터링, 추천 이유에 영향을 주지 않는다.

상세 기준은 `docs/RECOMMENDATION_RULES.md`를 따른다.

## 실행

로컬 백엔드:

```bash
./gradlew bootRun
```

로컬 프론트엔드:

```bash
cd frontend
npm run dev
```

Docker Compose:

```bash
test -f .env || cp .env.example .env
docker compose down -v
docker compose up --build
```

## 검증 명령

```bash
git diff --check
./gradlew test
./gradlew build
(cd frontend && npm run build)
docker compose config --quiet
python3 scripts/checks.py --docs-check-config phases/7-smartcloset-location-weather-trust/docs-checks.json --docs-check
```

Docker Compose smoke:

```bash
docker compose down -v
test -f .env || cp .env.example .env
docker compose up --build -d
curl -fsS http://localhost:8080/v3/api-docs >/dev/null
curl -fsS http://localhost:5173 >/dev/null
docker compose down
```

## 문서 기준

| 영역 | 문서 |
| --- | --- |
| 제품 목표와 MVP 범위 | `docs/PRD.md` |
| HTTP API와 DTO | `docs/API.md` |
| 추천 규칙과 점수 | `docs/RECOMMENDATION_RULES.md` |
| 백엔드 구조와 트랜잭션 | `docs/ARCHITECTURE.md` |
| DB schema | `docs/ERD.md` |
| 프론트 타입과 UX | `docs/FRONTEND.md` |
| 데모 시나리오 | `docs/DEMO_SCENARIO.md` |
| Docker Compose 공유 | `docs/SHARING_GUIDE.md` |
| 명령 모음 | `docs/COMMANDS.md` |
| MVP 변경 체크리스트 | `docs/MVP_CHANGE_CHECKLIST.md` |
| 결정 기록 | `docs/ADR.md`, `docs/adr/` |

## Archive

완료된 MVP 문맥은 `archive/` 아래의 최소 요약으로만 유지합니다. 구현 기준은 현재 `README.md`와 `docs/` 아래 문서입니다.

- MVP6 archive: `archive/mvp-6/README.md`
- MVP6 phase 기록: `phases/6-smartcloset-feedback-personalization/README.md`

# SmartCloset

현재 문서 기준은 **MVP6: 추천 피드백/개인화 MVP**입니다. MVP6는 MVP5 옷 이미지 업로드 완료 상태 위에 추천 상황 선택, 추천 피드백 저장, 최근 피드백 기반 개인화, 옷별 `styleTags` 점수 반영, 추천 이력의 착용/피드백 상태 표시를 추가하는 단계입니다.

현재 코드 baseline은 MVP6 추천 피드백/개인화 구현 완료 상태입니다. 현재 구현 source of truth는 루트 `README.md`와 `docs/` 아래 문서입니다.

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
- Docker Compose 공유 방식을 유지한다.

## MVP6 목표

사용자가 추천을 받은 뒤 실제 착용 경험을 남기고, 다음 추천이 그 피드백과 사용 상황을 반영하도록 만든다.

### 포함 범위

- 추천 상황 선택: 출근, 캐주얼, 운동, 데이트, 격식
- 추천별 피드백 저장: 마음에 들어요, 별로예요, 추웠어요, 더웠어요
- 기존 착용 완료 저장 유지
- 최근 14일 피드백을 `preferenceScore`에 반영
- 옷별 `styleTags` 저장과 추천 점수 반영
- 상황별 style tag 매핑표 고정
- 추천 이력에서 상황, 착용 여부, 착용 시각, 피드백을 한눈에 표시
- MVP6 phase 문서와 docs-check 규칙 작성

### 제외 범위

- AI/GPT 추천
- AI 자동 태깅
- 피드백 이벤트 로그 분석 플랫폼
- preference normalization table 분리
- 쇼핑 추천
- refresh token, social login, email verification, password reset
- Redis
- 외부 지도/주소 API
- AWS 배포와 CD 자동화
- S3/CDN 이미지 hosting

## API 요약

공개 API:

- `POST /api/auth/signup`
- `POST /api/auth/login`

보호 API:

- `GET /api/users/me`
- `GET /api/locations?keyword={keyword}`
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

MVP6 API 변경:

- `ClothingRequest`와 `ClothingResponse`는 `styleTags: string[]`를 포함한다.
- `POST /api/recommendations`는 선택 body `{ "situation": "WORK" }`를 받을 수 있다.
- 추천 상황 누락 또는 body 없음은 `CASUAL`로 처리한다.
- `RecommendationResponse`는 `situation`, `wornAt`, `feedback`을 포함한다.
- `PUT /api/recommendations/{recommendationId}/feedback`는 추천별 최신 피드백 상태를 전체 교체한다.

## 추천 규칙

추천은 계속 AI/GPT가 아닌 설명 가능하고 테스트 가능한 규칙 기반 추천이다.

- 총점은 100점이며 기존 score field를 유지한다.
- `weatherScore=35`, `colorScore=25`, `wearHistoryScore=20`, `recommendationHistoryScore=10`, `preferenceScore=10`이다.
- MVP6에서는 `preferenceScore` 내부에 색상, 소재, style tag, 최근 피드백 보정을 함께 반영한다.
- `styleTags` 비교는 trim 후 비교하고 ASCII는 case-insensitive로 처리한다.
- 최근 피드백 window는 14일이다.
- 이미지 존재 여부는 계속 추천 점수, 후보 필터링, 추천 이유에 영향을 주지 않는다.

상세 기준은 `docs/RECOMMENDATION_RULES.md`를 따른다.

## 피드백 정책

추천 피드백은 이벤트 로그가 아니라 추천 결과별 최신 상태 snapshot으로 저장한다.

```json
{
  "sentiment": "LIKED",
  "thermal": "TOO_COLD"
}
```

- `sentiment`: `LIKED`, `DISLIKED`, `null`
- `thermal`: `TOO_COLD`, `TOO_HOT`, `null`
- PUT은 전체 교체다.
- 누락 필드는 `null`로 간주한다.
- `{}` 또는 `{ "sentiment": null, "thermal": null }`은 피드백 전체 clear다.

## 이미지 정책

MVP5 이미지 정책은 유지한다.

- 옷 1개당 이미지 1장만 지원한다.
- 이미지 업로드/교체는 `PUT /api/clothes/{clothingId}/image`다.
- 이미지 조회는 `GET /api/clothes/{clothingId}/image`다.
- 이미지 삭제는 `DELETE /api/clothes/{clothingId}/image`다.
- 이미지 API는 모두 보호 API다.
- 파일 bytes는 DB가 아니라 로컬 파일 시스템 또는 Docker Compose volume에 저장한다.
- DB에는 `clothing_items` 이미지 메타데이터만 둔다.
- 허용 파일은 5MB 이하 jpg/jpeg/png/webp다.

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
python3 scripts/checks.py --docs-check-config phases/6-smartcloset-feedback-personalization/docs-checks.json --docs-check
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

- MVP5 archive: `archive/mvp-5/README.md`
- MVP5 phase 기록: `phases/5-smartcloset-clothing-images/README.md`

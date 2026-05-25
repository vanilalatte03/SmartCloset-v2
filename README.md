# SmartCloset

SmartCloset은 사용자의 옷장 데이터와 사용자별 위치 날씨를 기반으로, 입을 수 있는 옷 후보를 필터링하고 색상 조합, 최근 이력, 선호 색상/소재를 점수화해 설명 가능한 코디를 추천하는 반응형 웹 서비스입니다.

현재 문서 기준은 **MVP5: 옷 이미지 업로드 MVP**입니다. MVP5는 MVP4 실사용 UX 위에 옷별 이미지 1장을 업로드하고, 옷 목록과 추천 결과에서 썸네일로 확인할 수 있게 만드는 단계입니다.

## 현재 Baseline

- Spring Boot 4.0.6, Java 21, MySQL
- React + Vite + TypeScript SPA
- Spring Security + JWT Bearer access token
- 공개 API는 `POST /api/auth/signup`, `POST /api/auth/login`만 허용
- 보호 API는 `Authorization: Bearer {accessToken}` 필요
- 프론트 access token 저장 위치는 `sessionStorage`
- 사용자별 옷장, 위치, 선호도, 추천 이력, 착용 이력 분리
- KMA `getVilageFcst` JSON 기반 weather provider와 fallback weather
- 규칙 기반 추천 점수 100점 체계와 `preferenceScore`
- Docker Compose 공유 방식과 이미지 저장 volume

## MVP5 목표

MVP5의 목표는 사용자가 텍스트와 enum만 보던 옷장을 실제 옷 이미지 중심으로 확인하고, 추천 결과에서도 어떤 옷을 입을지 즉시 알아볼 수 있게 하는 것입니다.

### 포함 범위

- 옷 1개당 이미지 1장 업로드
- 기존 옷 등록/수정 JSON API 유지
- 별도 보호 이미지 API로 업로드, 교체, 삭제, 조회
- 옷 목록, 옷 상세, 추천 결과, 추천 이력에 썸네일 표시
- Docker Compose 기준 로컬 파일 또는 볼륨 저장
- 파일 크기, 확장자, MIME type 검증
- 이미지가 없는 옷은 기존 카테고리 glyph, 색상 swatch, 소재 chip으로 fallback

### 제외 범위

- AI 자동 태깅
- AI/GPT 추천
- 다중 이미지 업로드
- 이미지 편집, 크롭, 리사이즈 파이프라인
- S3, CDN, 외부 image hosting
- 이미지 기반 추천 점수 변경
- refresh token, 소셜 로그인, 이메일 인증, 비밀번호 재설정
- 외부 지도/주소 API
- Redis
- AWS 배포와 CD 자동화

## API 요약

공개 API:

- `POST /api/auth/signup`
- `POST /api/auth/login`

기존 보호 API:

- `GET /api/users/me`
- `GET /api/locations?keyword={keyword}`
- `GET /api/users/me/location`
- `PUT /api/users/me/location`
- `GET /api/users/me/preferences`
- `PUT /api/users/me/preferences`
- `GET /api/weather/current`
- `GET /api/clothes`
- `POST /api/clothes`
- `GET /api/clothes/{clothingId}`
- `PUT /api/clothes/{clothingId}`
- `PATCH /api/clothes/{clothingId}/archive`
- `POST /api/recommendations`
- `GET /api/recommendations?limit={limit}`
- `PATCH /api/recommendations/{recommendationId}/worn`

MVP5에서 추가할 보호 API:

- `PUT /api/clothes/{clothingId}/image`
- `GET /api/clothes/{clothingId}/image`
- `DELETE /api/clothes/{clothingId}/image`

이미지 API도 현재 인증 사용자 소유 옷에만 접근할 수 있습니다. 공개 `userId` query parameter는 사용하지 않습니다.

## 이미지 정책

| 항목 | MVP5 기준 |
| --- | --- |
| 개수 | 옷 1개당 최대 1장 |
| 최대 크기 | 5MB |
| 허용 확장자 | `.jpg`, `.jpeg`, `.png`, `.webp` |
| 허용 MIME type | `image/jpeg`, `image/png`, `image/webp` |
| 저장 파일명 | UUID 기반 서버 생성 이름 |
| 원본 파일명 | 저장 경로에 사용하지 않음 |
| 접근 방식 | 보호 API에서 인증/소유권 확인 후 bytes 반환 |
| Compose 저장 경로 | `CLOTHING_IMAGE_STORAGE_DIR=/data/smartcloset/clothing-images` |

이미지 URL은 DTO에 `/api/clothes/{clothingId}/image` 형태로 노출합니다. 프론트는 Authorization header가 필요하므로 일반 `<img src>` 직접 참조 대신 blob fetch 후 object URL을 사용합니다.

## 추천 규칙

MVP5는 추천 점수와 추천 이유를 변경하지 않습니다.

- 추천 생성은 `POST /api/recommendations`만 사용합니다.
- today 추천 GET 경로는 사용하지 않습니다.
- 현재 날씨 요약은 `GET /api/weather/current`이며 추천 결과를 생성하거나 저장하지 않습니다.
- `styleTags`는 저장, 조회, 표시만 하고 추천 점수와 추천 이유에는 반영하지 않습니다.
- 이미지 존재 여부는 `weatherScore`, `colorScore`, `wearHistoryScore`, `recommendationHistoryScore`, `preferenceScore`에 영향을 주지 않습니다.

## 실행

개발 전 준비:

```bash
python3 -m pip install -r requirements-dev.txt
git config core.hooksPath .githooks
```

백엔드:

```bash
./gradlew bootRun
```

프론트엔드:

```bash
cd frontend
npm run dev
```

Docker Compose 공유 실행:

```bash
test -f .env || cp .env.example .env
docker compose down -v
docker compose up --build
```

Docker Compose는 `clothing-image-data` volume을 app container의 `/data/smartcloset/clothing-images`에 연결합니다. `docker compose down` 후 재시작해도 이미지는 유지되고, DB와 이미지 volume을 모두 초기화하려면 `docker compose down -v`를 사용합니다.

접속 경로:

- Frontend: http://localhost:5173
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## 검증 명령

```bash
git diff --check
./gradlew test
./gradlew build
(cd frontend && npm run build)
docker compose config
```

MVP5 최종 공유 검증에서는 Docker Compose 실행 후 회원가입, 옷 등록, 이미지 업로드, 추천 생성, 썸네일 표시를 브라우저에서 확인합니다.

## 주요 문서

- PRD: `docs/PRD.md`
- API: `docs/API.md`
- ERD: `docs/ERD.md`
- 아키텍처: `docs/ARCHITECTURE.md`
- 프론트엔드: `docs/FRONTEND.md`
- 추천 규칙: `docs/RECOMMENDATION_RULES.md`
- 데모 시나리오: `docs/DEMO_SCENARIO.md`
- 공유 가이드: `docs/SHARING_GUIDE.md`
- 명령: `docs/COMMANDS.md`
- ADR: `docs/ADR.md`

## Archive

과거 MVP 문맥은 `archive/` 아래 최소 요약으로 보관합니다. `archive/`는 구현 source of truth가 아닙니다.

- `archive/mvp-1`
- `archive/mvp-1-5`
- `archive/mvp-2`
- `archive/mvp-3`
- `archive/mvp-4`

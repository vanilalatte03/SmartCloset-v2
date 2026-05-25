# Demo Scenario: SmartCloset MVP5

## 데모 목표

Docker Compose로 SmartCloset 백엔드, MySQL, React 프론트엔드를 실행한 뒤 브라우저에서 옷 이미지 업로드, 교체, 삭제, 추천 결과 썸네일 표시를 확인한다.

MVP5 데모의 핵심은 추천 알고리즘 변경이 아니라, 사용자가 실제 옷 이미지를 옷장과 추천 결과에서 자연스럽게 확인할 수 있는지다.

## MVP5 데모 범위

- 회원가입/로그인
- 신규/빈 계정 기본 옷 프리셋 자동 생성
- `sessionStorage` access token 저장과 세션 복구
- 옷 등록
- 옷 이미지 업로드
- 옷 이미지 교체
- 옷 이미지 삭제
- 옷 목록 썸네일 표시
- 추천 생성
- 추천 결과 썸네일 표시
- 추천 이력 썸네일 표시
- Docker Compose app 재시작 후 이미지 유지 확인

AI 자동 태깅, AI/GPT 추천, 이미지 기반 점수 변경, 다중 이미지, S3/CDN, 이미지 편집, refresh token, 소셜 로그인은 데모 범위가 아니다.

## 데모 전제

- Docker Compose 실행 완료
- Frontend 접속 가능: http://localhost:5173
- Swagger UI 접속 가능: http://localhost:8080/swagger-ui/index.html
- 신규 사용자는 기본 위치 서울특별시 `SEOUL`, `nx=60`, `ny=127`
- 신규 가입자는 기본 옷 5개와 상품컷 이미지가 자동으로 생성된다.
- 기존 계정도 로그인 시 옷이 0개이면 같은 기본 옷 5개를 한 번만 받는다.
- 프론트 access token 저장 위치는 `sessionStorage`
- 서비스키 없이 실행하면 fallback 날씨 `temperature=12`, `weatherType=CLOUDY`, `rainy=false`, `windy=false`를 사용한다.
- fallback 날씨는 OUTER 필수 조건이므로 첫 추천 성공 데모에는 TOP, BOTTOM, OUTER가 각각 1개 이상 필요하다.

## DB와 이미지 볼륨 초기화

로컬 Docker Compose DB와 이미지 볼륨은 기존 schema/file 상태와 충돌할 수 있으므로 데모 전 초기화를 권장한다.

```bash
docker compose down -v
docker compose up --build
```

MVP5 구현 후에는 이미지 저장 volume도 Compose volume으로 관리한다.

## 환경변수

서비스키 없이 실행하면 fallback 날씨를 사용한다.

```env
JWT_SECRET=change-me-local-development-only
KMA_SERVICE_KEY=
KMA_BASE_URL=http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0
WEATHER_FALLBACK_ENABLED=true
CLOTHING_IMAGE_STORAGE_DIR=/data/smartcloset/clothing-images
CLOTHING_IMAGE_MAX_SIZE_BYTES=5242880
VITE_API_BASE_URL=http://localhost:8080
```

실제 서비스키와 운영 JWT secret은 문서, 코드, 커밋에 남기지 않는다.

## React 앱 MVP5 데모 시나리오

### 1. 회원가입 또는 로그인

API:

```http
POST /api/auth/signup
POST /api/auth/login
GET /api/users/me
```

확인 포인트:

- 로그인 성공 후 access token이 `sessionStorage`에 저장된다.
- 새로고침 후 로그인 상태가 복구된다.
- 로그인 후 기본 view는 `오늘`이다.
- 신규 가입 직후 Closet view에는 기본 옷 프리셋 5개가 썸네일과 함께 보인다.
- 기본 프리셋은 화이트 반팔 티셔츠, 블랙 반팔 티셔츠, 흑청 데님 팬츠, 진청 데님 팬츠, 블랙 가디건이다.

### 2. 기본 옷 프리셋 확인

API:

```http
GET /api/clothes
GET /api/clothes/{clothingId}/image
```

확인 포인트:

- 기본 옷 5개는 현재 사용자 소유 옷으로 반환된다.
- 각 기본 옷의 `image.contentType`은 `image/jpeg`이다.
- 프리셋 이미지도 보호 API를 통해 Authorization header로 조회된다.
- 프리셋 이미지를 삭제하거나 교체해도 다른 사용자에게 영향이 없다.

### 3. 옷 등록

API:

```http
POST /api/clothes
GET /api/clothes
```

TOP 요청 예시:

```json
{
  "name": "그레이 후드",
  "category": "TOP",
  "color": "GRAY",
  "material": "COTTON",
  "minTemperature": 5,
  "maxTemperature": 18,
  "rainSuitable": false
}
```

확인 포인트:

- 옷 등록은 여전히 JSON API를 사용한다.
- 등록 직후 `image`는 `null`일 수 있다.
- 이미지가 없는 카드는 기존 fallback visual을 표시한다.

### 4. 옷 이미지 업로드

API:

```http
PUT /api/clothes/{clothingId}/image
```

요청:

- `multipart/form-data`
- part name: `image`
- jpg/jpeg/png/webp, 최대 5MB

확인 포인트:

- 업로드 성공 후 `ClothingResponse.image.url`이 생긴다.
- 옷 카드에 썸네일이 표시된다.
- 이미지 조회는 보호 API이므로 프론트가 Authorization header로 blob을 가져온다.
- 새로고침 후에도 썸네일이 다시 표시된다.

### 5. 이미지 교체

API:

```http
PUT /api/clothes/{clothingId}/image
```

확인 포인트:

- 새 파일 업로드 후 같은 옷 카드의 썸네일이 바뀐다.
- 기존 파일은 더 이상 노출되지 않는다.
- 옷 이름, 카테고리, 색상, 소재는 이미지 교체만으로 바뀌지 않는다.

### 6. 이미지 삭제

API:

```http
DELETE /api/clothes/{clothingId}/image
```

확인 포인트:

- 삭제 후 `image`가 `null`이다.
- 카드가 fallback visual로 돌아간다.
- 이미 이미지가 없는 상태에서 다시 삭제해도 성공한다.

### 7. 추천 결과 썸네일 확인

API:

```http
POST /api/recommendations
```

확인 포인트:

- 추천 결과의 TOP/BOTTOM/OUTER item에 image metadata가 포함된다.
- 이미지가 있는 옷은 썸네일로 표시된다.
- 이미지가 없는 옷은 fallback visual로 표시된다.
- 추천 점수와 추천 이유는 이미지 여부와 관계없이 기존 규칙을 따른다.

### 8. 추천 이력 썸네일 확인

API:

```http
GET /api/recommendations?limit=20
```

확인 포인트:

- History view의 outfit summary에 썸네일이 표시된다.
- 추천 후 이미지를 교체하면 이력에도 최신 이미지 상태가 표시된다.
- 추천 당시 이미지 snapshot을 별도로 저장하지 않는다.

### 9. Docker Compose 재시작 유지 확인

명령:

```bash
docker compose restart app
```

확인 포인트:

- app container 재시작 후에도 기존 이미지가 조회된다.
- DB 메타데이터와 volume 파일이 함께 유지된다.

## 실패 케이스 확인

- 토큰 없이 이미지 업로드: `401`
- 다른 사용자 옷 이미지 조회: `404 CLOTHING_NOT_FOUND`
- 내 옷이지만 이미지 없음: `404 CLOTHING_IMAGE_NOT_FOUND`
- 5MB 초과 파일: `400 INVALID_REQUEST`
- `.gif` 또는 `.heic` 파일: `400 INVALID_REQUEST`
- MIME type과 확장자가 맞지 않는 파일: `400 INVALID_REQUEST`

## 완료 기준

- 옷 이미지 업로드, 교체, 삭제가 가능하다.
- 신규/빈 계정에 기본 옷 프리셋이 한 번만 생성된다.
- 옷 목록과 추천 결과와 추천 이력에 썸네일이 보인다.
- 이미지가 없는 옷도 기존 fallback UI로 자연스럽게 보인다.
- Docker Compose 환경에서 app 재시작 후 이미지가 유지된다.
- 이미지 업로드가 추천 점수나 추천 이유를 바꾸지 않는다.

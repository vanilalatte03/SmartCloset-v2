# Demo Scenario: SmartCloset MVP4

## 데모 목표
Docker Compose로 SmartCloset 백엔드, MySQL, React 프론트엔드를 실행한 뒤 브라우저에서 신규 사용자가 2분 안에 첫 추천을 성공시키는 흐름을 확인한다.

MVP4 데모의 핵심은 API 기능 존재 여부가 아니라 사용자가 다음 행동을 이해하고 완료할 수 있는지다.

## MVP4 데모 범위
- 회원가입/로그인
- `sessionStorage` access token 저장과 세션 복구
- 오늘 추천 화면 진입
- 현재 날씨 요약 조회
- 첫 추천 준비 체크리스트 확인
- 위치 확인 또는 변경
- 선호도 저장
- TOP/BOTTOM/OUTER 최소 등록
- 추천 생성
- 추천 실패 CTA 확인
- 추천 성공 결과 확인
- 착용 완료와 이력 확인

외부 주소/지도 API, AI/GPT 추천, 이미지 업로드, refresh token, 소셜 로그인, 비밀번호 재설정, native app/PWA 출시는 데모 범위가 아니다.

## DB 초기화
로컬 Docker Compose DB는 기존 schema/seed data와 충돌할 수 있으므로 데모 전 초기화를 권장한다.

```bash
docker compose down -v
docker compose up --build
```

운영 DB migration은 현재 문서 범위에서 다루지 않는다. 로컬 데모 기준은 volume 초기화로 정리한다.

## 데모 전제
- Docker Compose 실행 완료
- Frontend 접속 가능: http://localhost:5173
- Swagger UI 접속 가능: http://localhost:8080/swagger-ui/index.html
- 신규 사용자는 기본 위치 서울특별시 `SEOUL`, `nx=60`, `ny=127`
- 신규 사용자의 기본 선호도는 `preferredColors=[]`, `preferredMaterials=[]`, `styleTags=[]`
- 프론트 access token 저장 위치는 `sessionStorage`
- 서비스키 없이 실행하면 fallback 날씨 `temperature=12`, `weatherType=CLOUDY`, `rainy=false`, `windy=false`를 사용한다.

fallback 날씨는 OUTER 필수 조건이므로 첫 추천 성공 데모에는 TOP, BOTTOM, OUTER가 각각 1개 이상 필요하다.

## 환경변수
서비스키 없이 실행하면 fallback 날씨를 사용한다.

```env
JWT_SECRET=change-me-local-development-only
KMA_SERVICE_KEY=
KMA_BASE_URL=http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0
WEATHER_FALLBACK_ENABLED=true
VITE_API_BASE_URL=http://localhost:8080
```

실제 API 연동을 확인하려면 `.env`에 공공데이터포털에서 발급받은 서비스키를 설정한다. 실제 서비스키와 운영 JWT secret은 문서, 코드, 커밋에 남기지 않는다.

## React 앱 MVP4 데모 시나리오

### 1. 회원가입 또는 로그인
목적: 사용자가 인증 후 제품 화면으로 진입한다.

API:

```http
POST /api/auth/signup
POST /api/auth/login
GET /api/users/me
```

확인 포인트:
- 인증 화면은 한국어로 표시된다.
- 소셜 로그인, 비밀번호 찾기, 이메일 인증 UI가 없다.
- 로그인 성공 후 access token이 `sessionStorage`에 저장된다.
- 새로고침 후 `GET /api/users/me`로 로그인 상태가 복구된다.
- 로그인 후 기본 view는 `오늘`이다.

### 2. 오늘 추천 화면 확인
목적: 첫 추천을 위해 무엇이 부족한지 사용자가 즉시 알 수 있다.

확인 포인트:
- 현재 위치와 날씨 요약이 보인다.
- 날씨 요약은 `GET /api/weather/current` 보호 API로 조회한다.
- 날씨 요약 조회는 추천 결과를 생성하거나 추천 이력을 만들지 않는다.
- 첫 추천 준비 체크리스트가 보인다.
- 위치 확인, 선호도 저장, 상의 등록, 하의 등록, 아우터 등록 상태가 구분된다.
- 추천 생성 CTA가 화면의 주요 행동으로 보인다.
- 모바일에서는 하단 탭 `오늘`, `옷장`, `선호도`, `위치`, `이력`이 보인다.

### 3. 위치 확인 또는 변경
목적: 외부 지도 없이 내장 대표 격자 catalog로 위치를 선택한다.

API:

```http
GET /api/users/me/location
GET /api/locations?keyword=부산
PUT /api/users/me/location
```

요청:

```json
{
  "locationCode": "BUSAN"
}
```

확인 포인트:
- 기본 위치는 서울특별시다.
- 응답에 `userId`가 없다.
- `GET /api/locations`는 로그인 후에만 호출한다.
- 외부 지도 UI나 browser location 권한 요청이 없다.
- 위치 선택 후 오늘 추천 화면의 위치 요약이 갱신된다.

### 4. 선호도 저장
목적: 추천에 영향을 주는 선호 색상/소재와 표시용 style tag를 구분한다.

API:

```http
GET /api/users/me/preferences
PUT /api/users/me/preferences
```

요청:

```json
{
  "preferredColors": ["NAVY", "BLACK"],
  "preferredMaterials": ["COTTON"],
  "styleTags": ["MINIMAL", "CASUAL"]
}
```

확인 포인트:
- 색상은 한국어 라벨과 swatch로 표시된다.
- 소재는 한국어 라벨과 chip으로 표시된다.
- `styleTags`는 저장/조회/표시만 한다는 뉘앙스로 표현된다.
- 저장 후 체크리스트의 선호도 항목이 완료된다.

### 5. 옷장 빠른 등록
목적: 추천 성공에 필요한 TOP/BOTTOM/OUTER를 빠르게 등록한다.

API:

```http
GET /api/clothes
POST /api/clothes
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

BOTTOM 요청 예시:

```json
{
  "name": "블랙 데님",
  "category": "BOTTOM",
  "color": "BLACK",
  "material": "DENIM",
  "minTemperature": 0,
  "maxTemperature": 22,
  "rainSuitable": false
}
```

OUTER 요청 예시:

```json
{
  "name": "네이비 코트",
  "category": "OUTER",
  "color": "NAVY",
  "material": "WOOL",
  "minTemperature": -10,
  "maxTemperature": 12,
  "rainSuitable": false
}
```

확인 포인트:
- category/color/material이 한국어 라벨로 보인다.
- 색상 swatch와 소재 chip이 보인다.
- 계절/기온 프리셋이 입력을 돕는다.
- 등록 후 오늘 추천 체크리스트가 갱신된다.

### 6. 옷 수정과 보관
목적: 등록한 옷을 실제로 관리할 수 있다.

API:

```http
PUT /api/clothes/{clothingId}
PATCH /api/clothes/{clothingId}/archive
```

확인 포인트:
- 목록 카드에서 수정과 보관이 가능하다.
- 모바일에서 hover 없이 액션에 접근할 수 있다.
- 보관된 옷은 활성 목록과 추천 후보에서 제외된다.
- archive 처리는 idempotent하다.

### 7. 추천 실패 CTA 확인
목적: 추천 실패 시 내부 코드 대신 다음 행동을 보여준다.

테스트 방법:
- TOP을 모두 보관한 뒤 추천 생성
- BOTTOM을 모두 보관한 뒤 추천 생성
- OUTER 필수 날씨에서 OUTER를 모두 보관한 뒤 추천 생성

확인 포인트:

| Scenario | Expected Failure | UI |
| --- | --- | --- |
| TOP 없음 | `NO_TOP_AVAILABLE` | 상의가 부족해요. 상의 등록하기 |
| BOTTOM 없음 | `NO_BOTTOM_AVAILABLE` | 하의가 부족해요. 하의 등록하기 |
| OUTER 없음 | `OUTER_REQUIRED_BUT_NOT_AVAILABLE` | 아우터가 필요해요. 아우터 등록하기 |

추천 실패는 비즈니스 실패이므로 HTTP `422 Unprocessable Entity`로 응답한다.

### 8. 추천 성공
목적: 사용자가 오늘 입기 좋은 이유 중심으로 추천 결과를 이해한다.

API:

```http
POST /api/recommendations
```

확인 포인트:
- HTTP status는 `201 Created`다.
- 응답과 화면에 `userId`가 없다.
- `weather`가 존재한다.
- `outfit.top`, `outfit.bottom`이 존재한다.
- fallback 날씨에서는 `outfit.outer`가 존재한다.
- `reasons`가 "오늘 입기 좋은 이유"로 먼저 표시된다.
- `score`는 보조 영역에 표시된다.
- `preferenceScore`가 표시되고 기존 다양성 점수 표현은 없다.
- `styleTags`만 바꿔도 추천 점수와 추천 이유가 바뀌지 않는다.

### 9. 착용 완료와 이력 확인
목적: 추천 결과를 실제 착용 완료로 처리하고 이력에서 확인한다.

API:

```http
PATCH /api/recommendations/{recommendationId}/worn
GET /api/recommendations?limit=20
```

확인 포인트:
- 착용 완료 후 `worn=true`가 표시된다.
- 같은 추천에 다시 호출해도 중복 `WearHistory`를 만들지 않고 성공한다.
- 이력은 최신순이다.
- 이력 카드에서 착용 여부와 추천 옷 조합을 확인할 수 있다.

## Swagger 보조 시나리오
프론트 문제를 분리해 API만 확인해야 할 때 Swagger UI를 사용한다.

1. `POST /api/auth/signup`
2. `POST /api/auth/login`
3. 발급받은 access token을 Swagger authorize 또는 header에 설정
4. `GET /api/users/me`
5. `GET /api/users/me/location`
6. `GET /api/weather/current`
7. `GET /api/locations?keyword=서울`
8. `PUT /api/users/me/location`
9. `GET /api/users/me/preferences`
10. `PUT /api/users/me/preferences`
11. `GET /api/clothes`
12. `POST /api/clothes`
13. `PUT /api/clothes/{clothingId}`
14. `PATCH /api/clothes/{clothingId}/archive`
15. `POST /api/recommendations`
16. `GET /api/recommendations?limit=20`
17. `PATCH /api/recommendations/{recommendationId}/worn`

## KMA 연동 수동 확인
서비스키가 있을 때만 수행한다.

1. `.env`에 `KMA_SERVICE_KEY`를 설정한다.
2. Docker Compose로 앱을 실행한다.
3. React 앱에서 로그인하고 위치를 서울, 부산, 제주 중 하나로 선택한다.
4. 추천 생성을 실행한다.
5. 응답 또는 화면의 `weather`가 선택 위치의 기상청 예보값에 맞게 달라질 수 있음을 확인한다.

주의:
- 실제 서비스키는 출력, 문서, 커밋에 남기지 않는다.
- 기상청 `NODATA` 또는 호출 실패가 발생해도 fallback이 활성화되어 있으면 추천은 성공할 수 있다.

## 모바일 확인
- 375px 너비에서 하단 탭이 겹치지 않는다.
- Today view의 체크리스트와 추천 CTA가 화면 밖으로 깨지지 않는다.
- Closet view에서 수정/보관 액션이 hover 없이 접근 가능하다.
- Preferences view의 swatch와 chip이 가로 스크롤 또는 줄바꿈으로 자연스럽게 표시된다.
- History view의 긴 날짜/옷 이름이 카드 밖으로 넘치지 않는다.

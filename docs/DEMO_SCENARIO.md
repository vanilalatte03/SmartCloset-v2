# Demo Scenario: SmartCloset 3차 MVP

## 데모 목표
Docker Compose로 SmartCloset 백엔드, MySQL, React 프론트엔드를 실행한 뒤 브라우저에서 회원가입/로그인, 사용자 위치 선택, 선호도 저장, 옷장 기반 추천, 추천 이력 조회 흐름을 확인한다.

3차 데모는 아래 흐름을 지원한다.

- 서비스키 없음: `StaticWeatherProvider` fallback으로 안정적인 추천 흐름 확인
- 서비스키 있음: 인증 사용자 위치 `nx`, `ny`로 기상청 단기예보 `getVilageFcst` JSON 기반 날씨 확인
- React 앱: 회원가입/로그인, 위치 선택, 선호도 저장, 옷 목록, 옷 등록, 추천 생성, 추천 이력, 착용 완료 처리

외부 주소/지도 API, AI/GPT 추천, 이미지 업로드, refresh token, 소셜 로그인은 데모 범위가 아니다.

## MVP 3 전환 전 DB 초기화
MVP 3 전환 시 로컬 Docker Compose DB는 기존 2차 schema/seed data와 충돌할 수 있으므로 초기화를 권장한다.

```bash
docker compose down -v
docker compose up --build
```

운영 DB migration은 3차 문서 범위에서 다루지 않는다. 로컬 데모 기준은 volume 초기화로 정리한다.

## 데모 전제
- Docker Compose 실행 완료
- Frontend 접속 가능
- Swagger UI 접속 가능
- 신규 사용자는 기본 위치 서울특별시 `SEOUL`, `nx=60`, `ny=127`
- 신규 사용자의 기본 선호도는 `preferredColors=[]`, `preferredMaterials=[]`, `styleTags=[]`
- 프론트 access token 저장 위치는 `sessionStorage`

## 환경변수
서비스키 없이 실행하면 fallback 날씨를 사용한다.

```env
JWT_SECRET=change-me-local-development-only
KMA_SERVICE_KEY=
KMA_BASE_URL=http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0
WEATHER_FALLBACK_ENABLED=true
```

실제 API 연동을 확인하려면 `.env`에 공공데이터포털에서 발급받은 서비스키를 설정한다. 실제 서비스키와 운영 JWT secret은 문서, 코드, 커밋에 남기지 않는다.

프론트 앱은 `VITE_API_BASE_URL=http://localhost:8080`을 사용한다.

## 접속 경로
- Frontend: http://localhost:5173
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- 보조 Demo UI: http://localhost:8080/demo/index.html

3차의 주 데모 경로는 React 프론트엔드다. Swagger 또는 Spring static Demo UI는 보조 smoke 확인용으로 사용한다.

## React 앱 데모 시나리오

### 1. 회원가입
목적: 새 사용자가 기본 위치와 빈 선호도 배열로 생성되는지 확인한다.

API:

```http
POST /api/auth/signup
Content-Type: application/json
```

요청:

```json
{
  "email": "demo@example.com",
  "password": "password123!",
  "name": "Demo User"
}
```

확인 포인트:
- HTTP status는 `201 Created`다.
- 같은 email로 다시 가입하면 `EMAIL_ALREADY_EXISTS`로 실패한다.
- 신규 사용자의 위치는 서울특별시다.
- 신규 사용자의 선호도는 모두 빈 배열이다.

### 2. 로그인과 세션 복구
목적: 로그인 후 access token을 저장하고 보호 API를 호출할 수 있는지 확인한다.

API:

```http
POST /api/auth/login
GET /api/users/me
```

확인 포인트:
- 로그인 성공 응답에 `accessToken`, `tokenType=Bearer`, 사용자 정보가 포함된다.
- 프론트는 access token을 `sessionStorage`에 저장한다.
- 새로고침 후 `GET /api/users/me`로 로그인 상태가 복구된다.
- 로그아웃하면 `sessionStorage` token과 사용자 상태가 제거된다.

### 3. 기본 위치 확인
목적: 인증 사용자의 기본 위치가 서울로 표시되는지 확인한다.

API:

```http
GET /api/users/me/location
Authorization: Bearer {accessToken}
```

기대 응답:

```json
{
  "data": {
    "code": "SEOUL",
    "name": "서울특별시",
    "nx": 60,
    "ny": 127,
    "updatedAt": "2026-05-22T10:00:00"
  }
}
```

확인 포인트:
- 응답에 `userId`가 없다.
- token 없이 호출하면 `401`로 실패한다.

### 4. 위치 검색과 선택
목적: 로그인 후 내장 대표 격자 catalog에서 위치를 찾고 사용자 위치로 저장할 수 있는지 확인한다.

API:

```http
GET /api/locations?keyword=부산
PUT /api/users/me/location
Authorization: Bearer {accessToken}
Content-Type: application/json
```

요청:

```json
{
  "locationCode": "BUSAN"
}
```

확인 포인트:
- `GET /api/locations`는 보호 API다.
- 회원가입 화면에서는 위치 catalog를 호출하지 않는다.
- 검색 결과에 `BUSAN`, `부산광역시`, `nx=98`, `ny=76`이 표시된다.
- 위치 선택 후 현재 위치가 부산으로 바뀐다.
- 잘못된 code 선택 시 `LOCATION_NOT_FOUND`가 표시된다.
- `GET /api/locations`의 `401`은 위치 검색 실패가 아니라 인증 만료로 처리한다.

### 5. 선호도 저장과 조회
목적: 선호 색상/소재/styleTags를 저장하고 다시 조회할 수 있는지 확인한다.

API:

```http
GET /api/users/me/preferences
PUT /api/users/me/preferences
Authorization: Bearer {accessToken}
Content-Type: application/json
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
- 기본값은 `preferredColors=[]`, `preferredMaterials=[]`, `styleTags=[]`다.
- 저장 후 같은 배열을 다시 조회할 수 있다.
- `styleTags`는 화면에 표시되지만 추천 점수와 추천 이유에는 반영되지 않는다.

### 6. 옷 목록 조회와 등록
목적: 인증 사용자 기준 활성 옷 목록을 조회하고 새 옷을 등록할 수 있는지 확인한다.

API:

```http
GET /api/clothes
POST /api/clothes
Authorization: Bearer {accessToken}
Content-Type: application/json
```

서비스키 없이 데모하면 fallback 날씨가 `temperature=12`로 고정된다. 이 온도에서는 OUTER가 필수이므로 추천 성공 데모 전에 TOP, BOTTOM, OUTER를 각각 1개 이상 등록한다.

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
- 응답에 `userId`가 없다.
- 목록은 현재 인증 사용자의 `archived=false`인 옷만 포함한다.
- 등록 후 프론트 목록이 갱신된다.
- fallback 날씨 기준 추천 성공을 위해 TOP, BOTTOM, OUTER가 모두 존재한다.

### 7. 추천 생성
목적: 현재 인증 사용자 위치, 옷장, 선호도 기준으로 추천을 생성하고 저장하는지 확인한다.

API:

```http
POST /api/recommendations
Authorization: Bearer {accessToken}
```

요청 body는 없다.

확인 포인트:
- HTTP status는 `201 Created`다.
- 응답에 `userId`가 없다.
- `weather`가 존재한다.
- 서비스키 없음 상태에서는 fallback 값 `temperature=12`, `weatherType=CLOUDY`, `rainy=false`, `windy=false`가 반환된다.
- `outfit.top`, `outfit.bottom`이 존재한다.
- `score`에는 `preferenceScore`가 있고 기존 다양성 점수 필드는 없다.
- 선호 색상 또는 소재와 맞는 옷이 포함되면 `preferenceScore`가 5점 또는 10점이 될 수 있다.
- `styleTags`만 바꿔도 추천 점수와 추천 이유가 바뀌지 않는다.
- `reasons`는 3개 이상 5개 이하이다.

### 8. 추천 이력 조회
목적: 현재 인증 사용자의 추천 이력을 최신순으로 조회할 수 있는지 확인한다.

API:

```http
GET /api/recommendations?limit=20
Authorization: Bearer {accessToken}
```

확인 포인트:
- 기본 limit은 20이다.
- 최대 limit은 50이다.
- `limit=51` 또는 숫자가 아닌 값은 `400 INVALID_REQUEST`로 실패한다.
- 다른 사용자의 추천 이력은 포함되지 않는다.

### 9. 추천 결과 착용 완료
목적: 추천 결과를 실제 착용 완료로 처리하고 이후 추천 이력에 반영할 수 있는지 확인한다.

API:

```http
PATCH /api/recommendations/{recommendationId}/worn
Authorization: Bearer {accessToken}
```

확인 포인트:
- 응답에 `userId`가 없다.
- `worn=true`로 변경된다.
- `WearHistory`가 생성된다.
- 같은 `recommendationId`로 다시 호출해도 중복 `WearHistory`를 만들지 않고 성공한다.
- 다른 사용자의 추천 결과는 처리할 수 없다.

## Swagger 보조 시나리오
프론트 문제를 분리해 API만 확인해야 할 때 Swagger UI를 사용한다.

1. `POST /api/auth/signup`
2. `POST /api/auth/login`
3. 발급받은 access token을 Swagger authorize 또는 header에 설정
4. `GET /api/users/me`
5. `GET /api/users/me/location`
6. `GET /api/locations?keyword=서울`
7. `PUT /api/users/me/location`
8. `GET /api/users/me/preferences`
9. `PUT /api/users/me/preferences`
10. `GET /api/clothes`
11. `POST /api/recommendations`
12. `GET /api/recommendations?limit=20`
13. `PATCH /api/recommendations/{recommendationId}/worn`

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

## 실패 케이스 데모 후보

| Scenario | Expected Failure |
| --- | --- |
| token 없이 보호 API 호출 | `UNAUTHORIZED` |
| 중복 email 회원가입 | `EMAIL_ALREADY_EXISTS` |
| 존재하지 않는 위치 code 선택 | `LOCATION_NOT_FOUND` |
| 추천 이력 `limit=51` | `INVALID_REQUEST` |
| TOP을 모두 archive 처리 | `NO_TOP_AVAILABLE` |
| BOTTOM을 모두 archive 처리 | `NO_BOTTOM_AVAILABLE` |
| OUTER 필수 날씨에서 OUTER를 모두 archive 처리 | `OUTER_REQUIRED_BUT_NOT_AVAILABLE` |

추천 실패는 비즈니스 실패이므로 HTTP `422 Unprocessable Entity`로 응답한다.

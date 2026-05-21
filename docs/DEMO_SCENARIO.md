# Demo Scenario: SmartCloset 2차 MVP

## 데모 목표
Docker Compose로 SmartCloset 백엔드, MySQL, React 프론트엔드를 실행한 뒤 브라우저에서 사용자 위치 선택과 옷장 기반 추천 흐름을 확인한다.

2차 데모는 아래 흐름을 지원한다.

- 서비스키 없음: `StaticWeatherProvider` fallback으로 안정적인 추천 흐름 확인
- 서비스키 있음: 사용자별 위치 `nx`, `ny`로 기상청 단기예보 `getVilageFcst` JSON 기반 날씨 확인
- React 앱: 위치 선택, 옷 목록, 옷 등록, 추천 생성, 착용 완료 처리

로그인/회원가입, 외부 주소/지도 API, AI/GPT 추천, 이미지 업로드는 데모 범위가 아니다.

## 데모 전제
- Docker Compose 실행 완료
- Frontend 접속 가능
- Swagger UI 접속 가능
- seed user: `userId=1`, `name=demo-user`
- seed user 기본 위치: 서울특별시 `SEOUL`, `nx=60`, `ny=127`

## 환경변수
서비스키 없이 실행하면 fallback 날씨를 사용한다.

```env
KMA_SERVICE_KEY=
KMA_BASE_URL=http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0
WEATHER_FALLBACK_ENABLED=true
```

실제 API 연동을 확인하려면 `.env`에 공공데이터포털에서 발급받은 서비스키를 설정한다. 실제 서비스키는 문서, 코드, 커밋에 남기지 않는다.

2차 frontend step 완료 후에는 프론트 앱에서 사용할 `VITE_API_BASE_URL=http://localhost:8080`을 추가한다.

## 접속 경로
- Frontend(2차 frontend step 완료 후): http://localhost:5173
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- 보조 Demo UI: http://localhost:8080/demo/index.html

2차의 주 데모 경로는 React 프론트엔드다. 현재 문서 전환 시점에는 `frontend/`가 아직 없을 수 있으므로, frontend 구현 step 완료 전에는 Swagger 또는 Spring static Demo UI를 보조 smoke 확인용으로 사용한다.

## React 앱 데모 시나리오

### 1. 앱 접속과 기본 위치 확인
목적: seed user의 기본 위치가 서울로 표시되는지 확인한다.

확인 포인트:
- 화면에 `userId=1` 기준 현재 위치가 표시된다.
- 위치 이름은 `서울특별시`다.
- `nx=60`, `ny=127`이 확인 가능하다.

API:

```http
GET /api/users/location?userId=1
```

기대 응답:

```json
{
  "data": {
    "userId": 1,
    "code": "SEOUL",
    "name": "서울특별시",
    "nx": 60,
    "ny": 127,
    "updatedAt": "2026-05-21T10:00:00"
  }
}
```

### 2. 위치 검색과 선택
목적: 내장 대표 격자 catalog에서 위치를 찾고 사용자 위치로 저장할 수 있는지 확인한다.

API:

```http
GET /api/locations?keyword=부산
PUT /api/users/location?userId=1
Content-Type: application/json
```

요청:

```json
{
  "locationCode": "BUSAN"
}
```

확인 포인트:
- 검색 결과에 `BUSAN`, `부산광역시`, `nx=98`, `ny=76`이 표시된다.
- 위치 선택 후 현재 위치가 부산으로 바뀐다.
- 잘못된 code 선택 시 `LOCATION_NOT_FOUND`가 표시된다.

### 3. 옷 목록 조회
목적: seed data가 로드되었고 `userId=1` 기준 활성 옷 목록을 조회할 수 있는지 확인한다.

API:

```http
GET /api/clothes?userId=1
```

확인 포인트:
- TOP, BOTTOM, OUTER가 최소 1개 이상 존재한다.
- 기본 목록은 `archived=false`인 옷만 포함한다.
- 정렬은 `id` 오름차순이다.

### 4. 옷 등록
목적: 데모 중 새 옷을 등록하고 목록/추천 후보에 반영할 수 있는지 확인한다.

API:

```http
POST /api/clothes?userId=1
Content-Type: application/json
```

요청 예시:

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
- HTTP status는 `201 Created`다.
- `archived`는 요청하지 않아도 `false`로 저장된다.
- 등록 후 프론트 목록이 갱신된다.

### 5. 추천 생성
목적: 현재 사용자 위치와 옷장 기준으로 추천을 생성하고 저장하는지 확인한다.

API:

```http
POST /api/recommendations?userId=1
```

요청 body는 없다.

확인 포인트:
- HTTP status는 `201 Created`다.
- 화면에 현재 사용자 위치가 표시된다.
- `weather`가 존재한다.
- 서비스키 없음 상태에서는 fallback 값 `temperature=12`, `weatherType=CLOUDY`, `rainy=false`, `windy=false`가 반환된다.
- 서비스키 설정 상태에서는 현재 사용자 위치의 KMA 예보값이 반환될 수 있다.
- `outfit.top`, `outfit.bottom`이 존재한다.
- `outfit.outer`는 날씨 조건에 따라 객체 또는 `null`일 수 있다.
- `score.totalScore`와 세부 점수가 존재한다.
- `reasons`는 3개 이상 5개 이하이다.

### 6. 추천 결과 착용 완료
목적: 추천 결과를 실제 착용 완료로 처리하고 이후 추천 이력에 반영할 수 있는지 확인한다.

API:

```http
PATCH /api/recommendations/{recommendationId}/worn?userId=1
```

확인 포인트:
- `worn=true`로 변경된다.
- `WearHistory`가 생성된다.
- 같은 `recommendationId`로 다시 호출해도 중복 `WearHistory`를 만들지 않고 성공한다.
- 착용 완료 처리는 idempotent하다.

## Swagger 보조 시나리오
프론트 문제를 분리해 API만 확인해야 할 때 Swagger UI를 사용한다.

1. `GET /api/users/location?userId=1`
2. `GET /api/locations?keyword=서울`
3. `PUT /api/users/location?userId=1`
4. `GET /api/clothes?userId=1`
5. `POST /api/recommendations?userId=1`
6. `PATCH /api/recommendations/{recommendationId}/worn?userId=1`

## KMA 연동 수동 확인
서비스키가 있을 때만 수행한다.

1. `.env`에 `KMA_SERVICE_KEY`를 설정한다.
2. Docker Compose로 앱을 실행한다.
3. React 앱에서 위치를 서울, 부산, 제주 중 하나로 선택한다.
4. 추천 생성을 실행한다.
5. 응답 또는 화면의 `weather`가 선택 위치의 기상청 예보값에 맞게 달라질 수 있음을 확인한다.

주의:
- 실제 서비스키는 출력, 문서, 커밋에 남기지 않는다.
- 기상청 `NODATA` 또는 호출 실패가 발생해도 fallback이 활성화되어 있으면 추천은 성공할 수 있다.

## 실패 케이스 데모 후보

| Scenario | Expected Failure |
| --- | --- |
| 존재하지 않는 위치 code 선택 | `LOCATION_NOT_FOUND` |
| TOP을 모두 archive 처리 | `NO_TOP_AVAILABLE` |
| BOTTOM을 모두 archive 처리 | `NO_BOTTOM_AVAILABLE` |
| OUTER 필수 날씨에서 OUTER를 모두 archive 처리 | `OUTER_REQUIRED_BUT_NOT_AVAILABLE` |

추천 실패는 비즈니스 실패이므로 HTTP `422 Unprocessable Entity`로 응답한다.

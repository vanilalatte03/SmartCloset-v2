# Demo Scenario: SmartCloset 1.5차 MVP

## 데모 목표
Docker Compose로 SmartCloset을 실행한 뒤 Swagger 또는 Demo UI에서 옷장 기반 추천 흐름을 확인한다.

1.5차 데모는 두 경로를 모두 지원한다.

- 서비스키 없음: `StaticWeatherProvider` fallback으로 1차 MVP와 같은 안정적인 추천 흐름 확인
- 서비스키 있음: 기상청 단기예보 `getVilageFcst` JSON 기반 날씨가 추천 응답의 `weather`에 반영되는지 확인

정식 프론트엔드 앱, 로그인/회원가입, 사용자별 위치 저장, AI/GPT 추천, 이미지 업로드는 데모 범위가 아니다.

## 데모 전제
- Docker Compose 실행 완료
- Swagger UI 접속 가능
- Demo UI 접속 가능
- seed user: `userId=1`, `name=demo-user`
- 기본 위치: 서울특별시 격자 `KMA_NX=60`, `KMA_NY=127`

## 환경변수
서비스키 없이 실행하면 fallback 날씨를 사용한다.

```env
KMA_SERVICE_KEY=
KMA_NX=60
KMA_NY=127
KMA_BASE_URL=http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0
WEATHER_FALLBACK_ENABLED=true
```

실제 API 연동을 확인하려면 `.env`에 공공데이터포털에서 발급받은 서비스키를 설정한다. 실제 서비스키는 문서, 코드, 커밋에 남기지 않는다.

## 접속 경로
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Demo UI: http://localhost:8080/demo/index.html

## Swagger 데모 시나리오

### 1. 옷 목록 조회
목적: seed data가 로드되었고 `userId=1` 기준 활성 옷 목록을 조회할 수 있는지 확인한다.

API:

```http
GET /api/clothes?userId=1
```

기대 응답 핵심:

```json
{
  "data": [
    {
      "id": 1,
      "userId": 1,
      "name": "아이보리 니트",
      "category": "TOP",
      "color": "WHITE",
      "material": "KNIT",
      "archived": false
    }
  ]
}
```

확인 포인트:
- `data` 배열이 반환된다.
- TOP, BOTTOM, OUTER가 최소 1개 이상 존재한다.
- 기본 목록은 `archived=false`인 옷만 포함한다.
- 정렬은 `id` 오름차순이다.

### 2. 옷 등록
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

기대 응답 핵심:

```json
{
  "data": {
    "id": 6,
    "userId": 1,
    "name": "그레이 후드",
    "category": "TOP",
    "color": "GRAY",
    "material": "COTTON",
    "minTemperature": 5,
    "maxTemperature": 18,
    "rainSuitable": false,
    "archived": false
  }
}
```

확인 포인트:
- HTTP status는 `201 Created`다.
- `archived`는 요청하지 않아도 `false`로 저장된다.
- `material`이 저장되고 조회 응답에 포함된다.

### 3. 추천 생성
목적: 현재 옷장과 `WeatherProvider`가 제공한 날씨 기준으로 추천을 생성하고 저장하는지 확인한다.

API:

```http
POST /api/recommendations?userId=1
```

요청 body는 없다.

기대 응답 핵심:

```json
{
  "data": {
    "recommendationId": 1,
    "weather": {
      "temperature": 12,
      "weatherType": "CLOUDY",
      "rainy": false,
      "windy": false
    },
    "outfit": {
      "top": {
        "id": 1,
        "category": "TOP"
      },
      "bottom": {
        "id": 2,
        "category": "BOTTOM"
      },
      "outer": {
        "id": 3,
        "category": "OUTER"
      }
    },
    "score": {
      "totalScore": 99,
      "weatherScore": 34,
      "colorScore": 25,
      "wearHistoryScore": 20,
      "recommendationHistoryScore": 10,
      "diversityScore": 10
    },
    "reasons": [
      "현재 기온이 낮아 아우터를 포함한 조합을 추천했습니다.",
      "상의와 하의 색상이 무채색 중심이라 안정적인 조합입니다.",
      "최근 착용 이력이 적어 반복 착용 부담이 낮습니다."
    ],
    "worn": false,
    "createdAt": "2026-05-20T10:00:00"
  }
}
```

확인 포인트:
- HTTP status는 `201 Created`다.
- `weather`가 존재한다.
- 서비스키 없음 상태에서는 fallback 값 `temperature=12`, `weatherType=CLOUDY`, `rainy=false`, `windy=false`가 반환된다.
- 서비스키 설정 상태에서는 기상청 단기예보 JSON에서 매핑된 값이 반환될 수 있다.
- `outfit.top`, `outfit.bottom`이 존재한다.
- `outfit.outer`는 날씨 조건에 따라 객체 또는 `null`일 수 있다.
- `score.totalScore`와 세부 점수가 존재한다.
- `reasons`는 3개 이상 5개 이하이다.
- `worn=false`다.
- `createdAt`이 존재한다.

### 4. 추천 결과 착용 완료
목적: 추천 결과를 실제 착용 완료로 처리하고 이후 추천 이력에 반영할 수 있는지 확인한다.

API:

```http
PATCH /api/recommendations/{recommendationId}/worn?userId=1
```

기대 응답 핵심:

```json
{
  "data": {
    "recommendationId": 1,
    "worn": true,
    "wornAt": "2026-05-20T11:00:00"
  }
}
```

확인 포인트:
- `worn=true`로 변경된다.
- `WearHistory`가 생성된다.
- 같은 `recommendationId`로 다시 호출해도 중복 `WearHistory`를 만들지 않고 성공한다.
- 착용 완료 처리는 idempotent하다.

### 5. 추천 재생성
목적: 착용 완료 이력이 다음 추천 점수 또는 추천 결과에 반영되는지 확인한다.

API:

```http
POST /api/recommendations?userId=1
```

기대 응답 핵심:
- 새 `recommendationId`가 반환된다.
- 최근 착용한 옷이 포함되면 `wearHistoryScore`가 낮아질 수 있다.
- 동일 조합이 최근 추천 결과에 있으면 `recommendationHistoryScore` 또는 `diversityScore`가 낮아질 수 있다.

## KMA 연동 수동 확인
서비스키가 있을 때만 수행한다.

1. `.env`에 `KMA_SERVICE_KEY`를 설정한다.
2. `KMA_NX=60`, `KMA_NY=127`을 유지하거나 원하는 격자로 변경한다.
3. `docker compose up --build`를 실행한다.
4. Swagger에서 `POST /api/recommendations?userId=1`을 호출한다.
5. 응답의 `weather`가 호출 시점의 기상청 예보값에 맞게 달라질 수 있음을 확인한다.

주의:
- 실제 서비스키는 출력, 문서, 커밋에 남기지 않는다.
- 기상청 `NODATA` 또는 호출 실패가 발생해도 fallback이 활성화되어 있으면 추천은 성공할 수 있다.

## 실패 케이스 데모 후보
아래 실패 케이스는 옷 보관 처리 API로 확인할 수 있는 선택 시나리오다.

| Scenario | Expected Failure |
| --- | --- |
| TOP을 모두 archive 처리 | `NO_TOP_AVAILABLE` |
| BOTTOM을 모두 archive 처리 | `NO_BOTTOM_AVAILABLE` |
| OUTER 필수 날씨에서 OUTER를 모두 archive 처리 | `OUTER_REQUIRED_BUT_NOT_AVAILABLE` |

추천 실패는 비즈니스 실패이므로 HTTP `422 Unprocessable Entity`로 응답한다.

## Demo UI 시나리오
Demo UI는 아래 경로에서 API 흐름을 확인한다.

```text
http://localhost:8080/demo/index.html
```

Demo UI 기능은 아래로 제한한다.

- 옷 등록
- 옷 목록 조회
- 추천 생성
- 착용 완료 처리

Demo UI는 제품용 프론트가 아니라 API 흐름 공유용 Spring Boot static resource 기반 단일 페이지다.
기본 `userId`는 `1`이며 화면에서 변경할 수 있다.

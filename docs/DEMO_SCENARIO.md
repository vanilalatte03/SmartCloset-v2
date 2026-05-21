# Demo Scenario: SmartCloset 1차 MVP

## 데모 목표
Docker Compose로 SmartCloset을 실행한 뒤 P0는 Swagger에서 추천 흐름을 확인한다. P1 Demo UI가 구현된 경우에는 Demo UI에서도 같은 흐름을 확인한다.

P0 기준으로 Spring Boot 4.0.6 백엔드 추천 도메인과 API가 동작하는지 검증한다. 정식 프론트엔드 앱, 외부 Weather API, AI/GPT 추천, 이미지 업로드는 데모 범위가 아니다.

## 데모 전제
- Docker Compose 실행 완료
- Swagger UI 접속 가능
- seed user: `userId=1`, `name=demo-user`
- `StaticWeatherProvider` 고정 날씨 사용

| Field | Value |
| --- | --- |
| `temperature` | `12` |
| `weatherType` | `CLOUDY` |
| `rainy` | `false` |
| `windy` | `false` |

`temperature=12`이므로 추천 생성 시 OUTER 필수 조합이 생성되어야 한다.

## Swagger 접속
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Swagger 데모 시나리오

### 1. 옷 목록 조회
목적: seed data가 로드되었고 `userId=1` 기준 활성 옷 목록을 조회할 수 있는지 확인한다.

API:

```http
GET /api/clothes?userId=1
```

요청 body는 없다.

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
목적: 현재 옷장과 고정 날씨 기준으로 오늘의 추천을 생성하고 저장하는지 확인한다.

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
      "최근 착용 이력이 적어 반복 착용 부담이 낮습니다.",
      "니트 또는 울 소재가 현재 기온에 적합해 보온성을 보완합니다.",
      "최근 추천된 동일 조합이 아니어서 반복 추천 부담이 낮습니다."
    ],
    "worn": false,
    "createdAt": "2026-05-20T10:00:00"
  }
}
```

확인 포인트:
- HTTP status는 `201 Created`다.
- `weather.temperature=12`다.
- `outfit.top`, `outfit.bottom`, `outfit.outer`가 모두 존재한다.
- `score.totalScore`가 존재한다.
- `score.weatherScore`, `colorScore`, `wearHistoryScore`, `recommendationHistoryScore`, `diversityScore`가 모두 존재한다.
- `reasons`는 3개 이상 5개 이하이다.
- `worn=false`다.
- `createdAt`이 존재한다.

### 4. 추천 결과 착용 완료
목적: 추천 결과를 실제 착용 완료로 처리하고 이후 추천 이력에 반영할 수 있는지 확인한다.

API:

```http
PATCH /api/recommendations/{recommendationId}/worn?userId=1
```

요청 body는 없다.

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

요청 body는 없다.

기대 응답 핵심:
- 새 `recommendationId`가 반환된다.
- 최근 착용한 옷이 포함되면 `wearHistoryScore`가 낮아질 수 있다.
- 동일 조합이 최근 추천 결과에 있으면 `recommendationHistoryScore` 또는 `diversityScore`가 낮아질 수 있다.
- 전체 추천 결과는 규칙과 seed data 상태에 따라 달라질 수 있다.

확인 포인트:
- 추천이 새로 생성되고 DB에 저장된다.
- 최근 착용 이력 때문에 점수 또는 추천 결과가 달라질 수 있음을 확인한다.

## 실패 케이스 데모 후보
아래 실패 케이스는 P1의 옷 보관 처리 API가 구현된 경우 Swagger로 확인할 수 있는 선택 시나리오다.

| Scenario | Expected Failure |
| --- | --- |
| TOP을 모두 archive 처리 | `NO_TOP_AVAILABLE` |
| BOTTOM을 모두 archive 처리 | `NO_BOTTOM_AVAILABLE` |
| `temperature=12`에서 OUTER를 모두 archive 처리 | `OUTER_REQUIRED_BUT_NOT_AVAILABLE` |

실패 응답 예시:

```json
{
  "code": "OUTER_REQUIRED_BUT_NOT_AVAILABLE",
  "message": "현재 기온에는 아우터가 필요하지만 추천 가능한 아우터가 없습니다.",
  "details": []
}
```

추천 실패는 비즈니스 실패이므로 HTTP `422 Unprocessable Entity`로 응답한다.

## Demo UI 시나리오
P1 최소 데모 UI가 구현된 경우 아래 경로에서 API 흐름을 확인한다.

```text
http://localhost:8080/demo/index.html
```

Demo UI 기능은 아래로 제한한다.

- 옷 등록
- 옷 목록 조회
- 추천 생성
- 착용 완료 처리

Demo UI는 제품용 프론트가 아니라 API 흐름 공유용 Spring Boot static resource 기반 단일 페이지다.

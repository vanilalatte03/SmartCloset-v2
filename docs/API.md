# API: SmartCloset 1차 MVP

## 1. API 공통 규칙
- `userId`는 인증 대신 request parameter로 전달한다.
- 요청과 응답의 `Content-Type`은 `application/json`이다.
- 날짜/시간은 ISO-8601 문자열로 표현한다.
- enum 값은 대문자 문자열로 주고받는다.
- 성공 응답은 항상 `data` 필드를 가진다.
- 실패 응답은 항상 `code`, `message`, `details` 필드를 가진다.
- P0 API는 Docker Compose 공유 시나리오에서 반드시 호출 가능해야 한다.

### 공통 성공 응답
```json
{
  "data": {
    "id": 1
  }
}
```

### 공통 실패 응답
```json
{
  "code": "NO_TOP_AVAILABLE",
  "message": "현재 날씨에 입을 수 있는 상의가 없습니다.",
  "details": []
}
```

## 2. API 목록

### P0 API
| Method | Path | Description | Success |
| --- | --- | --- | --- |
| `POST` | `/api/clothes?userId={userId}` | 옷 등록 | `201 Created` |
| `GET` | `/api/clothes?userId={userId}` | 옷 목록 조회 | `200 OK` |
| `POST` | `/api/recommendations?userId={userId}` | 오늘의 추천 생성 및 저장 | `201 Created` |
| `PATCH` | `/api/recommendations/{recommendationId}/worn?userId={userId}` | 추천 결과 착용 완료 처리 | `200 OK` |

### P1 API
| Method | Path | Description | Success |
| --- | --- | --- | --- |
| `GET` | `/api/clothes/{clothingId}?userId={userId}` | 옷 상세 조회 | `200 OK` |
| `PUT` | `/api/clothes/{clothingId}?userId={userId}` | 옷 전체 수정 | `200 OK` |
| `PATCH` | `/api/clothes/{clothingId}/archive?userId={userId}` | 옷 보관 처리 | `200 OK` |

## 3. 공통 DTO

### ClothingRequest
옷 등록과 옷 전체 수정에서 같은 요청 필드를 사용한다.

```json
{
  "name": "Gray Knit",
  "category": "TOP",
  "color": "GRAY",
  "material": "KNIT",
  "minTemperature": 5,
  "maxTemperature": 18,
  "rainSuitable": false
}
```

Validation 규칙은 다음과 같다.

| Field | Required | Rule |
| --- | --- | --- |
| `name` | yes | blank 불가, 최대 50자 |
| `category` | yes | `TOP`, `BOTTOM`, `OUTER` |
| `color` | yes | `ClothingColor` enum |
| `material` | yes | `ClothingMaterial` enum |
| `minTemperature` | yes | 정수 |
| `maxTemperature` | yes | 정수, `minTemperature`는 `maxTemperature`보다 작거나 같아야 함 |
| `rainSuitable` | yes | boolean |

`archived`는 요청 필드가 아니다. 등록 시 서버가 `false`로 설정하며, 수정 API에서도 변경하지 않는다.

### ClothingResponse
```json
{
  "id": 1,
  "userId": 1,
  "name": "Gray Knit",
  "category": "TOP",
  "color": "GRAY",
  "material": "KNIT",
  "minTemperature": 5,
  "maxTemperature": 18,
  "rainSuitable": false,
  "archived": false,
  "createdAt": "2026-05-20T10:00:00",
  "updatedAt": "2026-05-20T10:00:00"
}
```

### WeatherResponse
```json
{
  "temperature": 12,
  "weatherType": "CLOUDY",
  "rainy": false,
  "windy": false
}
```

### RecommendationScoreResponse
```json
{
  "totalScore": 88,
  "weatherScore": 35,
  "colorScore": 25,
  "wearHistoryScore": 20,
  "recommendationHistoryScore": 8,
  "diversityScore": 0
}
```

## 4. P0: 옷 등록
`POST /api/clothes?userId={userId}`

등록된 옷은 기본적으로 `archived=false`이다.

### Request
```json
{
  "name": "Gray Knit",
  "category": "TOP",
  "color": "GRAY",
  "material": "KNIT",
  "minTemperature": 5,
  "maxTemperature": 18,
  "rainSuitable": false
}
```

### Response
`201 Created`

```json
{
  "data": {
    "id": 1,
    "userId": 1,
    "name": "Gray Knit",
    "category": "TOP",
    "color": "GRAY",
    "material": "KNIT",
    "minTemperature": 5,
    "maxTemperature": 18,
    "rainSuitable": false,
    "archived": false,
    "createdAt": "2026-05-20T10:00:00",
    "updatedAt": "2026-05-20T10:00:00"
  }
}
```

## 5. P0: 옷 목록 조회
`GET /api/clothes?userId={userId}`

기본 조회 대상은 `archived=false`인 옷이다. 정렬은 `id` 오름차순이며, P0에서는 pagination을 제공하지 않는다.

### Response
`200 OK`

```json
{
  "data": [
    {
      "id": 1,
      "userId": 1,
      "name": "Gray Knit",
      "category": "TOP",
      "color": "GRAY",
      "material": "KNIT",
      "minTemperature": 5,
      "maxTemperature": 18,
      "rainSuitable": false,
      "archived": false,
      "createdAt": "2026-05-20T10:00:00",
      "updatedAt": "2026-05-20T10:00:00"
    }
  ]
}
```

## 6. P1: 옷 상세 조회
`GET /api/clothes/{clothingId}?userId={userId}`

`clothingId`와 `userId`가 모두 일치하는 옷만 조회한다. 다른 사용자의 옷은 조회되지 않으며 `CLOTHING_NOT_FOUND`로 응답한다.

### Response
`200 OK`

```json
{
  "data": {
    "id": 1,
    "userId": 1,
    "name": "Gray Knit",
    "category": "TOP",
    "color": "GRAY",
    "material": "KNIT",
    "minTemperature": 5,
    "maxTemperature": 18,
    "rainSuitable": false,
    "archived": false,
    "createdAt": "2026-05-20T10:00:00",
    "updatedAt": "2026-05-20T10:00:00"
  }
}
```

## 7. P1: 옷 수정
`PUT /api/clothes/{clothingId}?userId={userId}`

PUT은 전체 수정 기준이다. request body는 등록과 같은 필드를 받는다. `archived`는 수정 API에서 변경하지 않는다.

### Request
```json
{
  "name": "Warm Gray Knit",
  "category": "TOP",
  "color": "GRAY",
  "material": "KNIT",
  "minTemperature": 3,
  "maxTemperature": 16,
  "rainSuitable": false
}
```

### Response
`200 OK`

```json
{
  "data": {
    "id": 1,
    "userId": 1,
    "name": "Warm Gray Knit",
    "category": "TOP",
    "color": "GRAY",
    "material": "KNIT",
    "minTemperature": 3,
    "maxTemperature": 16,
    "rainSuitable": false,
    "archived": false,
    "createdAt": "2026-05-20T10:00:00",
    "updatedAt": "2026-05-20T10:05:00"
  }
}
```

## 8. P1: 옷 보관 처리
`PATCH /api/clothes/{clothingId}/archive?userId={userId}`

옷의 `archived`를 `true`로 변경한다. 이미 `archived=true`여도 idempotent하게 성공 처리한다.

### Response
`200 OK`

```json
{
  "data": {
    "id": 1,
    "userId": 1,
    "archived": true,
    "updatedAt": "2026-05-20T10:10:00"
  }
}
```

## 9. P0: 오늘의 추천 생성
`POST /api/recommendations?userId={userId}`

오늘의 추천을 새로 생성하고 `RecommendationResult`로 DB에 생성 및 저장한다. 추천은 `StaticWeatherProvider`가 제공하는 고정 날씨를 사용한다.

성공 응답에는 날씨 정보, 추천 outfit, 점수 breakdown, 추천 이유, 착용 완료 여부, 생성 시각을 포함한다. `outfit.outer`는 OUTER가 없는 추천에서는 `null`이다. 추천 이유는 3개 이상 5개 이하이다.

### Response
`201 Created`

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
        "name": "Gray Knit",
        "category": "TOP",
        "color": "GRAY",
        "material": "KNIT"
      },
      "bottom": {
        "id": 2,
        "name": "Black Denim",
        "category": "BOTTOM",
        "color": "BLACK",
        "material": "DENIM"
      },
      "outer": {
        "id": 3,
        "name": "Navy Jacket",
        "category": "OUTER",
        "color": "NAVY",
        "material": "NYLON"
      }
    },
    "score": {
      "totalScore": 88,
      "weatherScore": 35,
      "colorScore": 25,
      "wearHistoryScore": 20,
      "recommendationHistoryScore": 8,
      "diversityScore": 0
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

### 추천 실패 응답
추천 실패는 비즈니스 실패이므로 HTTP `422 Unprocessable Entity`로 통일한다.

```json
{
  "code": "OUTER_REQUIRED_BUT_NOT_AVAILABLE",
  "message": "현재 기온에는 아우터가 필요하지만 추천 가능한 아우터가 없습니다.",
  "details": []
}
```

## 10. P0: 추천 결과 착용 완료 처리
`PATCH /api/recommendations/{recommendationId}/worn?userId={userId}`

`recommendationId`와 `userId` 기준으로 추천 결과를 조회한다. 첫 호출 시 `RecommendationResult.worn=true`로 변경하고 `WearHistory`를 생성한다.

이미 `worn=true`인 추천 결과는 idempotent하게 성공 처리하며, `WearHistory`를 중복 생성하지 않는다.

### Response
`200 OK`

```json
{
  "data": {
    "recommendationId": 1,
    "worn": true,
    "wornAt": "2026-05-20T11:00:00"
  }
}
```

## 11. 추천 실패 코드
| Code | HTTP Status | Message |
| --- | --- | --- |
| `NO_TOP_AVAILABLE` | `422 Unprocessable Entity` | 현재 날씨에 입을 수 있는 상의가 없습니다. |
| `NO_BOTTOM_AVAILABLE` | `422 Unprocessable Entity` | 현재 날씨에 입을 수 있는 하의가 없습니다. |
| `NO_WEATHER_SUITABLE_ITEM` | `422 Unprocessable Entity` | 현재 기온에 맞는 옷이 없습니다. |
| `OUTER_REQUIRED_BUT_NOT_AVAILABLE` | `422 Unprocessable Entity` | 현재 기온에는 아우터가 필요하지만 추천 가능한 아우터가 없습니다. |
| `INSUFFICIENT_CLOSET_ITEMS` | `422 Unprocessable Entity` | 추천을 만들기 위해 옷을 더 등록해주세요. |

## 12. 일반 에러 코드
| Code | HTTP Status | Description |
| --- | --- | --- |
| `INVALID_REQUEST` | `400 Bad Request` | request parameter, path variable, body validation 실패 |
| `USER_NOT_FOUND` | `404 Not Found` | `userId`에 해당하는 사용자가 없음 |
| `CLOTHING_NOT_FOUND` | `404 Not Found` | `clothingId`에 해당하는 사용자 소유 옷이 없음 |
| `RECOMMENDATION_NOT_FOUND` | `404 Not Found` | `recommendationId`에 해당하는 사용자 소유 추천 결과가 없음 |
| `INTERNAL_SERVER_ERROR` | `500 Internal Server Error` | 예상하지 못한 서버 오류 |

## 정합성 메모
- PRD, ARCHITECTURE, RECOMMENDATION_RULES와 충돌하는 내용은 없다.
- 추천 생성 API 계약은 `POST /api/recommendations?userId={userId}`를 기준으로 한다.
- 이전 `GET /api/recommendations/today` 표현은 API 계약으로 사용하지 않는다.

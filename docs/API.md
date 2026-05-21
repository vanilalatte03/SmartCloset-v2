# API: SmartCloset 2차 MVP

## 1. 공통 규칙
- `userId`는 인증 대신 request parameter로 전달한다.
- 요청과 응답의 `Content-Type`은 `application/json`이다.
- 날짜/시간은 ISO-8601 문자열로 표현한다.
- enum 값은 대문자 문자열로 주고받는다.
- 성공 응답은 항상 `data` 필드를 가진다.
- 실패 응답은 항상 `code`, `message`, `details` 필드를 가진다.
- 추천 생성 API는 `POST /api/recommendations?userId={userId}`만 사용한다.
- today 추천 GET 경로는 API 계약으로 사용하지 않는다.

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
  "code": "LOCATION_NOT_FOUND",
  "message": "위치를 찾을 수 없습니다.",
  "details": [
    {
      "field": "locationCode",
      "message": "UNKNOWN"
    }
  ]
}
```

`details`는 없을 수 있으며, 항상 배열이다. 각 원소는 `{ "field": "...", "message": "..." }` 형태다.

## 2. API 목록

| Method | Path | Description | Success |
| --- | --- | --- | --- |
| `POST` | `/api/clothes?userId={userId}` | 옷 등록 | `201 Created` |
| `GET` | `/api/clothes?userId={userId}` | 옷 목록 조회 | `200 OK` |
| `GET` | `/api/clothes/{clothingId}?userId={userId}` | 옷 상세 조회 | `200 OK` |
| `PUT` | `/api/clothes/{clothingId}?userId={userId}` | 옷 전체 수정 | `200 OK` |
| `PATCH` | `/api/clothes/{clothingId}/archive?userId={userId}` | 옷 보관 처리 | `200 OK` |
| `GET` | `/api/locations?keyword={keyword}` | 내장 대표 격자 catalog 조회 | `200 OK` |
| `GET` | `/api/users/location?userId={userId}` | 사용자 위치 조회 | `200 OK` |
| `PUT` | `/api/users/location?userId={userId}` | 사용자 위치 선택 | `200 OK` |
| `POST` | `/api/recommendations?userId={userId}` | 추천 생성 및 저장 | `201 Created` |
| `PATCH` | `/api/recommendations/{recommendationId}/worn?userId={userId}` | 추천 결과 착용 완료 처리 | `200 OK` |

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

| Field | Required | Rule |
| --- | --- | --- |
| `name` | yes | blank 불가, 최대 50자 |
| `category` | yes | `TOP`, `BOTTOM`, `OUTER` |
| `color` | yes | `BLACK`, `WHITE`, `GRAY`, `NAVY`, `BLUE`, `BROWN`, `BEIGE`, `RED`, `GREEN`, `YELLOW`, `UNKNOWN` |
| `material` | yes | `COTTON`, `DENIM`, `KNIT`, `WOOL`, `POLYESTER`, `NYLON`, `UNKNOWN` |
| `minTemperature` | yes | 정수 |
| `maxTemperature` | yes | 정수, `minTemperature <= maxTemperature` |
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
  "createdAt": "2026-05-21T10:00:00",
  "updatedAt": "2026-05-21T10:00:00"
}
```

### LocationOptionResponse
```json
{
  "code": "SEOUL",
  "name": "서울특별시",
  "nx": 60,
  "ny": 127
}
```

### UserLocationResponse
```json
{
  "userId": 1,
  "code": "SEOUL",
  "name": "서울특별시",
  "nx": 60,
  "ny": 127,
  "updatedAt": "2026-05-21T10:00:00"
}
```

### UpdateUserLocationRequest
```json
{
  "locationCode": "SEOUL"
}
```

Validation:

| Field | Required | Rule |
| --- | --- | --- |
| `locationCode` | yes | 내장 catalog에 존재하는 code |

### WeatherResponse
추천 응답의 `weather`는 추천 생성 시점에 사용한 내부 `WeatherCondition` snapshot이다.

```json
{
  "temperature": 12,
  "weatherType": "CLOUDY",
  "rainy": false,
  "windy": false
}
```

추천 응답의 `weather`에는 KMA 원본 응답, source, `nx`, `ny`를 직접 포함하지 않는다. 프론트가 현재 위치를 보여줄 때는 사용자 위치 API 응답을 함께 사용한다.

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

## 4. 옷 API

### 옷 등록
`POST /api/clothes?userId={userId}`

Response: `201 Created`

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
    "createdAt": "2026-05-21T10:00:00",
    "updatedAt": "2026-05-21T10:00:00"
  }
}
```

### 옷 목록 조회
`GET /api/clothes?userId={userId}`

기본 조회 대상은 `archived=false`인 옷이다. 정렬은 `id` 오름차순이며 pagination을 제공하지 않는다.

Response: `200 OK`

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
      "createdAt": "2026-05-21T10:00:00",
      "updatedAt": "2026-05-21T10:00:00"
    }
  ]
}
```

### 옷 상세 조회
`GET /api/clothes/{clothingId}?userId={userId}`

`clothingId`와 `userId`가 모두 일치하는 옷만 조회한다. 다른 사용자의 옷은 조회되지 않으며 `CLOTHING_NOT_FOUND`로 응답한다.

### 옷 수정
`PUT /api/clothes/{clothingId}?userId={userId}`

PUT은 전체 수정 기준이다. request body는 등록과 같은 필드를 받는다. `archived`는 수정 API에서 변경하지 않는다.

### 옷 보관 처리
`PATCH /api/clothes/{clothingId}/archive?userId={userId}`

보관 처리는 idempotent하다. 이미 보관된 옷에 다시 호출해도 성공한다.

```json
{
  "data": {
    "id": 1,
    "archived": true
  }
}
```

## 5. 위치 API

### 위치 catalog 조회
`GET /api/locations?keyword={keyword}`

서버 내장 대표 격자 catalog를 반환한다. `keyword`는 선택값이며, 생략하면 전체 목록을 반환한다. 검색은 code 또는 name에 대해 수행한다.

Response: `200 OK`

```json
{
  "data": [
    {
      "code": "SEOUL",
      "name": "서울특별시",
      "nx": 60,
      "ny": 127
    },
    {
      "code": "BUSAN",
      "name": "부산광역시",
      "nx": 98,
      "ny": 76
    }
  ]
}
```

2차 최소 catalog:

| Code | Name | nx | ny |
| --- | --- | ---: | ---: |
| `SEOUL` | 서울특별시 | 60 | 127 |
| `BUSAN` | 부산광역시 | 98 | 76 |
| `DAEGU` | 대구광역시 | 89 | 90 |
| `INCHEON` | 인천광역시 | 55 | 124 |
| `GWANGJU` | 광주광역시 | 58 | 74 |
| `DAEJEON` | 대전광역시 | 67 | 100 |
| `ULSAN` | 울산광역시 | 102 | 84 |
| `SEJONG` | 세종특별자치시 | 66 | 103 |
| `JEJU` | 제주특별자치도 | 52 | 38 |

### 사용자 위치 조회
`GET /api/users/location?userId={userId}`

Response: `200 OK`

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

사용자를 찾을 수 없으면 `USER_NOT_FOUND`로 응답한다. 기존 데이터에 위치가 없으면 애플리케이션에서 서울 기본값으로 backfill한 뒤 응답한다. 이 backfill은 저장을 동반하므로 read-only transaction으로 처리하지 않는다.

### 사용자 위치 선택
`PUT /api/users/location?userId={userId}`

Request:

```json
{
  "locationCode": "BUSAN"
}
```

Response: `200 OK`

```json
{
  "data": {
    "userId": 1,
    "code": "BUSAN",
    "name": "부산광역시",
    "nx": 98,
    "ny": 76,
    "updatedAt": "2026-05-21T10:05:00"
  }
}
```

존재하지 않는 `locationCode`는 `LOCATION_NOT_FOUND`로 응답한다.

## 6. 추천 API

### 추천 생성
`POST /api/recommendations?userId={userId}`

요청 body는 없다. 추천 생성은 사용자 위치의 `nx`, `ny`로 KMA 날씨를 조회한다. fallback이 활성화된 경우 KMA 설정/호출/매핑 실패는 fallback 날씨로 이어진다.

Response: `201 Created`

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
        "name": "아이보리 니트",
        "category": "TOP",
        "color": "WHITE",
        "material": "KNIT"
      },
      "bottom": {
        "id": 2,
        "name": "블랙 데님",
        "category": "BOTTOM",
        "color": "BLACK",
        "material": "DENIM"
      },
      "outer": {
        "id": 3,
        "name": "네이비 코트",
        "category": "OUTER",
        "color": "NAVY",
        "material": "WOOL"
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
    "createdAt": "2026-05-21T10:00:00"
  }
}
```

`outfit.outer`는 날씨 조건에 따라 객체 또는 `null`일 수 있다.

### 추천 결과 착용 완료
`PATCH /api/recommendations/{recommendationId}/worn?userId={userId}`

착용 완료 처리는 idempotent하다.

```json
{
  "data": {
    "recommendationId": 1,
    "worn": true,
    "wornAt": "2026-05-21T11:00:00"
  }
}
```

## 7. Error Codes

| Code | HTTP | Message |
| --- | --- | --- |
| `INVALID_REQUEST` | `400` | 요청 값이 올바르지 않습니다. |
| `USER_NOT_FOUND` | `404` | 사용자를 찾을 수 없습니다. |
| `LOCATION_NOT_FOUND` | `404` | 위치를 찾을 수 없습니다. |
| `CLOTHING_NOT_FOUND` | `404` | 옷을 찾을 수 없습니다. |
| `RECOMMENDATION_NOT_FOUND` | `404` | 추천 결과를 찾을 수 없습니다. |
| `NO_TOP_AVAILABLE` | `422` | 현재 날씨에 입을 수 있는 상의가 없습니다. |
| `NO_BOTTOM_AVAILABLE` | `422` | 현재 날씨에 입을 수 있는 하의가 없습니다. |
| `NO_WEATHER_SUITABLE_ITEM` | `422` | 현재 기온에 맞는 옷이 없습니다. |
| `OUTER_REQUIRED_BUT_NOT_AVAILABLE` | `422` | 현재 기온에는 아우터가 필요하지만 추천 가능한 아우터가 없습니다. |
| `INSUFFICIENT_CLOSET_ITEMS` | `422` | 추천을 만들기 위해 옷을 더 등록해주세요. |
| `INTERNAL_SERVER_ERROR` | `500` | 예상하지 못한 서버 오류가 발생했습니다. |

추천 실패 코드 5종은 비즈니스 실패이므로 `422 Unprocessable Entity`로 응답한다. 외부 KMA API 실패는 `WEATHER_FALLBACK_ENABLED=false` strict KMA mode에서만 `INTERNAL_SERVER_ERROR`로 승격한다.

## 8. KMA 내부 연동 계약
외부 요청은 아래 하나로 제한한다.

```text
GET {KMA_BASE_URL}/getVilageFcst
```

요청 parameter:

| Parameter | Source |
| --- | --- |
| `serviceKey` | `KMA_SERVICE_KEY` |
| `pageNo` | fixed `1` |
| `numOfRows` | fixed `1000` |
| `dataType` | fixed `JSON` |
| `base_date` | KST 기준 최신 제공 가능 발표일자 |
| `base_time` | KST 기준 최신 제공 가능 발표시각 |
| `nx` | 사용자 위치의 `locationNx` |
| `ny` | 사용자 위치의 `locationNy` |

KMA 원본 응답은 SmartCloset API 응답에 그대로 노출하지 않는다.

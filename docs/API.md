# API: SmartCloset MVP4 Contract

이 문서는 MVP4에서 유지하는 SmartCloset API 계약을 설명한다. MVP4는 실사용 UX 개선 범위이며 새 공개 API, DB schema, 추천 규칙을 추가하지 않는다. 단, Today 화면의 현재 날씨 요약을 위해 보호 API `GET /api/weather/current`를 추가한다.

MVP4 프론트엔드는 이 API 계약 위에서 한국어 라벨, 색상 swatch, 소재 chip, 첫 추천 준비 체크리스트, 추천 실패 CTA를 구성한다.

## MVP4 API 결정
- 새 공개 API를 추가하지 않는다.
- 보호 API의 Bearer token 정책을 변경하지 않는다.
- 현재 날씨 요약 보호 API `GET /api/weather/current`를 추가한다.
- 현재 사용자 전용 DTO의 `userId` 비노출 정책을 유지한다.
- `userId` query parameter를 공개 HTTP 계약에 되살리지 않는다.
- today 추천 GET 경로를 추가하지 않는다.
- `GET /api/weather/current`는 today 추천 조회가 아니며 추천 결과를 생성하거나 저장하지 않는다.
- 옷 수정과 보관 처리는 기존 `PUT /api/clothes/{clothingId}`, `PATCH /api/clothes/{clothingId}/archive`를 사용한다.

## 1. 공통 규칙
- 공개 API는 토큰 없이 호출 가능하다.
- 보호 API는 `Authorization: Bearer {accessToken}` header가 필요하다.
- 요청과 응답의 `Content-Type`은 `application/json`이다.
- 날짜/시간은 ISO-8601 문자열로 표현한다.
- enum 값은 대문자 문자열로 주고받는다.
- 성공 응답은 항상 `data` 필드를 가진다.
- 실패 응답은 항상 `code`, `message`, `details` 필드를 가진다.
- 현재 날씨 요약 API는 `GET /api/weather/current`를 사용한다.
- 추천 생성 API는 `POST /api/recommendations`만 사용한다.
- today 추천 GET 경로는 API 계약으로 사용하지 않는다.

### userId 제거 정책
현재 HTTP 계약에서는 `userId`를 query parameter로 받지 않는다. Controller는 인증 principal에서 현재 사용자 id를 얻는다.

현재 사용자 전용 response DTO에서도 `userId` 필드를 제거한다. 옷, 위치, 선호도, 추천 생성, 추천 이력, 착용 완료 응답은 모두 현재 인증 사용자의 리소스임을 전제로 한다.

내부 service/repository는 구현 편의를 위해 `Long userId`를 사용할 수 있지만 HTTP 계약과 프론트 타입에는 노출하지 않는다.

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
  "code": "INVALID_REQUEST",
  "message": "요청 값이 올바르지 않습니다.",
  "details": [
    {
      "field": "email",
      "message": "must be a well-formed email address"
    }
  ]
}
```

`details` 필드는 항상 배열이다. 상세 항목이 없으면 빈 배열 `[]`이며, 각 원소는 `{ "field": "...", "message": "..." }` 형태다.

## 2. API 목록

### 공개 API
| Method | Path | Description | Success |
| --- | --- | --- | --- |
| `POST` | `/api/auth/signup` | 회원가입 | `201 Created` |
| `POST` | `/api/auth/login` | 로그인 | `200 OK` |

### 보호 API
| Method | Path | Description | Success |
| --- | --- | --- | --- |
| `GET` | `/api/users/me` | 현재 사용자 조회 | `200 OK` |
| `GET` | `/api/locations?keyword={keyword}` | 내장 대표 격자 catalog 조회 | `200 OK` |
| `GET` | `/api/users/me/location` | 현재 사용자 위치 조회 | `200 OK` |
| `PUT` | `/api/users/me/location` | 현재 사용자 위치 선택 | `200 OK` |
| `GET` | `/api/users/me/preferences` | 현재 사용자 선호도 조회 | `200 OK` |
| `PUT` | `/api/users/me/preferences` | 현재 사용자 선호도 저장 | `200 OK` |
| `GET` | `/api/weather/current` | 현재 사용자 위치 기준 날씨 요약 조회 | `200 OK` |
| `POST` | `/api/clothes` | 옷 등록 | `201 Created` |
| `GET` | `/api/clothes` | 옷 목록 조회 | `200 OK` |
| `GET` | `/api/clothes/{clothingId}` | 옷 상세 조회 | `200 OK` |
| `PUT` | `/api/clothes/{clothingId}` | 옷 전체 수정 | `200 OK` |
| `PATCH` | `/api/clothes/{clothingId}/archive` | 옷 보관 처리 | `200 OK` |
| `POST` | `/api/recommendations` | 추천 생성 및 저장 | `201 Created` |
| `GET` | `/api/recommendations?limit={limit}` | 추천 이력 조회 | `200 OK` |
| `PATCH` | `/api/recommendations/{recommendationId}/worn` | 추천 결과 착용 완료 처리 | `200 OK` |

## 2.1 MVP4 UX 매핑
API enum 값은 변경하지 않는다. 프론트 화면에서만 사용자 친화적인 한국어 라벨로 변환한다.

| API enum | UI treatment |
| --- | --- |
| `ClothingCategory` | 상의, 하의, 아우터 |
| `ClothingColor` | 한국어 라벨과 색상 swatch |
| `ClothingMaterial` | 한국어 라벨과 소재 chip |
| `WeatherType` | 한국어 날씨 라벨 |

추천 실패 코드는 API 계약으로 유지하고, 프론트에서 아래 메시지와 CTA로 변환한다.

| Code | 사용자 메시지 | CTA |
| --- | --- | --- |
| `NO_TOP_AVAILABLE` | 현재 날씨에 맞는 상의가 부족해요. | 상의 등록하기 |
| `NO_BOTTOM_AVAILABLE` | 현재 날씨에 맞는 하의가 부족해요. | 하의 등록하기 |
| `OUTER_REQUIRED_BUT_NOT_AVAILABLE` | 오늘은 아우터가 필요한 날씨예요. | 아우터 등록하기 |
| `NO_WEATHER_SUITABLE_ITEM` | 현재 기온에 맞는 옷이 부족해요. | 옷장 확인하기 |
| `INSUFFICIENT_CLOSET_ITEMS` | 추천을 만들려면 옷을 더 등록해야 해요. | 빠른 등록하기 |

## 3. 인증 API

### SignupRequest
```json
{
  "email": "demo@example.com",
  "password": "password123!",
  "name": "Demo User"
}
```

| Field | Required | Rule |
| --- | --- | --- |
| `email` | yes | email 형식, unique |
| `password` | yes | blank 불가, 최소 8자 |
| `name` | yes | blank 불가, 최대 50자 |

회원가입 시 서버는 기본 위치와 빈 선호도를 함께 생성한다.

```json
{
  "locationCode": "SEOUL",
  "preferredColors": [],
  "preferredMaterials": [],
  "styleTags": []
}
```

### LoginRequest
```json
{
  "email": "demo@example.com",
  "password": "password123!"
}
```

### AuthResponse
```json
{
  "data": {
    "accessToken": "eyJhbGciOi...",
    "tokenType": "Bearer",
    "user": {
      "email": "demo@example.com",
      "name": "Demo User",
      "role": "USER",
      "createdAt": "2026-05-22T10:00:00",
      "updatedAt": "2026-05-22T10:00:00"
    }
  }
}
```

`AuthResponse.user`는 `GET /api/users/me`의 현재 사용자 응답과 같은 필드를 사용한다.

JWT는 access token 단일 구조로 시작한다. refresh token은 MVP4 범위가 아니다.

JWT access token 정책:

| Item | Value |
| --- | --- |
| Signing algorithm | `HS256` |
| Secret source | `JWT_SECRET` |
| Subject | 현재 사용자 id 문자열 |
| Claims | `email`, `role` |
| Expires in | 2시간 |

만료된 token, 잘못된 서명, 지원하지 않는 token은 보호 API에서 `401`로 실패한다.

## 4. 사용자 API

### 현재 사용자 조회
`GET /api/users/me`

Response: `200 OK`

```json
{
  "data": {
    "email": "demo@example.com",
    "name": "Demo User",
    "role": "USER",
    "createdAt": "2026-05-22T10:00:00",
    "updatedAt": "2026-05-22T10:00:00"
  }
}
```

## 5. 선호도 API

### UserPreferencesResponse
```json
{
  "preferredColors": ["NAVY", "BLACK"],
  "preferredMaterials": ["COTTON"],
  "styleTags": ["MINIMAL", "CASUAL"]
}
```

### UpdateUserPreferencesRequest
```json
{
  "preferredColors": ["NAVY", "BLACK"],
  "preferredMaterials": ["COTTON"],
  "styleTags": ["MINIMAL", "CASUAL"]
}
```

| Field | Required | Rule |
| --- | --- | --- |
| `preferredColors` | yes | `ClothingColor` 배열, 중복 제거 권장 |
| `preferredMaterials` | yes | `ClothingMaterial` 배열, 중복 제거 권장 |
| `styleTags` | yes | 문자열 배열, blank 불가, 각 항목 최대 30자 |

신규 사용자의 기본값은 모두 빈 배열이다.

```json
{
  "data": {
    "preferredColors": [],
    "preferredMaterials": [],
    "styleTags": []
  }
}
```

`styleTags`는 저장/조회/표시만 하며 `preferenceScore`와 추천 이유에는 반영하지 않는다.

## 6. 옷 API

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
  "name": "Gray Knit",
  "category": "TOP",
  "color": "GRAY",
  "material": "KNIT",
  "minTemperature": 5,
  "maxTemperature": 18,
  "rainSuitable": false,
  "archived": false,
  "createdAt": "2026-05-22T10:00:00",
  "updatedAt": "2026-05-22T10:00:00"
}
```

### 옷 등록
`POST /api/clothes`

Response: `201 Created`

```json
{
  "data": {
    "id": 1,
    "name": "Gray Knit",
    "category": "TOP",
    "color": "GRAY",
    "material": "KNIT",
    "minTemperature": 5,
    "maxTemperature": 18,
    "rainSuitable": false,
    "archived": false,
    "createdAt": "2026-05-22T10:00:00",
    "updatedAt": "2026-05-22T10:00:00"
  }
}
```

### 옷 목록 조회
`GET /api/clothes`

기본 조회 대상은 현재 인증 사용자의 `archived=false`인 옷이다. 정렬은 `id` 오름차순이며 pagination을 제공하지 않는다.

### 옷 상세 조회
`GET /api/clothes/{clothingId}`

현재 인증 사용자가 소유한 옷만 조회한다. 다른 사용자의 옷은 조회되지 않으며 `CLOTHING_NOT_FOUND`로 응답한다.

### 옷 수정
`PUT /api/clothes/{clothingId}`

PUT은 전체 수정 기준이다. request body는 등록과 같은 필드를 받는다. `archived`는 수정 API에서 변경하지 않는다.

### 옷 보관 처리
`PATCH /api/clothes/{clothingId}/archive`

보관 처리는 idempotent하다. 이미 보관된 옷에 다시 호출해도 성공한다.

```json
{
  "data": {
    "id": 1,
    "archived": true
  }
}
```

## 7. 위치 API

### LocationOptionResponse
```json
{
  "code": "SEOUL",
  "name": "서울특별시",
  "nx": 60,
  "ny": 127
}
```

### 위치 catalog 조회
`GET /api/locations?keyword={keyword}`

보호 API다. 회원가입 화면에서는 호출하지 않는다. 로그인 후 위치 선택 화면에서만 사용한다. 인증이 만료되어 `401`이 반환되면 프론트는 위치 검색 실패가 아니라 로그인 만료로 처리한다.

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
    }
  ]
}
```

### 현재 사용자 위치 조회
`GET /api/users/me/location`

Response: `200 OK`

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

기존 데이터에 위치가 없으면 애플리케이션에서 서울 기본값으로 backfill한 뒤 응답한다. 이 backfill은 저장을 동반하므로 read-only transaction으로 처리하지 않는다.

### 현재 사용자 위치 선택
`PUT /api/users/me/location`

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
    "code": "BUSAN",
    "name": "부산광역시",
    "nx": 98,
    "ny": 76,
    "updatedAt": "2026-05-22T10:05:00"
  }
}
```

존재하지 않는 `locationCode`는 `LOCATION_NOT_FOUND`로 응답한다.

## 8. 현재 날씨 요약 API

### 현재 날씨 요약 조회
`GET /api/weather/current`

보호 API다. 현재 인증 사용자의 저장 위치 `nx`, `ny`로 KMA `getVilageFcst` JSON을 조회하거나, fallback이 활성화된 경우 fallback 날씨를 반환한다.

weather provider는 현재 사용자 id, 위치 code/nx/ny, 요청 시점의 KMA base date/time, 서비스키 설정 여부, fallback enabled 여부가 같은 경우 2분 TTL 인메모리 snapshot을 재사용할 수 있다. 이 cache는 DB에 저장되지 않으며 response shape를 바꾸지 않는다.

이 API는 추천 결과를 생성하거나 저장하지 않는다. 추천 이력, 착용 이력, 점수 계산에는 영향을 주지 않는다.

Response: `200 OK`

```json
{
  "data": {
    "temperature": 12,
    "weatherType": "CLOUDY",
    "rainy": false,
    "windy": false
  }
}
```

인증이 없거나 만료되면 `401`로 실패한다. `WEATHER_FALLBACK_ENABLED=false` strict KMA mode에서 KMA 설정/호출/매핑에 실패하면 기존 weather provider 규칙대로 `INTERNAL_SERVER_ERROR`로 실패한다.

## 9. 추천 API

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

### RecommendationScoreResponse
```json
{
  "totalScore": 88,
  "weatherScore": 35,
  "colorScore": 25,
  "wearHistoryScore": 20,
  "recommendationHistoryScore": 8,
  "preferenceScore": 0
}
```

`preferenceScore`는 선호 색상/소재만 반영한다. `styleTags`는 추천 점수와 추천 이유에 반영하지 않는다.

### 추천 생성
`POST /api/recommendations`

요청 body는 없다. 추천 생성은 현재 인증 사용자 위치의 `nx`, `ny`로 KMA 날씨를 조회한다. fallback이 활성화된 경우 KMA 설정/호출/매핑 실패는 fallback 날씨로 이어진다.

추천 생성도 같은 `WeatherProvider`를 사용하므로, 같은 위치와 같은 KMA base date/time의 짧은 사용 흐름에서는 `GET /api/weather/current`와 같은 weather snapshot을 재사용할 수 있다.

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
      "preferenceScore": 10
    },
    "reasons": [
      "현재 기온이 낮아 아우터를 포함한 조합을 추천했습니다.",
      "상의와 하의 색상이 무채색 중심이라 안정적인 조합입니다.",
      "선호 색상 또는 소재와 맞는 옷이 포함되어 있습니다."
    ],
    "worn": false,
    "createdAt": "2026-05-22T10:00:00"
  }
}
```

`outfit.outer`는 날씨 조건에 따라 객체 또는 `null`일 수 있다.

### 추천 이력 조회
`GET /api/recommendations?limit={limit}`

현재 인증 사용자의 추천 결과를 최신순으로 반환한다.

Limit 정책:

| Rule | Value |
| --- | --- |
| 기본값 | `20` |
| 최소값 | `1` |
| 최대값 | `50` |
| 정렬 | 최신순 |
| invalid value | `400 INVALID_REQUEST` |

Response: `200 OK`

```json
{
  "data": [
    {
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
        "totalScore": 88,
        "weatherScore": 35,
        "colorScore": 25,
        "wearHistoryScore": 20,
        "recommendationHistoryScore": 8,
        "preferenceScore": 0
      },
      "reasons": [
        "현재 기온이 낮아 아우터를 포함한 조합을 추천했습니다.",
        "상의와 하의 색상 조합이 안정적입니다.",
        "최근 착용 이력이 적어 반복 착용 부담이 낮습니다."
      ],
      "worn": false,
      "createdAt": "2026-05-22T10:00:00"
    }
  ]
}
```

### 추천 결과 착용 완료
`PATCH /api/recommendations/{recommendationId}/worn`

착용 완료 처리는 idempotent하다. 현재 인증 사용자의 추천 결과만 처리할 수 있다.

```json
{
  "data": {
    "recommendationId": 1,
    "worn": true,
    "wornAt": "2026-05-22T11:00:00"
  }
}
```

## 10. Error Codes

| Code | HTTP | Message |
| --- | --- | --- |
| `INVALID_REQUEST` | `400` | 요청 값이 올바르지 않습니다. |
| `UNAUTHORIZED` | `401` | 인증이 필요합니다. |
| `INVALID_TOKEN` | `401` | 인증 토큰이 올바르지 않습니다. |
| `FORBIDDEN` | `403` | 접근 권한이 없습니다. |
| `EMAIL_ALREADY_EXISTS` | `409` | 이미 사용 중인 이메일입니다. |
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

## 11. KMA 내부 연동 계약
외부 Weather 요청은 아래 하나로 제한한다.

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
| `nx` | 현재 인증 사용자 위치의 `locationNx` |
| `ny` | 현재 인증 사용자 위치의 `locationNy` |

KMA 원본 응답은 SmartCloset API 응답에 그대로 노출하지 않는다.

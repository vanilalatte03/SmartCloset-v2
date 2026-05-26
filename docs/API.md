# API: SmartCloset MVP7 Contract

이 문서는 SmartCloset MVP7 API 계약을 설명한다. MVP7은 기존 인증 사용자 API, MVP5 이미지 API, MVP6 피드백/개인화 API 위에 KMA 위치 catalog 확장, 좌표 resolve, 예보 시간대 선택, 위치/날씨 source snapshot을 추가한다.

## MVP7 API 결정

- 새 공개 API를 추가하지 않는다.
- 공개 `userId` query parameter를 추가하지 않는다.
- 현재 사용자 전용 response DTO에 `userId`를 노출하지 않는다.
- `GET /api/locations?keyword={keyword}`는 KMA 행정구역 catalog 검색으로 확장한다.
- `POST /api/locations/resolve`는 브라우저 좌표를 KMA grid와 위치 후보로 변환한다.
- 브라우저 좌표 원문은 저장하지 않는다.
- `PUT /api/users/me/location`은 optional `source`를 받을 수 있다.
- `POST /api/recommendations`는 optional `forecastPeriod`를 받을 수 있다.
- `WeatherResponse`는 `location`과 `source`를 포함한다.
- 추천 결과와 이력은 추천 생성 당시 위치/날씨 source snapshot을 반환한다.
- raw KMA 응답 JSON은 저장하거나 응답하지 않는다.

## 1. 공통 규칙

- 공개 API는 토큰 없이 호출 가능하다.
- 보호 API는 `Authorization: Bearer {accessToken}` header가 필요하다.
- JSON API 요청과 응답의 `Content-Type`은 `application/json`이다.
- 이미지 업로드 요청은 `multipart/form-data`다.
- 이미지 bytes 조회 응답은 이미지 MIME type을 `Content-Type`으로 반환한다.
- 날짜/시간은 ISO-8601 문자열로 표현한다.
- KMA `baseDate`, `baseTime`, `forecastDate`, `forecastTime`은 KMA 요청/응답 형식 문자열을 유지한다.
- enum 값은 대문자 문자열로 주고받는다.
- JSON 성공 응답은 항상 `data` 필드를 가진다.
- JSON 실패 응답은 항상 `code`, `message`, `details` 필드를 가진다.
- `details`는 항상 배열이다.

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
      "field": "forecastPeriod",
      "message": "지원하지 않는 예보 시간대입니다."
    }
  ]
}
```

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
| `GET` | `/api/locations?keyword={keyword}` | KMA 행정구역 catalog 검색 | `200 OK` |
| `POST` | `/api/locations/resolve` | 브라우저 좌표를 KMA grid와 위치 후보로 변환 | `200 OK` |
| `GET` | `/api/users/me/location` | 현재 사용자 위치 조회 | `200 OK` |
| `PUT` | `/api/users/me/location` | 현재 사용자 위치 선택 | `200 OK` |
| `GET` | `/api/users/me/preferences` | 현재 사용자 선호도 조회 | `200 OK` |
| `PUT` | `/api/users/me/preferences` | 현재 사용자 선호도 저장 | `200 OK` |
| `GET` | `/api/weather/current` | 현재 사용자 위치 기준 날씨 요약과 source 조회 | `200 OK` |
| `POST` | `/api/clothes` | 옷 등록 | `201 Created` |
| `GET` | `/api/clothes` | 옷 목록 조회 | `200 OK` |
| `GET` | `/api/clothes/{clothingId}` | 옷 상세 조회 | `200 OK` |
| `PUT` | `/api/clothes/{clothingId}` | 옷 전체 수정 | `200 OK` |
| `PATCH` | `/api/clothes/{clothingId}/archive` | 옷 보관 처리 | `200 OK` |
| `PUT` | `/api/clothes/{clothingId}/image` | 옷 이미지 업로드 또는 교체 | `200 OK` |
| `GET` | `/api/clothes/{clothingId}/image` | 옷 이미지 bytes 조회 | `200 OK` |
| `DELETE` | `/api/clothes/{clothingId}/image` | 옷 이미지 삭제 | `200 OK` |
| `POST` | `/api/recommendations` | 상황/예보 시간대 기반 추천 생성 및 저장 | `201 Created` |
| `GET` | `/api/recommendations?limit={limit}` | 추천 이력 조회 | `200 OK` |
| `PATCH` | `/api/recommendations/{recommendationId}/worn` | 추천 결과 착용 완료 처리 | `200 OK` |
| `PUT` | `/api/recommendations/{recommendationId}/feedback` | 추천 피드백 전체 교체 또는 clear | `200 OK` |

## 3. 인증 API

### SignupRequest

```json
{
  "email": "demo@example.com",
  "password": "password123!",
  "name": "Demo User"
}
```

회원가입 시 서버는 기본 위치 `SEOUL`, 위치 source `MANUAL_SEARCH`, 빈 선호도, 기본 옷 프리셋 5개를 함께 생성한다.

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

## 4. 위치 API

### LocationSource

| Value | Description |
| --- | --- |
| `MANUAL_SEARCH` | 사용자가 검색 결과에서 직접 선택 |
| `BROWSER_GEOLOCATION` | 브라우저 좌표 resolve 후보에서 선택 |

### LocationOptionResponse

```json
{
  "code": "KMA_4128751000",
  "name": "일산1동",
  "fullName": "경기도 고양시일산서구 일산1동",
  "region1": "경기도",
  "region2": "고양시일산서구",
  "region3": "일산1동",
  "nx": 56,
  "ny": 129,
  "latitude": 37.6843,
  "longitude": 126.7707
}
```

규칙:

- `code`는 catalog 안에서 stable unique 값이어야 한다.
- `name`은 선택 화면의 짧은 표시명이다.
- `fullName`은 동명이인 후보 구분을 위한 전체 표시명이다.
- `region3`은 시/군/구 단위 row처럼 3단계가 없는 경우 `null`일 수 있다.
- `latitude`, `longitude`는 catalog 원천 데이터에 없으면 `null`일 수 있다.

### 위치 검색

`GET /api/locations?keyword={keyword}`

- 보호 API다.
- keyword가 없거나 blank면 기본 또는 상위 후보 목록을 반환할 수 있다.
- keyword는 code, fullName, region1, region2, region3에 대해 검색한다.
- `일산동`처럼 동명이인이 있으면 여러 후보를 반환한다.
- 외부 지도/주소 API를 호출하지 않는다.

성공 응답:

```json
{
  "data": [
    {
      "code": "KMA_4128751000",
      "name": "일산1동",
      "fullName": "경기도 고양시일산서구 일산1동",
      "region1": "경기도",
      "region2": "고양시일산서구",
      "region3": "일산1동",
      "nx": 56,
      "ny": 129,
      "latitude": 37.6843,
      "longitude": 126.7707
    }
  ]
}
```

### ResolveLocationRequest

```json
{
  "latitude": 37.6843,
  "longitude": 126.7707
}
```

Validation:

| Field | Rule |
| --- | --- |
| `latitude` | `-90` 이상 `90` 이하 |
| `longitude` | `-180` 이상 `180` 이하 |

### LocationResolveResponse

```json
{
  "grid": {
    "nx": 56,
    "ny": 129
  },
  "nearest": {
    "code": "KMA_4128751000",
    "name": "일산1동",
    "fullName": "경기도 고양시일산서구 일산1동",
    "region1": "경기도",
    "region2": "고양시일산서구",
    "region3": "일산1동",
    "nx": 56,
    "ny": 129,
    "latitude": 37.6843,
    "longitude": 126.7707
  },
  "candidates": [
    {
      "code": "KMA_4128751000",
      "name": "일산1동",
      "fullName": "경기도 고양시일산서구 일산1동",
      "region1": "경기도",
      "region2": "고양시일산서구",
      "region3": "일산1동",
      "nx": 56,
      "ny": 129,
      "latitude": 37.6843,
      "longitude": 126.7707
    }
  ]
}
```

규칙:

- 서버는 KMA 공식 변환식으로 latitude/longitude를 nx/ny로 변환한다.
- `nearest`는 `candidates` 중 가장 가까운 후보다.
- resolve 요청의 좌표 원문은 DB에 저장하지 않는다.
- 사용자가 후보를 선택해 `PUT /api/users/me/location`을 호출해야 위치가 저장된다.

### UserLocationResponse

```json
{
  "code": "KMA_4128751000",
  "name": "일산1동",
  "fullName": "경기도 고양시일산서구 일산1동",
  "region1": "경기도",
  "region2": "고양시일산서구",
  "region3": "일산1동",
  "nx": 56,
  "ny": 129,
  "source": "BROWSER_GEOLOCATION",
  "updatedAt": "2026-05-26T10:00:00"
}
```

### UpdateUserLocationRequest

```json
{
  "locationCode": "KMA_4128751000",
  "source": "BROWSER_GEOLOCATION"
}
```

규칙:

- `source`는 optional이며 누락 시 `MANUAL_SEARCH`다.
- 존재하지 않는 `locationCode`는 `INVALID_REQUEST`다.
- 현재 사용자 위치만 수정한다.

## 5. 옷 API

MVP5 이미지 API와 MVP6 `styleTags` 계약을 유지한다.

### ClothingRequest

```json
{
  "name": "Gray Knit",
  "category": "TOP",
  "color": "GRAY",
  "material": "KNIT",
  "minTemperature": 5,
  "maxTemperature": 18,
  "rainSuitable": false,
  "styleTags": ["MINIMAL", "OFFICE", "미니멀"]
}
```

### ClothingImageResponse

```json
{
  "url": "/api/clothes/1/image",
  "contentType": "image/jpeg",
  "sizeBytes": 123456,
  "uploadedAt": "2026-05-25T10:00:00"
}
```

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
  "styleTags": ["MINIMAL", "OFFICE", "미니멀"],
  "archived": false,
  "image": null,
  "createdAt": "2026-05-22T10:00:00",
  "updatedAt": "2026-05-25T10:00:00"
}
```

## 6. 날씨 API

### ForecastPeriod

| Value | Description |
| --- | --- |
| `CURRENT` | 현재 시각 이후 가장 가까운 예보 |
| `MORNING` | 오늘 오전 추천 기준 예보 |
| `AFTERNOON` | 오늘 오후 추천 기준 예보 |
| `EVENING` | 오늘 저녁 추천 기준 예보 |

시간대 대표 forecast target은 구현에서 고정한다. 권장 기본값은 `MORNING=0900`, `AFTERNOON=1500`, `EVENING=2100`이다. 해당 시각 예보가 없으면 같은 날짜의 가장 가까운 이후 예보, 없으면 가장 가까운 이전 예보를 사용한다.

### WeatherLocationSnapshotResponse

```json
{
  "code": "KMA_4128751000",
  "name": "일산1동",
  "fullName": "경기도 고양시일산서구 일산1동",
  "nx": 56,
  "ny": 129,
  "source": "BROWSER_GEOLOCATION"
}
```

### WeatherSourceResponse

```json
{
  "provider": "KMA_VILAGE_FORECAST",
  "kmaUsed": true,
  "fallbackUsed": false,
  "baseDate": "20260526",
  "baseTime": "0800",
  "forecastDate": "20260526",
  "forecastTime": "1500"
}
```

규칙:

- `provider`는 `KMA_VILAGE_FORECAST` 또는 `STATIC_FALLBACK`이다.
- `kmaUsed=true`이면 KMA `getVilageFcst` 결과에서 내부 `WeatherCondition`을 만들었다.
- `fallbackUsed=true`이면 fallback weather를 사용했다.
- fallback 시에도 계산 가능한 base/forecast 시각은 표시할 수 있다.
- raw KMA 응답 JSON은 응답하지 않는다.

### WeatherResponse

```json
{
  "temperature": 12,
  "weatherType": "CLOUDY",
  "rainy": false,
  "windy": false,
  "location": {
    "code": "KMA_4128751000",
    "name": "일산1동",
    "fullName": "경기도 고양시일산서구 일산1동",
    "nx": 56,
    "ny": 129,
    "source": "BROWSER_GEOLOCATION"
  },
  "source": {
    "provider": "KMA_VILAGE_FORECAST",
    "kmaUsed": true,
    "fallbackUsed": false,
    "baseDate": "20260526",
    "baseTime": "0800",
    "forecastDate": "20260526",
    "forecastTime": "1500"
  }
}
```

`GET /api/weather/current`는 인증 사용자 위치와 `CURRENT` 기준 source를 반환하며 추천 결과, 추천 이력, 착용 이력, 피드백을 생성하거나 변경하지 않는다.

## 7. 추천 API

### RecommendationSituation

| Value | Label |
| --- | --- |
| `WORK` | 출근 |
| `CASUAL` | 캐주얼 |
| `WORKOUT` | 운동 |
| `DATE` | 데이트 |
| `FORMAL` | 격식 |

### RecommendationRequest

`POST /api/recommendations` 요청 body는 선택이다. body가 없거나 `situation`이 누락되면 `CASUAL`, `forecastPeriod`가 누락되면 `CURRENT`다.

```json
{
  "situation": "WORK",
  "forecastPeriod": "AFTERNOON"
}
```

### RecommendationResponse

```json
{
  "recommendationId": 10,
  "situation": "WORK",
  "forecastPeriod": "AFTERNOON",
  "weather": {
    "temperature": 12,
    "weatherType": "CLOUDY",
    "rainy": false,
    "windy": false,
    "location": {
      "code": "KMA_4128751000",
      "name": "일산1동",
      "fullName": "경기도 고양시일산서구 일산1동",
      "nx": 56,
      "ny": 129,
      "source": "BROWSER_GEOLOCATION"
    },
    "source": {
      "provider": "KMA_VILAGE_FORECAST",
      "kmaUsed": true,
      "fallbackUsed": false,
      "baseDate": "20260526",
      "baseTime": "0800",
      "forecastDate": "20260526",
      "forecastTime": "1500"
    }
  },
  "outfit": {
    "top": {
      "id": 1,
      "name": "Gray Knit",
      "category": "TOP",
      "color": "GRAY",
      "material": "KNIT",
      "styleTags": ["MINIMAL", "OFFICE"],
      "image": null
    },
    "bottom": {
      "id": 2,
      "name": "Black Denim",
      "category": "BOTTOM",
      "color": "BLACK",
      "material": "DENIM",
      "styleTags": ["CASUAL"],
      "image": null
    },
    "outer": null
  },
  "score": {
    "totalScore": 83,
    "weatherScore": 35,
    "colorScore": 24,
    "wearHistoryScore": 20,
    "recommendationHistoryScore": 7,
    "preferenceScore": 7
  },
  "reasons": [
    "오후 예보 기준 12도에 맞는 조합이에요.",
    "출근 상황에 맞는 단정한 태그를 반영했어요."
  ],
  "worn": true,
  "wornAt": "2026-05-26T10:00:00",
  "feedback": {
    "sentiment": "LIKED",
    "thermal": "TOO_COLD",
    "updatedAt": "2026-05-26T10:05:00"
  },
  "createdAt": "2026-05-26T09:00:00"
}
```

`wornAt`과 `feedback`은 nullable이다.

### 추천 생성

`POST /api/recommendations`

- 현재 인증 사용자 위치 snapshot을 읽는다.
- `forecastPeriod`에 맞는 weather snapshot을 만든다.
- 현재 인증 사용자 옷장, 선호도, 최근 착용/추천/피드백 이력을 사용한다.
- 추천 결과, 상황, 예보 시간대, 위치/날씨 source snapshot을 저장한다.
- 성공 시 `201 Created`와 `RecommendationResponse`를 반환한다.
- 추천 business failure는 `422 Unprocessable Entity`를 사용한다.

### 추천 이력 조회

`GET /api/recommendations?limit={limit}`

- 기본 `limit=20`
- 최소 `1`, 최대 `50`
- 최신순
- 각 항목은 `situation`, `forecastPeriod`, `weather.location`, `weather.source`, `worn`, `wornAt`, `feedback`을 포함한다.

### 추천 피드백 전체 교체

MVP6 계약을 유지한다.

`PUT /api/recommendations/{recommendationId}/feedback`

```json
{
  "sentiment": "LIKED",
  "thermal": "TOO_COLD"
}
```

- 현재 사용자 소유 추천만 수정 가능하다.
- PUT은 전체 교체다.
- 누락 필드는 `null`로 간주한다.
- `{}`는 피드백 clear다.

## 8. 에러 코드

| Code | HTTP | Description |
| --- | --- | --- |
| `INVALID_REQUEST` | `400` | 요청 값이 올바르지 않습니다. |
| `UNAUTHORIZED` | `401` | 인증이 필요합니다. |
| `FORBIDDEN` | `403` | 접근 권한이 없습니다. |
| `USER_NOT_FOUND` | `404` | 사용자를 찾을 수 없습니다. |
| `CLOTHING_NOT_FOUND` | `404` | 옷을 찾을 수 없습니다. |
| `CLOTHING_IMAGE_NOT_FOUND` | `404` | 옷 이미지를 찾을 수 없습니다. |
| `RECOMMENDATION_NOT_FOUND` | `404` | 추천 결과를 찾을 수 없습니다. |
| `NO_TOP_AVAILABLE` | `422` | 추천 가능한 상의가 없습니다. |
| `NO_BOTTOM_AVAILABLE` | `422` | 추천 가능한 하의가 없습니다. |
| `OUTER_REQUIRED_BUT_NOT_AVAILABLE` | `422` | 아우터가 필요한 날씨지만 추천 가능한 아우터가 없습니다. |
| `NO_WEATHER_SUITABLE_ITEM` | `422` | 현재 날씨에 맞는 옷이 없습니다. |
| `INSUFFICIENT_CLOSET_ITEMS` | `422` | 추천을 만들 옷이 부족합니다. |
| `INTERNAL_SERVER_ERROR` | `500` | 서버 오류가 발생했습니다. |

## 9. 프론트 API 주의

- 로그인 전 보호 API를 호출하지 않는다.
- 브라우저 Geolocation API는 사용자 클릭 뒤에만 호출한다.
- resolve 결과는 자동 저장하지 않고 사용자가 후보를 선택하게 한다.
- protected image URL은 Authorization header가 필요하므로 blob fetch로 조회한다.
- `POST /api/recommendations`는 body 없이 호출 가능해야 한다.
- feedback clear는 `{}` 또는 양쪽 `null` body로 처리한다.

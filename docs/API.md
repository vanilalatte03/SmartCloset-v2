# API: SmartCloset MVP6 Contract

이 문서는 SmartCloset MVP6 API 계약을 설명한다. MVP6는 기존 인증 사용자 API와 MVP5 이미지 API 위에 추천 상황, 옷별 `styleTags`, 추천 피드백 snapshot을 추가한다.

## MVP6 API 결정

- 새 공개 API를 추가하지 않는다.
- 공개 `userId` query parameter를 추가하지 않는다.
- 현재 사용자 전용 response DTO에 `userId`를 노출하지 않는다.
- 기존 옷 등록/수정 JSON API는 유지하고 `styleTags` 필드를 추가한다.
- `POST /api/recommendations`는 선택 JSON body를 받을 수 있다.
- 추천 상황 body가 없거나 `situation`이 누락되면 `CASUAL`로 처리한다.
- 추천 피드백은 `PUT /api/recommendations/{recommendationId}/feedback` 보호 API로 저장한다.
- 추천 피드백은 추천 결과별 최신 상태 snapshot이며 이벤트 로그를 만들지 않는다.
- 피드백 PUT은 전체 교체다. 누락 필드는 `null`로 간주하고 양쪽 `null`이면 clear한다.
- 이미지 API는 MVP5 보호 API 계약을 유지한다.

## 1. 공통 규칙

- 공개 API는 토큰 없이 호출 가능하다.
- 보호 API는 `Authorization: Bearer {accessToken}` header가 필요하다.
- JSON API 요청과 응답의 `Content-Type`은 `application/json`이다.
- 이미지 업로드 요청은 `multipart/form-data`다.
- 이미지 bytes 조회 응답은 이미지 MIME type을 `Content-Type`으로 반환한다.
- 날짜/시간은 ISO-8601 문자열로 표현한다.
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
      "field": "situation",
      "message": "지원하지 않는 추천 상황입니다."
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
| `PUT` | `/api/clothes/{clothingId}/image` | 옷 이미지 업로드 또는 교체 | `200 OK` |
| `GET` | `/api/clothes/{clothingId}/image` | 옷 이미지 bytes 조회 | `200 OK` |
| `DELETE` | `/api/clothes/{clothingId}/image` | 옷 이미지 삭제 | `200 OK` |
| `POST` | `/api/recommendations` | 상황 기반 추천 생성 및 저장 | `201 Created` |
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

회원가입 시 서버는 기본 위치 `SEOUL`, 빈 선호도, 기본 옷 프리셋 5개를 함께 생성한다.

### LoginRequest

```json
{
  "email": "demo@example.com",
  "password": "password123!"
}
```

로그인 시 현재 사용자 옷이 0개이면 서버는 기본 옷 프리셋 5개를 한 번만 생성한다.

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

JWT access token 정책:

| Item | Value |
| --- | --- |
| Signing algorithm | `HS256` |
| Secret source | `JWT_SECRET` |
| Subject | 현재 사용자 id 문자열 |
| Claims | `email`, `role` |
| Expires in | 2시간 |

## 4. 옷 API

### ClothingRequest

옷 등록과 옷 전체 수정에서 같은 JSON 요청 필드를 사용한다. 이미지 파일은 포함하지 않는다.

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

`styleTags` 규칙:

- Type contract: `styleTags: string[]`.
- 요청에서 누락되면 빈 배열 `[]`로 처리한다.
- 응답은 항상 배열을 반환한다.
- blank tag는 저장하지 않는다.
- tag는 trim 후 저장한다.
- 중복 tag는 제거한다.
- 단일 tag 최대 길이는 30자다.

### ClothingImageResponse

```json
{
  "url": "/api/clothes/1/image",
  "contentType": "image/jpeg",
  "sizeBytes": 123456,
  "uploadedAt": "2026-05-25T10:00:00"
}
```

이미지가 없으면 `image`는 `null`이다.

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
  "image": {
    "url": "/api/clothes/1/image",
    "contentType": "image/jpeg",
    "sizeBytes": 123456,
    "uploadedAt": "2026-05-25T10:00:00"
  },
  "createdAt": "2026-05-22T10:00:00",
  "updatedAt": "2026-05-25T10:00:00"
}
```

`ClothingResponse`는 현재 사용자 전용 응답이므로 `userId`를 노출하지 않는다.

### 옷 목록 조회

`GET /api/clothes`

- 현재 인증 사용자의 `archived=false` 옷만 반환한다.
- 정렬은 `id` 오름차순을 유지한다.
- 각 옷의 `image`는 nullable이다.
- 각 옷의 `styleTags`는 항상 배열이다.

### 기본 옷 프리셋

서버는 신규 가입자와 옷이 0개인 기존 로그인 사용자에게 기본 프리셋을 현재 사용자 소유 옷으로 생성한다.

| Name | Category | Color | Material | Style tags |
| --- | --- | --- | --- | --- |
| 화이트 반팔 티셔츠 | `TOP` | `WHITE` | `COTTON` | `CASUAL`, `DAILY`, `캐주얼` |
| 블랙 반팔 티셔츠 | `TOP` | `BLACK` | `COTTON` | `CASUAL`, `MINIMAL`, `미니멀` |
| 흑청 데님 팬츠 | `BOTTOM` | `BLACK` | `DENIM` | `CASUAL`, `DAILY`, `데일리` |
| 진청 데님 팬츠 | `BOTTOM` | `BLUE` | `DENIM` | `CASUAL`, `DAILY`, `데일리` |
| 블랙 가디건 | `OUTER` | `BLACK` | `KNIT` | `MINIMAL`, `OFFICE`, `미니멀` |

## 5. 옷 이미지 API

이미지 API는 MVP5 계약을 유지한다.

### 업로드 또는 교체

`PUT /api/clothes/{clothingId}/image`

- `Content-Type: multipart/form-data`
- part name: `image`
- file: jpg/jpeg/png/webp, 최대 5MB
- 현재 인증 사용자 소유 옷만 수정 가능

### 이미지 조회

`GET /api/clothes/{clothingId}/image`

- 인증과 소유권 확인 후 image content type과 bytes를 반환한다.
- 다른 사용자 옷 또는 존재하지 않는 옷은 `CLOTHING_NOT_FOUND`다.
- 내 옷이지만 이미지가 없으면 `CLOTHING_IMAGE_NOT_FOUND`다.

### 이미지 삭제

`DELETE /api/clothes/{clothingId}/image`

- idempotent하다.
- 이미지가 이미 없어도 성공한다.

## 6. 추천 API

### RecommendationSituation

| Value | Label |
| --- | --- |
| `WORK` | 출근 |
| `CASUAL` | 캐주얼 |
| `WORKOUT` | 운동 |
| `DATE` | 데이트 |
| `FORMAL` | 격식 |

### RecommendationRequest

`POST /api/recommendations` 요청 body는 선택이다. body가 없거나 `situation`이 누락되면 `CASUAL`이다.

```json
{
  "situation": "WORK"
}
```

### OutfitItemResponse

```json
{
  "id": 1,
  "name": "Gray Knit",
  "category": "TOP",
  "color": "GRAY",
  "material": "KNIT",
  "styleTags": ["MINIMAL", "OFFICE"],
  "image": {
    "url": "/api/clothes/1/image",
    "contentType": "image/jpeg",
    "sizeBytes": 123456,
    "uploadedAt": "2026-05-25T10:00:00"
  }
}
```

### RecommendationFeedbackStateResponse

피드백이 있는 경우:

```json
{
  "sentiment": "LIKED",
  "thermal": "TOO_COLD",
  "updatedAt": "2026-05-26T10:05:00"
}
```

피드백이 없거나 clear된 경우 `feedback`은 `null`이다.

### RecommendationFeedbackResponse

`PUT /api/recommendations/{recommendationId}/feedback` 성공 응답의 `data`는 아래 wrapper 형태다.

```json
{
  "recommendationId": 10,
  "feedback": {
    "sentiment": "LIKED",
    "thermal": "TOO_COLD",
    "updatedAt": "2026-05-26T10:05:00"
  }
}
```

### RecommendationResponse

```json
{
  "recommendationId": 10,
  "situation": "WORK",
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
    "출근 상황에 맞는 단정한 태그를 반영했어요.",
    "최근 마음에 든 조합과 일부 겹쳐 선호를 반영했어요."
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

- 현재 인증 사용자 위치의 현재 날씨를 조회한다.
- 현재 인증 사용자 옷장, 선호도, 최근 착용/추천/피드백 이력을 사용한다.
- 추천 결과와 상황 snapshot을 저장한다.
- 성공 시 `201 Created`와 `RecommendationResponse`를 반환한다.
- 추천 business failure는 `422 Unprocessable Entity`를 사용한다.

### 추천 이력 조회

`GET /api/recommendations?limit={limit}`

- 기본 `limit=20`
- 최소 `1`, 최대 `50`
- 최신순
- 각 항목은 `situation`, `worn`, `wornAt`, `feedback`을 포함한다.

### 착용 완료

`PATCH /api/recommendations/{recommendationId}/worn`

- idempotent하다.
- 현재 사용자 소유 추천만 처리한다.
- 이미 착용 완료된 추천이면 기존 착용 시각을 반환한다.

### 추천 피드백 전체 교체

`PUT /api/recommendations/{recommendationId}/feedback`

Request:

```json
{
  "sentiment": "LIKED",
  "thermal": "TOO_COLD"
}
```

Field values:

| Field | Allowed values |
| --- | --- |
| `sentiment` | `LIKED`, `DISLIKED`, `null` |
| `thermal` | `TOO_COLD`, `TOO_HOT`, `null` |

처리 규칙:

- 현재 사용자 소유 추천만 수정 가능하다.
- PUT은 전체 교체다.
- 누락 필드는 `null`로 간주한다.
- 명시적 `null`도 해당 필드를 clear한다.
- `{}`는 `{ "sentiment": null, "thermal": null }`과 같다.
- 둘 다 `null`이면 피드백 전체 clear이며 응답 `feedback`은 `null`이다.
- 둘 중 하나라도 값이 있으면 `feedback.updatedAt`을 현재 시각으로 갱신한다.

성공 응답:

```json
{
  "data": {
    "recommendationId": 10,
    "feedback": {
      "sentiment": "LIKED",
      "thermal": "TOO_COLD",
      "updatedAt": "2026-05-26T10:05:00"
    }
  }
}
```

Clear 응답:

```json
{
  "data": {
    "recommendationId": 10,
    "feedback": null
  }
}
```

## 7. 에러 코드

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

## 8. 프론트 API 주의

- 로그인 전 보호 API를 호출하지 않는다.
- protected image URL은 Authorization header가 필요하므로 blob fetch로 조회한다.
- `POST /api/recommendations`는 body 없이 호출 가능해야 한다.
- feedback clear는 `{}` 또는 양쪽 `null` body로 처리한다.

# API: SmartCloset MVP5 Contract

이 문서는 SmartCloset MVP5 API 계약을 설명한다. MVP5는 기존 인증 사용자 API 위에 옷 이미지 업로드, 조회, 삭제 API를 제공한다.

## MVP5 API 결정

- 새 공개 API를 추가하지 않는다.
- 기존 옷 등록/수정 JSON API를 multipart로 바꾸지 않는다.
- 이미지 업로드는 별도 보호 API `PUT /api/clothes/{clothingId}/image`로 처리한다.
- 이미지 조회는 보호 API `GET /api/clothes/{clothingId}/image`로 처리한다.
- 이미지 삭제는 보호 API `DELETE /api/clothes/{clothingId}/image`로 처리한다.
- 이미지 API도 현재 인증 사용자 소유 옷만 접근할 수 있다.
- 회원가입 직후 기본 옷 프리셋 5개를 생성한다.
- 기존 계정은 로그인 시 옷이 0개이면 같은 프리셋을 한 번만 생성한다.
- 공개 `userId` query parameter를 추가하지 않는다.
- 현재 사용자 전용 response DTO에 `userId`를 노출하지 않는다.

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
      "field": "image",
      "message": "허용되지 않는 이미지 형식입니다."
    }
  ]
}
```

`details` 필드는 항상 배열이다. 상세 항목이 없으면 빈 배열 `[]`이다.

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
| `POST` | `/api/recommendations` | 추천 생성 및 저장 | `201 Created` |
| `GET` | `/api/recommendations?limit={limit}` | 추천 이력 조회 | `200 OK` |
| `PATCH` | `/api/recommendations/{recommendationId}/worn` | 추천 결과 착용 완료 처리 | `200 OK` |

## 3. 인증 API

### SignupRequest

```json
{
  "email": "demo@example.com",
  "password": "password123!",
  "name": "Demo User"
}
```

회원가입 시 서버는 기본 위치 `SEOUL`, 빈 선호도, 기본 옷 프리셋 5개를 함께 생성한다. 기본 옷 프리셋도 현재 사용자 소유 옷이므로 `GET /api/clothes`와 보호 이미지 API로만 조회한다.

### LoginRequest

```json
{
  "email": "demo@example.com",
  "password": "password123!"
}
```

로그인 시 현재 사용자 옷이 0개이면 서버는 기본 옷 프리셋 5개를 한 번만 생성한다. 옷 row가 하나라도 있으면 archived 상태와 관계없이 자동 프리셋을 추가하지 않는다.

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

옷 등록과 옷 전체 수정에서 같은 JSON 요청 필드를 사용한다. MVP5에서도 이 요청은 이미지 파일을 포함하지 않는다.

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

### ClothingImageResponse

`image`가 있는 경우:

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
- 정렬은 기존 구현 기준인 `id` 오름차순을 유지한다.
- 각 옷의 `image`는 nullable이다.
- 신규 가입 직후에는 기본 옷 프리셋 5개가 이미지 metadata와 함께 반환된다.

### 기본 옷 프리셋

서버는 신규 가입자와 옷이 0개인 기존 로그인 사용자에게 아래 프리셋을 현재 사용자 소유 옷으로 생성한다.

| Name | Category | Color | Material | Min | Max | Rain |
| --- | --- | --- | --- | ---: | ---: | --- |
| 화이트 반팔 티셔츠 | `TOP` | `WHITE` | `COTTON` | 8 | 30 | false |
| 블랙 반팔 티셔츠 | `TOP` | `BLACK` | `COTTON` | 8 | 30 | false |
| 흑청 데님 팬츠 | `BOTTOM` | `BLACK` | `DENIM` | 0 | 28 | false |
| 진청 데님 팬츠 | `BOTTOM` | `BLUE` | `DENIM` | 0 | 28 | false |
| 블랙 가디건 | `OUTER` | `BLACK` | `KNIT` | 8 | 20 | false |

프리셋 이미지는 번들 resource에서 사용자별 UUID 파일로 복사해 저장하며 content type은 `image/jpeg`이다. 삭제, 교체, 조회 규칙은 사용자가 직접 업로드한 이미지와 동일하다.

### 옷 등록

`POST /api/clothes`

- 요청은 `application/json`이다.
- 성공 시 `201 Created`와 `ClothingResponse`를 반환한다.
- 이미지는 이 API에서 받지 않는다.

### 옷 전체 수정

`PUT /api/clothes/{clothingId}`

- 요청은 `application/json`이다.
- 이름, 카테고리, 색상, 소재, 기온 범위, 비 적합 여부를 전체 수정한다.
- 이미지 메타데이터는 이 API에서 변경하지 않는다.

### 옷 보관 처리

`PATCH /api/clothes/{clothingId}/archive`

- idempotent해야 한다.
- 이미지 파일과 이미지 메타데이터는 보관 처리만으로 삭제하지 않는다.

## 5. 옷 이미지 API

### 업로드 또는 교체

`PUT /api/clothes/{clothingId}/image`

Request:

- `Content-Type: multipart/form-data`
- part name: `image`
- file: jpg/jpeg/png/webp, 최대 5MB

성공 응답:

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
    "image": {
      "url": "/api/clothes/1/image",
      "contentType": "image/jpeg",
      "sizeBytes": 123456,
      "uploadedAt": "2026-05-25T10:00:00"
    },
    "createdAt": "2026-05-22T10:00:00",
    "updatedAt": "2026-05-25T10:00:00"
  }
}
```

동작:

- 기존 이미지가 있으면 새 이미지 저장 성공 후 기존 파일을 삭제한다.
- DB 메타데이터와 파일 저장이 불일치하지 않도록 실패 시 기존 이미지 상태를 유지한다.
- 원본 파일명은 저장 경로에 사용하지 않는다.

### 이미지 조회

`GET /api/clothes/{clothingId}/image`

성공:

- `200 OK`
- `Content-Type: image/jpeg`, `image/png`, 또는 `image/webp`
- response body: image bytes

실패:

- 토큰 없음 또는 잘못된 토큰: `401`
- 다른 사용자 옷 또는 존재하지 않는 옷: `404 CLOTHING_NOT_FOUND`
- 내 옷이지만 이미지가 없음: `404 CLOTHING_IMAGE_NOT_FOUND`

### 이미지 삭제

`DELETE /api/clothes/{clothingId}/image`

- idempotent하다.
- 이미지가 이미 없어도 `200 OK`와 `ClothingResponse`를 반환한다.
- 삭제 후 `image`는 `null`이다.

## 6. 이미지 검증 정책

| 항목 | 기준 |
| --- | --- |
| 최대 크기 | 5MB |
| 허용 확장자 | `.jpg`, `.jpeg`, `.png`, `.webp` |
| 허용 MIME type | `image/jpeg`, `image/png`, `image/webp` |
| part name | `image` |

잘못된 파일은 `400 INVALID_REQUEST`로 실패한다.
Spring multipart limit은 앱 validator의 `CLOTHING_IMAGE_MAX_SIZE_BYTES`보다 작게 설정하지 않는다. 파일 크기 초과와 multipart size 초과는 모두 `400 INVALID_REQUEST`와 `details` 배열로 실패한다.

검증 대상:

- 파일 없음
- 빈 파일
- 최대 크기 초과
- 허용되지 않는 확장자
- 허용되지 않는 MIME type
- 확장자와 MIME type 불일치
- 이미지 signature 불일치

## 7. 추천 API

추천 생성은 기존처럼 `POST /api/recommendations`만 사용한다. today 추천 GET 경로는 사용하지 않는다.

### OutfitItemResponse

MVP5에서 추천 outfit item은 nullable image metadata를 포함한다.

```json
{
  "id": 1,
  "name": "Gray Knit",
  "category": "TOP",
  "color": "GRAY",
  "material": "KNIT",
  "image": {
    "url": "/api/clothes/1/image",
    "contentType": "image/jpeg",
    "sizeBytes": 123456,
    "uploadedAt": "2026-05-25T10:00:00"
  }
}
```

이미지가 없으면 `image`는 `null`이다.

### RecommendationResponse

추천 응답의 전체 구조는 유지한다.

```json
{
  "recommendationId": 10,
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
      "image": null
    },
    "bottom": {
      "id": 2,
      "name": "Black Denim",
      "category": "BOTTOM",
      "color": "BLACK",
      "material": "DENIM",
      "image": null
    },
    "outer": null
  },
  "score": {
    "totalScore": 80,
    "weatherScore": 30,
    "colorScore": 20,
    "wearHistoryScore": 20,
    "recommendationHistoryScore": 5,
    "preferenceScore": 5
  },
  "reasons": ["현재 기온에 맞는 조합이에요."],
  "worn": false,
  "createdAt": "2026-05-25T10:00:00"
}
```

이미지 존재 여부는 추천 점수, 후보 필터링, 추천 이유에 영향을 주지 않는다.

## 8. 에러 코드

기존 에러 코드에 아래 코드를 추가한다.

| Code | HTTP | Message |
| --- | --- | --- |
| `CLOTHING_IMAGE_NOT_FOUND` | `404` | 옷 이미지를 찾을 수 없습니다. |

이미지 검증 실패는 새 도메인 코드가 아니라 `INVALID_REQUEST`와 `details`로 표현한다.

기존 주요 코드:

| Code | HTTP |
| --- | --- |
| `INVALID_REQUEST` | `400` |
| `UNAUTHORIZED` | `401` |
| `INVALID_TOKEN` | `401` |
| `FORBIDDEN` | `403` |
| `USER_NOT_FOUND` | `404` |
| `CLOTHING_NOT_FOUND` | `404` |
| `RECOMMENDATION_NOT_FOUND` | `404` |
| `NO_TOP_AVAILABLE` | `422` |
| `NO_BOTTOM_AVAILABLE` | `422` |
| `NO_WEATHER_SUITABLE_ITEM` | `422` |
| `OUTER_REQUIRED_BUT_NOT_AVAILABLE` | `422` |
| `INSUFFICIENT_CLOSET_ITEMS` | `422` |
| `INTERNAL_SERVER_ERROR` | `500` |

## 9. 프론트 API 주의

- 이미지 업로드 함수는 `FormData`를 사용한다.
- 브라우저가 multipart boundary를 설정하도록 `Content-Type` header를 직접 지정하지 않는다.
- 이미지 조회 함수는 `Authorization` header를 붙여 `Blob`을 가져온다.
- blob object URL은 화면에서 더 이상 쓰지 않을 때 revoke한다.

# API: SmartCloset MVP8 Contract

이 문서는 SmartCloset MVP8 API 계약을 설명한다. MVP8은 기존 인증 사용자 API, MVP5 이미지 API, MVP6 피드백/개인화 API, MVP7 위치/날씨 신뢰도 API 위에 refresh token, 이메일 인증, 비밀번호 재설정, Google login, 계정 삭제 API를 추가한다.

## MVP8 API 결정

- 공개 `userId` query parameter를 추가하지 않는다.
- 현재 사용자 전용 response DTO에 `userId`를 노출하지 않는다.
- Access token은 JWT bearer token으로 유지한다.
- Refresh token은 HttpOnly cookie로만 전달하고 JSON 응답에 포함하지 않는다.
- Refresh token 원문, 이메일 인증 token 원문, 비밀번호 재설정 token 원문은 저장하지 않는다.
- Password signup 직후 access token을 발급하지 않는다.
- 미인증 password 계정 login은 실패한다.
- Google verified email은 이메일 인증 완료로 취급한다.
- Account deletion은 보호 API이며 현재 사용자 데이터만 삭제한다.
- 추천/날씨/위치/이미지 API 계약은 MVP7 기준을 유지한다.

## 1. 공통 규칙

- 공개 API는 access token 없이 호출 가능하다.
- 보호 API는 `Authorization: Bearer {accessToken}` header가 필요하다.
- Refresh API와 logout은 refresh cookie를 사용할 수 있다.
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
      "field": "email",
      "message": "올바른 이메일 형식이어야 합니다."
    }
  ]
}
```

## 2. API 목록

### 공개 API

| Method | Path | Description | Success |
| --- | --- | --- | --- |
| `POST` | `/api/auth/signup` | 회원가입과 이메일 인증 요청 생성 | `201 Created` |
| `POST` | `/api/auth/login` | 로그인, access token 발급, refresh cookie 설정 | `200 OK` |
| `POST` | `/api/auth/refresh` | refresh cookie 회전과 access token 재발급 | `200 OK` |
| `POST` | `/api/auth/logout` | refresh session revoke와 cookie 만료 | `200 OK` |
| `POST` | `/api/auth/email-verification/request` | 이메일 인증 재요청 | `200 OK` |
| `POST` | `/api/auth/email-verification/confirm` | 이메일 인증 확인 | `200 OK` |
| `POST` | `/api/auth/password-reset/request` | 비밀번호 재설정 요청 | `200 OK` |
| `POST` | `/api/auth/password-reset/confirm` | 비밀번호 재설정 확인 | `200 OK` |
| `GET` | `/api/auth/oauth2/providers` | OAuth provider 활성 상태 조회 | `200 OK` |
| `GET` | `/api/auth/oauth2/google` | Google OAuth login 시작 | `302 Found` |
| `GET` | `/api/auth/oauth2/callback/google` | Google OAuth callback | `302 Found` |

### 보호 API

| Method | Path | Description | Success |
| --- | --- | --- | --- |
| `GET` | `/api/users/me` | 현재 사용자 조회 | `200 OK` |
| `DELETE` | `/api/users/me` | 현재 사용자 계정과 데이터 삭제 | `200 OK` |
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

## 3. Auth API

### SignupRequest

```json
{
  "email": "demo@example.com",
  "password": "password123!",
  "name": "Demo User"
}
```

회원가입 시 서버는 기본 위치 `SEOUL`, 위치 source `MANUAL_SEARCH`, 빈 선호도, 기본 옷 프리셋 5개를 함께 생성한다. MVP8 password signup은 이메일 인증 전에는 access token을 발급하지 않는다.

### SignupResponse

```json
{
  "data": {
    "email": "demo@example.com",
    "emailVerificationRequired": true,
    "message": "이메일 인증 후 로그인할 수 있습니다."
  }
}
```

### LoginRequest

```json
{
  "email": "demo@example.com",
  "password": "password123!"
}
```

미인증 password 계정은 `EMAIL_VERIFICATION_REQUIRED`로 실패한다.

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
      "emailVerified": true,
      "passwordLoginEnabled": true,
      "authProviders": ["PASSWORD"],
      "createdAt": "2026-05-27T10:00:00",
      "updatedAt": "2026-05-27T10:00:00"
    }
  }
}
```

성공한 login, refresh, OAuth callback은 refresh cookie를 설정한다. Refresh token 원문은 JSON body에 넣지 않는다.

### Refresh

`POST /api/auth/refresh`

Request body는 없다. 서버는 refresh cookie를 읽어 session을 검증하고 token rotation을 수행한다.

성공 응답은 `AuthResponse`이며 새 refresh cookie를 함께 설정한다.

실패:

- refresh cookie 없음: `401 UNAUTHORIZED`
- 만료, revoke, hash 불일치, 재사용 의심: `401 INVALID_TOKEN`

### Logout

`POST /api/auth/logout`

Request body는 없다. 서버는 refresh cookie가 있으면 관련 session을 revoke하고 cookie를 만료한다. refresh cookie가 없거나 이미 revoke된 session이어도 성공한다.

성공 응답:

```json
{
  "data": {
    "loggedOut": true
  }
}
```

## 4. Email Verification API

### EmailVerificationRequest

```json
{
  "email": "demo@example.com"
}
```

응답은 계정 존재 여부를 과도하게 노출하지 않는다. 이미 인증된 계정도 성공 응답을 반환할 수 있다.

```json
{
  "data": {
    "requested": true
  }
}
```

### EmailVerificationConfirmRequest

```json
{
  "token": "verification-token-from-email"
}
```

성공 응답:

```json
{
  "data": {
    "emailVerified": true
  }
}
```

규칙:

- Token 원문은 DB에 저장하지 않는다.
- Token은 hash로 조회한다.
- Token은 만료 시간이 있고 single-use다.
- 만료, 사용 완료, hash 불일치 시 `ACCOUNT_TOKEN_INVALID`로 실패한다.

## 5. Password Reset API

### PasswordResetRequest

```json
{
  "email": "demo@example.com"
}
```

성공 응답은 계정 존재 여부와 무관하게 동일하다.

```json
{
  "data": {
    "requested": true
  }
}
```

### PasswordResetConfirmRequest

```json
{
  "token": "reset-token-from-email",
  "newPassword": "newPassword123!"
}
```

성공 응답:

```json
{
  "data": {
    "passwordReset": true
  }
}
```

규칙:

- 새 password는 기존 signup password validation을 따른다.
- 성공 시 password hash를 BCrypt로 갱신한다.
- 성공 시 해당 사용자 refresh sessions를 revoke한다.
- Password login disabled 계정은 `PASSWORD_LOGIN_DISABLED`로 실패할 수 있다.

## 6. OAuth API

### OAuthProvidersResponse

`GET /api/auth/oauth2/providers`

```json
{
  "data": {
    "google": {
      "enabled": true,
      "loginUrl": "/api/auth/oauth2/google"
    }
  }
}
```

Google client id/secret/redirect 설정이 없으면 `enabled=false`, `loginUrl=null`이다.

### Google OAuth rules

- OAuth login start는 `/api/auth/oauth2/google`이다.
- OAuth callback은 `/api/auth/oauth2/callback/google`이다.
- OAuth login start는 short-lived HttpOnly state cookie를 설정하고 Google authorization URL의 `state`와 매칭한다.
- OAuth callback의 `state`가 state cookie와 일치하지 않으면 refresh cookie를 발급하지 않고 `UNAUTHORIZED`로 실패한다.
- 성공 시 backend는 refresh cookie를 설정하고 frontend callback URL로 redirect한다.
- Access token은 frontend가 callback 후 `POST /api/auth/refresh`로 받아도 된다.
- Google profile의 email은 verified email이어야 한다.
- 기존 같은 email user가 있으면 social account를 link한다.
- 새 Google user는 password login disabled, email verified 상태로 생성한다.

## 7. Current User API

`GET /api/users/me`

```json
{
  "data": {
    "email": "demo@example.com",
    "name": "Demo User",
    "role": "USER",
    "emailVerified": true,
    "passwordLoginEnabled": true,
    "authProviders": ["PASSWORD", "GOOGLE"],
    "createdAt": "2026-05-27T10:00:00",
    "updatedAt": "2026-05-27T10:00:00"
  }
}
```

## 8. Account Deletion API

`DELETE /api/users/me`

### AccountDeletionRequest

Password login enabled 계정:

```json
{
  "confirmation": "DELETE",
  "password": "password123!"
}
```

Google-only 계정:

```json
{
  "confirmation": "DELETE"
}
```

성공 응답:

```json
{
  "data": {
    "deleted": true
  }
}
```

규칙:

- 보호 API다.
- `confirmation`은 정확히 `DELETE`여야 한다.
- Password login enabled 계정은 현재 비밀번호 검증이 필요하다.
- 현재 사용자 소유 데이터만 삭제한다.
- DB 삭제 대상은 user, clothing items, recommendation results/items, wear histories, refresh sessions, account action tokens, social accounts다.
- 이미지 파일은 `ClothingImageStorage`를 통해 삭제한다.
- 삭제 후 기존 access token은 사용자 조회에서 `USER_NOT_FOUND` 또는 인증 실패 성격의 응답으로 더 이상 보호 resource를 읽을 수 없어야 한다.

## 9. 유지 API

위치, 날씨, 옷, 추천 API는 MVP7 계약을 유지한다.

- `GET /api/locations?keyword={keyword}`는 내부 KMA catalog 검색이다.
- `POST /api/locations/resolve`는 브라우저 좌표 원문을 저장하지 않는다.
- `POST /api/recommendations`는 optional `situation`, `forecastPeriod`를 받을 수 있다.
- `WeatherResponse`와 `RecommendationResponse.weather`는 location/source metadata를 포함한다.
- 추천 피드백 PUT은 전체 교체이며 누락 필드는 `null`로 간주한다.

## 10. Error Codes

MVP8에서 추가 또는 명시하는 error code:

| Code | Status | Meaning |
| --- | --- | --- |
| `EMAIL_VERIFICATION_REQUIRED` | `403 Forbidden` | 이메일 인증 전 password login 차단 |
| `ACCOUNT_TOKEN_INVALID` | `400 Bad Request` | 인증/재설정 token 없음, 만료, 사용 완료, 불일치 |
| `PASSWORD_LOGIN_DISABLED` | `400 Bad Request` | password login이 없는 계정에서 password flow 사용 |
| `OAUTH2_PROVIDER_UNAVAILABLE` | `503 Service Unavailable` | Google OAuth 설정 없음 또는 비활성 |

기존 공통 code와 MVP5/MVP6/MVP7 domain failure code는 유지한다.

# 아키텍처: SmartCloset MVP8

## 전체 아키텍처 개요

SmartCloset MVP8은 Spring Boot 4.0.6 백엔드와 React+Vite+TypeScript 프론트엔드 앱으로 구성한다. MVP8의 변경 지점은 account/auth 영역이다.

기존 위치/날씨, 옷 이미지, 추천 피드백/개인화, 추천 이력 구조는 유지한다.

```text
Controller -> Application Service -> Domain Service -> Repository / Provider
```

추천 점수 계산은 recommendation domain service에 둔다. Controller와 Repository에는 점수 계산 로직을 두지 않는다.

## 권장 패키지 구조

```text
com.smartcloset
├── auth
│   ├── application
│   ├── domain
│   ├── dto
│   ├── infrastructure
│   └── presentation
├── account
│   ├── application
│   ├── domain
│   ├── dto
│   └── presentation
├── common
├── security
├── user
├── location
├── weather
├── clothing
└── recommendation
```

MVP8 auth/account에는 아래 개념을 둔다.

- `RefreshSession`
- `RefreshTokenService`
- `RefreshTokenCookieProperties`
- `AccountActionToken`
- `AccountActionTokenPurpose`: `EMAIL_VERIFICATION`, `PASSWORD_RESET`
- `EmailSender`
- `ConsoleEmailSender`
- `SocialAccount`
- `OAuthProvider`: `GOOGLE`
- `AccountDeletionService`

프론트엔드:

```text
frontend/src
├── api
├── components
├── features
│   ├── auth
│   ├── account
│   ├── clothes
│   ├── location
│   ├── preferences
│   ├── recommendation
│   └── history
├── types
└── main.tsx
```

## 인증 경계

공개 API:

- `POST /api/auth/signup`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `POST /api/auth/email-verification/request`
- `POST /api/auth/email-verification/confirm`
- `POST /api/auth/password-reset/request`
- `POST /api/auth/password-reset/confirm`
- `GET /api/auth/oauth2/providers`
- `GET /api/auth/oauth2/google`
- `GET /api/auth/oauth2/callback/google`

그 외 `/api/**` endpoint는 보호 API다.

모든 사용자 소유 데이터는 인증 principal의 현재 사용자 id로 제한한다.

## Refresh session 흐름

```text
POST /api/auth/login
  -> AuthController
  -> AuthService.login
      -> password 검증
      -> emailVerified 확인
      -> JwtTokenProvider.createAccessToken
      -> RefreshTokenService.issue
      -> RefreshTokenCookieWriter.write
```

```text
POST /api/auth/refresh
  -> AuthController
  -> RefreshTokenCookieReader.read
  -> RefreshTokenService.rotate
      -> hash 조회
      -> 만료/revoked 검증
      -> 기존 session revokedAt 기록
      -> 새 refresh session 생성
      -> JwtTokenProvider.createAccessToken
      -> RefreshTokenCookieWriter.write
```

정책:

- Raw refresh token은 DB에 저장하지 않는다.
- Refresh token hash는 고정된 서버 secret 또는 secure digest 정책으로 생성한다.
- Refresh token rotation은 매 refresh 요청마다 수행한다.
- Reused/revoked token은 `INVALID_TOKEN`으로 실패한다.
- Logout은 멱등이며 cookie를 만료한다.
- Cookie 설정은 properties/env에서 읽는다.

## Email verification 흐름

```text
POST /api/auth/signup
  -> AuthService.signup
      -> User.createPasswordUser(emailVerified=false)
      -> default presets seed
      -> AccountActionTokenService.issue(EMAIL_VERIFICATION)
      -> EmailSender.sendEmailVerification
```

```text
POST /api/auth/email-verification/confirm
  -> AccountActionTokenService.consume
  -> User.markEmailVerified
```

정책:

- Password signup은 access token을 발급하지 않는다.
- 미인증 password 계정은 login할 수 없다.
- Token 원문은 저장하지 않고 hash만 저장한다.
- Token은 만료와 single-use를 적용한다.
- MVP8 구현체는 `ConsoleEmailSender`다.

## Password reset 흐름

```text
POST /api/auth/password-reset/request
  -> AccountActionTokenService.issue(PASSWORD_RESET)
  -> EmailSender.sendPasswordReset
```

```text
POST /api/auth/password-reset/confirm
  -> AccountActionTokenService.consume
  -> User.changePasswordHash
  -> RefreshSessionService.revokeAll(userId)
```

정책:

- Reset request 응답은 계정 존재 여부를 노출하지 않는다.
- Password login disabled 계정은 password reset confirm 대상이 아니다.
- Reset 성공 시 기존 refresh sessions를 revoke한다.

## Google OAuth 흐름

```text
GET /api/auth/oauth2/providers
  -> Google client 설정 존재 여부 확인
```

```text
GET /api/auth/oauth2/google
  -> OAuth state 생성
  -> HttpOnly state cookie 설정
  -> Google authorization redirect

GET /api/auth/oauth2/callback/google
  -> callback state와 state cookie 매칭
  -> Google profile 검증
  -> SocialAccount find/link/create
  -> User create or load
  -> refresh cookie 발급
  -> frontend callback URL redirect
```

정책:

- Google provider 설정이 없으면 provider status는 disabled다.
- Google email은 verified email이어야 한다.
- OAuth callback state가 state cookie와 일치해야 한다.
- 기존 같은 email user가 있으면 social account를 link한다.
- 새 Google user는 password login disabled, email verified 상태로 생성한다.
- OAuth redirect/base URL은 properties/env로 설정한다.

## Account deletion 흐름

```text
DELETE /api/users/me
  -> AccountController
  -> AccountDeletionService.deleteAccount(userId, request)
      -> password 확인 또는 confirmation 확인
      -> 삭제할 image stored filename 수집
      -> recommendation result items / wear histories / recommendations 삭제
      -> clothing items 삭제
      -> refresh sessions / action tokens / social accounts 삭제
      -> user 삭제
      -> ClothingImageStorage.delete(filename)
```

정책:

- 삭제는 현재 사용자 소유 데이터만 대상으로 한다.
- Password login enabled 계정은 현재 password 검증이 필요하다.
- Google-only 계정은 `confirmation=DELETE`를 요구한다.
- DB 삭제와 파일 삭제 보상 정책은 구현 단계에서 테스트 가능하게 정한다.
- 이미지 삭제는 `ClothingImageStorage` 인터페이스만 호출한다.

## AWS-ready adapter boundary

MVP8은 AWS 배포를 구현하지 않는다.

- `EmailSender`는 interface로 두고 MVP8은 `ConsoleEmailSender`만 구현한다.
- MVP9에서 SES/SMTP sender를 추가해도 auth application service는 바꾸지 않는다.
- `ClothingImageStorage`는 기존 local file 구현을 유지한다.
- MVP9에서 S3 구현체를 추가해도 account deletion service는 storage interface만 사용한다.
- Cookie, CORS, OAuth URL은 properties/env로 분리한다.
- `local` profile은 Docker Compose 기본 실행 경로로 유지하고, future `prod` profile은 별도 properties/env와 adapter bean으로 추가한다.
- local profile과 Docker Compose 경로는 계속 동작해야 한다.

## 기존 domain 흐름 유지

- 위치 검색과 좌표 resolve는 MVP7 계약을 유지한다.
- Weather provider는 KMA `getVilageFcst`와 fallback만 사용한다.
- 추천 생성은 `POST /api/recommendations`이며 optional `situation`, `forecastPeriod`를 받는다.
- 추천 결과와 이력의 위치/날씨 source snapshot은 유지한다.
- 옷 이미지 API는 보호 API이며 blob fetch에 Authorization header가 필요하다.
- 추천 피드백 PUT은 전체 교체이고 누락 필드는 `null`이다.

## 트랜잭션 경계

- Signup: user/default presets/action token 생성 write transaction, email sending은 transaction 이후 또는 실패 보상 가능 구조
- Login: user read, refresh session issue write transaction
- Refresh: refresh session rotation write transaction
- Logout: refresh session revoke write transaction 또는 멱등 no-op
- Email verification confirm: action token consume + user update write transaction
- Password reset confirm: action token consume + password update + refresh revoke write transaction
- OAuth callback: user/social account upsert + refresh issue write transaction
- Account deletion: current user owned data delete write transaction, image file cleanup은 명시적 보상 정책 필요
- 기존 위치/날씨/추천/이미지 transaction 정책은 MVP7 기준 유지

## 금지사항

- AWS 배포 구현을 추가하지 않는다. 이유: MVP8은 account stability이며 AWS는 MVP9 범위다.
- S3 구현체를 추가하지 않는다. 이유: MVP8은 `ClothingImageStorage` 경계만 보존한다.
- SES/SMTP 실제 발송 구현체를 추가하지 않는다. 이유: MVP8 이메일은 `ConsoleEmailSender` 기준이다.
- Redis를 추가하지 않는다. 이유: refresh session은 DB-backed로 검증한다.
- 추천 점수 계산을 변경하지 않는다. 이유: MVP8은 계정 안정성 범위다.
- 공개 `userId` query parameter를 추가하지 않는다. 이유: 인증 사용자 API 계약과 충돌한다.
- Refresh token 원문을 DB 또는 JSON 응답에 저장/노출하지 않는다. 이유: 계정 안정성 핵심 보안 계약이다.

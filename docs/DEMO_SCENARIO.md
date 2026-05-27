# Demo Scenario: SmartCloset MVP8

## 데모 목표

Docker Compose로 SmartCloset 백엔드, MySQL, React 프론트엔드를 실행한 뒤 브라우저에서 refresh session, 이메일 인증, 비밀번호 재설정, Google provider 상태, 세션 만료 UX, 계정 삭제 흐름을 확인한다.

MVP8 데모의 핵심은 사용자가 계정을 안정적으로 유지하고 복구하며 삭제할 수 있는지 확인하는 것이다.

## MVP8 데모 범위

포함:

- 회원가입 후 이메일 인증 필요 상태 확인
- 개발용 console/log email sender로 인증 token 확인
- 이메일 인증 완료 후 로그인
- 로그인 화면 이메일 저장 체크박스
- refresh cookie 기반 새로고침 세션 복구
- 보호 API 401 시 refresh retry-once 흐름
- 비밀번호 재설정 요청/확인
- Google provider enabled/disabled 상태 확인
- Google login flow 또는 설정 미비 시 disabled UX 확인
- 계정 삭제와 데이터 삭제
- MVP5/MVP6/MVP7 핵심 흐름 유지 확인

제외:

- AWS 배포
- S3 storage
- SES/SMTP 실제 발송
- Secrets Manager
- CD 자동화
- Redis
- admin 계정 관리
- 추천 규칙 변경

## 데모 전제

- `.env`는 `.env.example`을 복사해 만든다.
- MVP8 이메일은 `ConsoleEmailSender` 기준이며 실제 메일은 발송하지 않는다.
- Google OAuth 설정이 없으면 provider disabled 상태로 데모한다.
- KMA key가 없어도 `WEATHER_FALLBACK_ENABLED=true`이면 fallback weather로 기존 추천 데모 가능하다.
- Docker Compose reset 시 DB와 이미지 volume이 초기화된다.

## 실행

```bash
docker compose down -v
test -f .env || cp .env.example .env
docker compose up --build
```

Frontend:

```text
http://localhost:5173
```

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

## React 앱 MVP8 데모 시나리오

### 1. 회원가입과 이메일 인증 안내

1. Frontend에 접속한다.
2. 새 이메일/password/name으로 회원가입한다.
3. 회원가입 성공 후 이메일 인증 필요 안내를 확인한다.

기대 결과:

- 가입 직후 자동 로그인되지 않는다.
- access token이 JSON 응답에 포함되지 않는다.
- 인증 안내와 재요청 진입점이 표시된다.

### 2. 이메일 인증 확인

1. backend console/log에서 인증 token 또는 인증 링크를 확인한다.
2. 앱의 이메일 인증 확인 화면에 token을 입력하거나 링크를 연다.

기대 결과:

- 이메일 인증이 성공한다.
- 같은 token 재사용은 실패한다.
- 인증 완료 후 login 가능 상태가 된다.

### 3. 로그인과 refresh cookie

1. 인증된 이메일/password로 로그인한다.
2. 이메일 저장 체크박스를 선택한 뒤 로그인한다.
3. 앱을 새로고침한다.
4. 로그아웃 후 로그인 화면에 다시 진입한다.

기대 결과:

- login 응답은 access token을 반환한다.
- refresh token 값은 JSON body에 없다.
- 새로고침 후 `POST /api/auth/refresh`로 세션이 복구된다.
- 로그인 화면에는 저장된 이메일 주소만 복원된다.
- 비밀번호, access token, refresh token은 브라우저 저장소에 저장되지 않는다.
- Today, Closet, Preferences, Location, History view를 이동할 수 있다.

### 4. 세션 만료 UX

1. access token 만료 또는 invalid token 상황을 만든다.
2. 보호 API 호출을 수행한다.

기대 결과:

- 앱은 refresh를 한 번 시도한다.
- refresh 성공 시 원 요청을 한 번 재시도한다.
- refresh 실패 시 세션 만료 안내와 로그인 화면을 보여준다.

### 5. 비밀번호 재설정

1. 비밀번호 재설정 요청 화면에서 이메일을 입력한다.
2. backend console/log에서 reset token을 확인한다.
3. 새 비밀번호를 입력해 reset confirm을 수행한다.
4. 새 비밀번호로 로그인한다.

기대 결과:

- reset request 응답은 계정 존재 여부를 노출하지 않는다.
- reset token은 single-use다.
- reset 성공 후 기존 refresh session은 사용할 수 없다.

### 6. Google provider 상태

1. Auth 화면에서 Google login button 상태를 확인한다.
2. Google OAuth 설정이 없으면 disabled 안내를 확인한다.
3. 설정이 있으면 Google login flow를 수행한다.

기대 결과:

- `/api/auth/oauth2/providers`가 Google enabled/disabled 상태를 반환한다.
- disabled 상태에서는 login 시도를 유도하지 않는다.
- enabled 상태에서는 Google verified email 계정이 인증 완료 상태로 로그인된다.

### 7. 기존 MVP 기능 유지

1. Location에서 `일산동` 검색과 현재 위치 후보를 확인한다.
2. Today에서 상황과 예보 시간대를 선택해 추천을 생성한다.
3. 추천 결과와 History에서 위치/날씨 source snapshot을 확인한다.
4. Closet 이미지 업로드/삭제와 추천 피드백 저장/clear를 확인한다.

기대 결과:

- MVP5 이미지, MVP6 피드백/개인화, MVP7 위치/날씨 신뢰도 흐름이 유지된다.

### 8. 계정 삭제

1. Account settings로 이동한다.
2. confirmation `DELETE`를 입력한다.
3. Password login enabled 계정이면 현재 비밀번호를 입력한다.
4. 삭제를 실행한다.

기대 결과:

- 계정 삭제가 성공한다.
- 로그인 상태가 초기화된다.
- 기존 refresh cookie는 더 이상 사용할 수 없다.
- 삭제된 계정의 옷장, 추천 이력, 이미지 파일이 남아 보호 API에서 조회되지 않는다.

## API 실패 케이스 확인

- 미인증 password 계정 login: `403 EMAIL_VERIFICATION_REQUIRED`
- 만료/사용 완료 인증 token confirm: `400 ACCOUNT_TOKEN_INVALID`
- reset token 재사용: `400 ACCOUNT_TOKEN_INVALID`
- refresh cookie 없음: `401 UNAUTHORIZED`
- revoked refresh token 사용: `401 INVALID_TOKEN`
- Google provider 설정 없음: provider status `enabled=false`
- 계정 삭제 confirmation 누락: `400 INVALID_REQUEST`
- 계정 삭제 password 불일치: `401 UNAUTHORIZED`

## 완료 기준

- 이메일 인증 전 password login이 차단된다.
- 이메일 인증 후 login과 refresh session이 동작한다.
- 로그인 이메일 저장 체크박스가 이메일 주소만 저장하고 비밀번호와 token은 저장하지 않는다.
- 새로고침 세션 복구가 동작한다.
- 비밀번호 재설정이 동작하고 기존 refresh session이 revoke된다.
- Google provider 상태가 표시된다.
- 계정 삭제가 현재 사용자 데이터와 이미지 파일을 삭제한다.
- MVP5/MVP6/MVP7 핵심 기능이 유지된다.
- Docker Compose local 환경에서 앱이 정상 실행된다.

# MVP8을 계정 안정성 MVP로 정의

## 상태

승인됨

## 맥락

MVP7에서는 위치/날씨 source snapshot을 통해 추천 신뢰도를 높였다. 다음 단계에서는 사용자가 계정을 안정적으로 유지하고 복구하며, 본인 데이터를 삭제할 수 있어야 한다.

현재 인증은 JWT bearer access token만 사용하고 프론트는 access token을 `sessionStorage`에 저장한다. 이 방식은 단순하지만 새로고침 복구와 만료 UX가 제한적이고, 비밀번호 분실이나 이메일 소유 확인, 소셜 로그인, 계정 삭제 요구를 다루지 못한다.

MVP9는 AWS 배포가 예정되어 있다. MVP8에서 AWS를 직접 구현하면 범위가 커지므로, MVP8은 local Docker Compose 흐름을 유지하면서 MVP9에서 S3/SES/RDS/도메인 설정으로 교체할 수 있는 adapter 경계만 준비한다.

## 결정

MVP8은 계정 안정성 MVP다.

- DB-backed refresh session을 추가한다.
- Refresh token 원문은 DB에 저장하지 않고 hash만 저장한다.
- Refresh token은 HttpOnly cookie로만 전달하고 `AuthResponse` JSON에는 포함하지 않는다.
- Refresh API는 token rotation을 수행한다.
- Logout은 refresh session revoke와 cookie 만료를 수행하며 멱등이어야 한다.
- Access token은 JWT bearer token으로 유지한다.
- 프론트는 access token을 memory state에 저장하고, 앱 시작과 새로고침 시 refresh cookie로 세션을 복구한다.
- 보호 API 401 응답 시 프론트는 refresh를 한 번 시도하고 원 요청을 한 번만 재시도한다.
- Password 계정은 이메일 인증 전 로그인할 수 없다.
- 이메일 인증과 비밀번호 재설정 token은 hash만 저장하고 single-use로 처리한다.
- MVP8 이메일 발송 구현은 `EmailSender` 인터페이스와 개발용 `ConsoleEmailSender`만 제공한다.
- Google social login을 추가한다.
- Google이 verified email을 반환한 계정은 이메일 인증 완료로 취급한다.
- 계정 삭제는 soft delete가 아니라 즉시 하드 삭제다.
- 계정 삭제는 사용자 row, 옷장, 추천/착용/피드백, refresh session, account token, social account, 이미지 파일을 삭제한다.

## AWS-ready adapter 결정

MVP8은 AWS 배포를 구현하지 않는다. 대신 MVP9에서 운영 어댑터를 추가할 수 있도록 다음 경계를 둔다.

- Email 발송은 `EmailSender` 인터페이스 뒤에 둔다. MVP9에서 `SesEmailSender` 또는 SMTP sender를 추가할 수 있어야 한다.
- Clothing image 파일 삭제는 `ClothingImageStorage` 인터페이스만 통해 수행한다. MVP9에서 S3 구현체를 추가해도 계정 삭제 application service는 바꾸지 않는다.
- Refresh cookie name, max age, Secure, SameSite, domain, path는 properties/env로 분리한다.
- CORS allowed origins와 credentials 설정은 properties/env로 분리한다.
- OAuth redirect/base URL은 properties/env로 분리한다.
- local profile과 Docker Compose 실행 흐름은 계속 유지한다.

## 결과

- 사용자는 access token 만료 후에도 refresh cookie가 유효하면 세션을 복구할 수 있다.
- 이메일 소유 확인 전 password login을 차단할 수 있다.
- 사용자는 비밀번호를 잊어도 reset flow로 복구할 수 있다.
- 사용자는 Google 계정으로 로그인할 수 있다.
- 사용자는 본인 계정과 데이터를 삭제할 수 있다.
- MVP9 AWS 배포에서는 S3/SES/RDS/도메인 설정을 adapter와 profile 중심으로 추가할 수 있다.

## 범위 제외

- AWS 배포 구현
- S3 storage 구현체
- SES/SMTP 실제 발송 구현체
- Secrets Manager
- CD 자동화
- Redis
- admin 계정 관리
- soft delete/복구 정책
- production DB migration 도구 전환
- native mobile app 또는 PWA 배포
- 추천 점수/규칙 변경
- AI/GPT 추천
- AI 자동 태깅

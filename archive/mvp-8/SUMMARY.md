# MVP 8 Summary

## 구현된 기능

- DB-backed refresh session과 refresh token rotation/revoke
- HttpOnly refresh cookie 기반 세션 복구
- Access token memory state 저장과 보호 API 401 retry-once
- Password signup 이메일 인증 gate
- 이메일 인증 token과 비밀번호 재설정 token hash 저장, 만료, single-use 처리
- 개발용 `EmailSender` 인터페이스와 `ConsoleEmailSender`
- Google OAuth provider status, login/callback, social account link
- Google verified email의 이메일 인증 완료 처리
- 로그인 이메일 저장 체크박스
- Account settings와 계정 hard delete
- 계정 삭제 시 사용자 소유 데이터와 이미지 파일 cleanup
- Cookie/CORS/OAuth URL, Email, Image storage의 AWS-ready adapter boundary
- Docker Compose smoke, API smoke, QA 기록

## 유지된 기능

- Spring Boot 4.0.6, Java 21, MySQL
- Spring Security + JWT Bearer access token
- 인증 사용자 기준 옷장/위치/선호도/추천 이력/착용 이력 격리
- KMA `getVilageFcst` JSON weather provider와 fallback weather
- React+Vite+TypeScript 프론트엔드
- 규칙 기반 추천 점수 100점 체계
- MVP5 이미지 업로드/교체/조회/삭제와 썸네일 표시
- MVP6 추천 상황, 옷별 `styleTags`, 추천 피드백, 최근 피드백 기반 `preferenceScore`
- MVP7 위치/날씨 source snapshot과 `forecastPeriod`
- Docker Compose 공유 흐름

## 제외된 기능

- AWS 배포 구현
- S3 storage 구현체
- SES/SMTP 실제 발송 구현체
- Secrets Manager
- CD 자동화
- Redis
- admin 계정 관리
- soft delete/복구 정책
- production DB migration 도구 전환
- 추천 규칙 변경
- AI/GPT 추천
- AI 자동 태깅

## 데모 시나리오 요약

- Docker Compose로 MySQL, 백엔드, React 프론트엔드를 함께 실행한다.
- 회원가입 후 console/log 인증 token으로 이메일 인증을 완료한다.
- 인증된 password 계정 또는 Google provider로 로그인한다.
- 새로고침 후 refresh cookie로 세션이 복구되는지 확인한다.
- 비밀번호 재설정과 세션 만료 UX를 확인한다.
- 기존 위치/날씨 추천, 옷 이미지, 피드백 흐름이 유지되는지 확인한다.
- 계정 삭제 후 사용자 데이터와 이미지 파일이 삭제되는지 확인한다.

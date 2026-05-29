# MVP 8 Decisions

MVP 8의 상세 결정 기록은 현재 `docs/adr/`에 유지한다. 이 문서는 주요 결정만 요약한다.

## 주요 결정

- MVP8은 계정 안정성 MVP로 정의했다.
- 자세한 내용은 ../../docs/adr/013-mvp8-account-stability.md 를 따른다.
- Refresh token은 DB-backed session으로 관리하고 원문은 저장하지 않았다.
- Refresh token은 HttpOnly cookie로만 전달하고 JSON 응답에는 포함하지 않았다.
- Access token은 JWT bearer token으로 유지하되 frontend memory state에 저장했다.
- Password signup은 이메일 인증 필요 상태를 반환하고 가입 직후 access token을 발급하지 않았다.
- 미인증 password 계정 login은 `EMAIL_VERIFICATION_REQUIRED`로 차단했다.
- 이메일 인증과 비밀번호 재설정 token은 hash만 저장하고 single-use로 처리했다.
- MVP8 이메일 발송은 `EmailSender` 인터페이스와 개발용 `ConsoleEmailSender` 기준으로 확정했다.
- Google verified email은 이메일 인증 완료로 취급했다.
- 계정 삭제는 soft delete가 아니라 즉시 hard delete로 확정했다.
- AWS 배포는 구현하지 않고 Email/Image/Cookie/CORS/OAuth URL adapter 경계만 준비했다.

## MVP9로 넘긴 문제

- 서비스가 완성된 느낌을 줄 수 있는 프론트 UI/UX 리디자인
- `tmp/design-preview` 기반 Auth, 추천, 옷장, 취향, 위치, 기록, 계정 설정 화면 개선
- 데스크톱과 모바일 반응형 레이아웃 완성도 점검
- AWS 배포는 MVP9에서 다루지 않고 후속 MVP로 연기한다.

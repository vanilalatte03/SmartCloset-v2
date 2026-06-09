# 계정 삭제 refresh cookie 만료

## 배경

MVP8 계정 안정성 계약은 계정 삭제 후 현재 사용자 데이터, refresh session, 계정 토큰, social account를 제거하고 기존 세션을 복구할 수 없게 하는 것이다. `POST /api/auth/logout`은 서버 refresh session revoke와 브라우저 refresh cookie 만료를 함께 수행한다.

## 문제

`DELETE /api/users/me` 성공 경로는 DB refresh session을 삭제했지만, 브라우저의 HttpOnly refresh cookie를 만료하지 않았다. 프론트는 HttpOnly cookie를 직접 삭제할 수 없으므로 계정 삭제 직후에도 stale cookie가 남아 다음 refresh 시도에서 불필요한 `401` 흐름이 발생할 수 있었다.

## 변경

- `CurrentUserController`가 `RefreshTokenCookieWriter`를 주입받는다.
- 계정 삭제 service가 성공 응답을 반환한 뒤에만 `RefreshTokenCookieWriter.expire(response)`를 호출한다.
- cookie 만료는 logout과 동일한 writer를 사용하므로 name, path, domain, SameSite, Secure 설정을 공유한다.
- confirmation 오류, 비밀번호 불일치, 사용자 없음 등 삭제 실패 경로에서는 controller가 expire 호출까지 도달하지 않아 refresh cookie 만료 header를 내려주지 않는다.

## 계약 유지

- 계정 삭제 DB hard delete 책임은 `AccountDeletionService`에 유지한다.
- refresh cookie HTTP header 정리는 controller/infrastructure 경계에서 처리한다.
- refresh token 원문은 JSON response, DB, 로그에 노출하지 않는다.
- 삭제 후 기존 access token은 `USER_NOT_FOUND`로 실패한다.
- 삭제 후 기존 refresh cookie로 refresh를 시도해도 session이 복구되지 않는다.

## 검증

- `CurrentUserControllerTest`
  - password 계정 삭제 성공 응답에 refresh cookie `Max-Age=0` header가 포함된다.
  - Google-only 계정 삭제 성공 응답에도 refresh cookie 만료 header가 포함된다.
  - password 불일치 실패 응답에는 refresh cookie 만료 header가 없다.
  - confirmation 오류 실패 응답에는 refresh cookie 만료 header가 없다.
  - 삭제 후 기존 access token은 `USER_NOT_FOUND`로 실패한다.
  - 삭제 후 기존 refresh cookie로 `POST /api/auth/refresh`를 호출해도 `UNAUTHORIZED`로 실패한다.

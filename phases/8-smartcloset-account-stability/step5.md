# 단계 5: frontend-account-stability-ux

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/API.md`
- `docs/FRONTEND.md`
- `docs/DEMO_SCENARIO.md`
- `frontend/src/App.tsx`
- `frontend/src/api/**`
- `frontend/src/features/auth/**`
- `frontend/src/types/api.ts`

## 작업

- MVP8 auth/account DTO를 `frontend/src/types/api.ts`에 추가한다.
- `frontend/src/api/smartClosetApi.ts`에 signup/login/refresh/logout/email verification/password reset/OAuth providers/account deletion 함수를 추가한다.
- refresh cookie 요청은 `credentials: 'include'`를 사용한다.
- access token 저장 위치를 memory state로 변경한다.
- 앱 시작 시 refresh session 복구를 시도한다.
- 보호 API 401 발생 시 refresh 후 원 요청을 한 번만 재시도한다.
- refresh 실패 시 세션 만료 안내를 보여준다.
- AuthPanel 로그인 form에 이메일 저장 체크박스를 추가하고 저장된 이메일 주소를 초기 입력값으로 복원한다.
- 이메일 저장 체크박스를 선택한 로그인은 이메일 주소만 저장하고, 해제한 로그인은 저장된 이메일 주소를 제거한다.
- AuthPanel에 이메일 인증 안내/재요청/확인, 비밀번호 재설정, Google provider button 상태를 추가한다.
- Authenticated shell에 account settings 진입점과 계정 삭제 UI를 추가한다.
- 기존 Location/Today/History/Closet/Preferences 흐름을 유지한다.

## 인수 기준

```bash
(cd frontend && npm run build)
git diff --check
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. 아키텍처 체크리스트를 확인한다:
   - access token이 sessionStorage/localStorage에 저장되지 않는가?
   - 이메일 저장 기능이 이메일 주소 문자열만 저장하고 비밀번호/access token/refresh token/current user를 저장하지 않는가?
   - refresh cookie 요청에 credentials가 포함되는가?
   - retry-once가 무한 반복되지 않는가?
   - Google disabled 상태가 사용자에게 자연스럽게 표시되는가?
   - 계정 삭제 성공 후 local auth state가 초기화되는가?
3. 결과에 따라 `phases/8-smartcloset-account-stability/index.json`의 해당 단계를 업데이트한다:
   - 성공 -> `"status": "completed"`, `"summary": "MVP8 auth/session 복구, 이메일 저장, 인증/재설정, Google 상태, 계정 삭제 frontend UX를 추가했다."`
   - 수정 3회 시도 후에도 실패 -> `"status": "error"`, `"error_message": "구체적 에러 내용"`
   - 사용자 개입 필요 -> `"status": "blocked"`, `"blocked_reason": "구체적 사유"` 후 즉시 중단

검증 또는 리뷰가 통과하지 못하면 `issues/8-smartcloset-account-stability/issue-N.md`에 재현 명령, 핵심 에러, 수정 방향을 기록하고 fix step을 추가한다.

## 금지사항

- access token을 `sessionStorage`나 `localStorage`에 저장하지 마라. 이유: MVP8 프론트 기준은 memory state다.
- refresh token 값을 JavaScript state에 저장하지 마라. 이유: refresh token은 HttpOnly cookie 전용이다.
- 이메일 저장 기능으로 비밀번호, token, current user object를 저장하지 마라. 이유: 이메일 주소 문자열만 저장하는 편의 기능이다.
- 큰 state-management library를 추가하지 마라. 이유: 기존 React state 구조를 유지한다.
- 기존 MVP7 위치/날씨 UX를 제거하지 마라. 이유: 계정 안정성 외 기존 기능은 유지되어야 한다.

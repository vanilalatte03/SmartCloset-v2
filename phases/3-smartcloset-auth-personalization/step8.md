# 단계 8: frontend-auth-session

범위: Must-have / 3차 P0

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/FRONTEND.md`
- `docs/API.md`
- `docs/PRD.md`
- `docs/COMMANDS.md`
- `frontend/package.json`
- `frontend/src/api/**`
- `frontend/src/types/**`
- `frontend/src/App.tsx`

이전 단계에서 만들어진 코드를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업
React 프론트엔드의 인증 세션, API client, 타입 계약을 3차 API에 맞춘다. 이 단계는 로그인 전/후 shell과 auth/session 복구를 우선하고, 세부 위치/선호도/추천 화면 연결은 다음 step에서 마무리한다.

## 변경 예상 파일
- `frontend/src/types/api.ts`
- `frontend/src/api/client.ts`
- `frontend/src/api/smartClosetApi.ts`
- `frontend/src/features/auth/**`
- `frontend/src/App.tsx`
- `frontend/src/main.tsx`
- `frontend/src/App.css` 또는 현재 스타일 파일

## 구현 메모
- access token 저장 위치는 `sessionStorage`로 고정한다.
- 권장 key는 `smartcloset.accessToken`이다.
- 앱 시작 시 token이 있으면 `GET /api/users/me`로 현재 사용자 정보를 복구한다.
- token이 없거나 `GET /api/users/me`가 401이면 로그인/회원가입 화면을 보여준다.
- 로그인 성공 시 `accessToken`, `tokenType=Bearer`, `user`를 상태에 저장한다.
- 로그아웃 시 sessionStorage token과 user state를 제거한다.
- 모든 보호 API client 함수는 access token을 받아 `Authorization: Bearer ...` header를 붙인다.
- 프론트 타입에서 현재 사용자 전용 response의 `userId`를 제거한다.
- 프론트 API client에서 `userId=1` 또는 `?userId=`를 제거한다.
- 회원가입 화면에서는 `GET /api/locations`를 호출하지 않는다.
- 대형 상태 관리 라이브러리를 추가하지 않는다. React state와 작은 hook으로 구현한다.

## 검증 절차
```bash
git diff --check
! rg -n -F -e 'userId=1' -e '?userId=' -e 'userId:' frontend/src
(cd frontend && npm run build)
```

## 인수 기준
- 로그인 form과 회원가입 form이 있다.
- 로그인 성공 후 access token이 `sessionStorage`에 저장된다.
- 새로고침 후 저장된 token으로 `GET /api/users/me`를 호출해 로그인 상태를 복구한다.
- 401이면 token과 user state를 정리하고 로그인 화면으로 전환한다.
- 로그아웃하면 token과 user state가 제거된다.
- 프론트 API 타입에서 `CurrentUserResponse`, 옷, 위치, 선호도, 추천 응답에 `userId`가 없다.
- frontend build가 통과한다.

## 금지사항
- access token을 `localStorage`에 저장하지 마라. 이유: 3차 기준은 `sessionStorage`다.
- refresh token 상태나 자동 재발급 흐름을 만들지 마라. 이유: 3차 제외 범위다.
- 로그인 전 위치 catalog를 호출하지 마라. 이유: `GET /api/locations`는 보호 API이며 로그인 후 흐름이다.
- 컴포넌트에서 `fetch`를 직접 흩뿌리지 마라. 이유: API 함수는 `smartClosetApi.ts`에 모아 계약을 관리한다.
- 프론트 타입에 `userId`를 되살리지 마라. 이유: 현재 사용자 전용 DTO에서는 `userId`를 제거한다.

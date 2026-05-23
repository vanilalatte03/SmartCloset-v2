# 단계 2: responsive-app-shell

범위: Must-have / MVP4 P0

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/FRONTEND.md`
- `docs/design/mvp4/README.md`
- `docs/COMMANDS.md`
- `frontend/src/App.tsx`
- `frontend/src/App.css`
- `frontend/src/features/auth/AuthPanel.tsx`
- `frontend/src/components/**`

이전 단계에서 만들어진 frontend API/label foundation을 확인한 뒤 작업하라.

## 작업
로그인 후 앱을 Today, Closet, Preferences, Location, History 5개 view를 가진 반응형 앱 셸로 전환한다. 이 단계의 핵심은 navigation과 layout skeleton이며, 각 view의 상세 UX는 후속 step에서 구현한다.

## 변경 예상 파일
- `frontend/src/App.tsx`
- `frontend/src/App.css`
- `frontend/src/index.css`
- `frontend/src/components/**`
- `frontend/src/features/auth/AuthPanel.tsx`
- `frontend/src/features/**`

## 구현 메모
- `type AppView = 'today' | 'closet' | 'preferences' | 'location' | 'history'` 구조를 둔다.
- 로그인 후 기본 view는 `today`다.
- 데스크톱은 좌측 sidebar navigation과 top status bar를 사용한다.
- 모바일은 top app bar, single column content, bottom tab navigation을 사용한다.
- 하단 탭은 `오늘`, `옷장`, `선호도`, `위치`, `이력` 5개로 고정한다.
- top status bar에는 사용자 이름/email, 현재 위치, API 상태, 로그아웃을 표시한다.
- Auth 화면 문구는 한국어로 정리하되 소셜 로그인/비밀번호 찾기/이메일 인증 UI는 추가하지 않는다.
- 로그인 전에는 `GET /api/locations`를 호출하지 않는다.
- 기존 `sessionStorage` token 저장과 새로고침 복구 흐름을 유지한다.

## 검증 절차
```bash
git diff --check
rg -n \"type AppView|'today'|'closet'|'preferences'|'location'|'history'|sessionStorage\" frontend/src
! rg -n '소셜|비밀번호 찾기|이메일 인증|GET /api/locations' frontend/src/features/auth frontend/src/App.tsx
(cd frontend && npm run build)
```

## 인수 기준
- 로그인 후 기본 화면이 Today view다.
- desktop navigation과 mobile bottom tab이 같은 5개 view를 전환한다.
- 기존 인증 세션 복구와 로그아웃이 계속 동작한다.
- Auth 화면은 한국어 중심이고 MVP4 제외 기능을 노출하지 않는다.
- 하단 탭은 hover 없이 클릭 가능한 button/nav 구조다.
- 각 view는 후속 step에서 기능을 채울 수 있는 명확한 mount point를 가진다.

## 금지사항
- view별 상세 기능을 한꺼번에 완성하려 하지 마라. 이유: Today, Closet, Preferences, Location, History는 후속 step 책임이다.
- 랜딩 페이지나 마케팅 hero를 만들지 마라. 이유: 첫 화면은 제품 설명이 아니라 Today 작업 화면이어야 한다.
- 회원가입 화면에서 위치 catalog를 호출하지 마라. 이유: `GET /api/locations`는 보호 API이며 로그인 후 위치 선택 흐름에만 사용한다.
- access token을 `localStorage`나 cookie로 옮기지 마라. 이유: 프론트 저장 위치는 `sessionStorage`로 고정한다.
- 카드 안에 카드를 중첩하는 shell 구조를 만들지 마라. 이유: MVP4 UI 기준은 조용하고 밀도 있는 작업 화면이다.

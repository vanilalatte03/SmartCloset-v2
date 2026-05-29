# 단계 1: app-shell-auth-redesign

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/FRONTEND.md`
- `docs/design/mvp9/README.md`
- `frontend/src/App.tsx`
- `frontend/src/App.css`
- `frontend/src/features/auth/AuthPanel.tsx`

## 작업

- Auth view를 `smartcloset-auth-mockup.png`와 `auth-london-editorial.png` 방향으로 리디자인한다.
- Auth 기능은 MVP8 계약을 유지한다: 로그인, 이메일 저장, 회원가입, 이메일 인증, 비밀번호 재설정, Google provider 상태.
- Auth 화면은 full-bleed 또는 넓은 visual background와 중앙 form 구조를 사용하되 모바일에서 form이 잘리지 않게 한다.
- Authenticated shell은 데스크톱 상단 탭과 모바일 하단 탭으로 바꾼다.
- primary nav는 `추천`, `옷장`, `내 취향`, `위치`, `기록`만 둔다.
- `계정 설정`은 primary nav에서 제거하고 우측 상단 profile pill/menu에서 진입하게 한다.
- 개발용 API/status 정보는 사용자의 핵심 동선을 방해하지 않게 축소하거나 보조 위치로 이동한다.

## 인수 기준

```bash
(cd frontend && npm run build)
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. UI 체크리스트를 확인한다:
   - 데스크톱에서 상단 탭이 5개만 보이는가?
   - 모바일에서 하단 탭이 5개만 보이는가?
   - 계정 설정은 profile pill/menu에서 진입하는가?
   - Auth 기능이 기존 API 계약과 상태 흐름을 유지하는가?
   - Auth와 shell이 1440px 데스크톱과 390px 모바일에서 겹침/잘림 없이 보이는가?
3. 결과에 따라 `phases/9-smartcloset-ui-ux-redesign/index.json`의 해당 단계를 업데이트한다:
   - 성공 -> `"status": "completed"`, `"summary": "app shell navigation과 Auth view를 MVP9 디자인 기준으로 리디자인했다."`
   - 수정 3회 시도 후에도 실패 -> `"status": "error"`, `"error_message": "구체적 에러 내용"`
   - 사용자 개입 필요 -> `"status": "blocked"`, `"blocked_reason": "구체적 사유"` 후 즉시 중단

## 금지사항

- access token을 `localStorage`나 `sessionStorage`에 저장하지 마라. 이유: MVP8 이후 세션 정책은 memory state다.
- refresh token 값을 JavaScript state나 JSON body에 추가하지 마라. 이유: refresh token은 HttpOnly cookie 전용이다.
- 계정 설정을 primary nav tab으로 유지하지 마라. 이유: MVP9 navigation 계약은 5개 주요 화면과 profile 진입이다.
- 큰 state-management library를 추가하지 마라. 이유: 현재 앱은 React state와 작은 hook으로 충분하다.

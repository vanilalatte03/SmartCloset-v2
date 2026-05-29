# 단계 6: account-settings

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/FRONTEND.md`
- `docs/design/mvp9/README.md`
- `frontend/src/features/account/AccountSettingsPanel.tsx`
- `frontend/src/App.tsx`
- `frontend/src/App.css`

## 작업

- Account settings 화면을 `smartcloset-account-mockup.png` 방향으로 리디자인한다.
- Profile pill/menu에서 열린 계정 설정이라는 맥락이 분명하게 보이게 한다.
- 이메일 인증 상태, 로그인 제공자, 세션 상태, 계정 삭제 위험 영역을 명확히 분리한다.
- 기존 계정 안정성 UX를 유지한다: 이메일 인증 상태 표시, 비밀번호 재설정 진입, Google provider 상태, 계정 삭제 확인 조건.
- 1440px 데스크톱과 390px 모바일에서 Account settings 화면의 겹침/잘림을 점검하고 수정한다.

## 인수 기준

```bash
(cd frontend && npm run build)
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. Account 체크리스트를 확인한다:
   - 390px 모바일에서 계정 설정 버튼 텍스트와 입력값이 parent를 넘지 않는가?
   - 이메일 인증 상태, 로그인 제공자, 세션 상태가 기존 API 상태 흐름과 맞게 표시되는가?
   - 계정 삭제 확인 문구와 password 조건이 유지되는가?
   - 5개 주요 화면 외에 계정 설정 탭이 다시 생기지 않았는가?
   - Shell/global focus/hover polish를 함께 수행하지 않았는가?
3. 결과에 따라 `phases/9-smartcloset-ui-ux-redesign/index.json`의 해당 단계를 업데이트한다:
   - 성공 -> `"status": "completed"`, `"summary": "계정 설정 화면을 MVP9 디자인 기준으로 리디자인했다."`
   - 수정 3회 시도 후에도 실패 -> `"status": "error"`, `"error_message": "구체적 에러 내용"`
   - 사용자 개입 필요 -> `"status": "blocked"`, `"blocked_reason": "구체적 사유"` 후 즉시 중단

## 금지사항

- 계정 삭제 정책을 soft delete로 바꾸지 마라. 이유: MVP8 결정은 즉시 hard delete다.
- Password 계정 삭제에서 현재 비밀번호 확인을 제거하지 마라. 이유: 계정 삭제 보안 계약이다.
- Google-only 계정 삭제 confirmation string 조건을 제거하지 마라. 이유: 기존 계정 삭제 UX 계약이다.
- 화면 polish를 위해 API DTO를 변경하지 마라. 이유: MVP9는 프론트 리디자인 MVP다.
- Shell/global focus/hover polish를 함께 수행하지 마라. 이유: Step 7의 별도 범위다.
- Step 1-5 담당 화면을 광범위하게 다시 리디자인하지 마라. 이유: Step 6은 계정 설정 화면으로 범위를 제한한다.

# 단계 7: global-focus-hover-polish

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/FRONTEND.md`
- `docs/design/mvp9/README.md`
- `frontend/src/App.tsx`
- `frontend/src/App.css`
- `frontend/src/features/account/AccountSettingsPanel.tsx`

## 작업

- Shell/shared 수준의 focus-visible, hover, disabled, loading 상태를 주요 CTA와 입력을 가리지 않게 정리한다.
- Primary nav, bottom nav, profile pill/menu, 공통 button/input/chip/card 상태를 MVP9 디자인 기준에 맞춘다.
- Hover 없이도 주요 CTA와 navigation이 식별 가능하게 유지한다.
- 1440px 데스크톱과 390px 모바일에서 shell 공통 UI가 레이아웃 shift, 겹침, 잘림을 만들지 않는지 점검하고 수정한다.

## 인수 기준

```bash
(cd frontend && npm run build)
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. Shell/shared polish 체크리스트를 확인한다:
   - focus-visible outline이 주요 CTA, input text, navigation label을 가리지 않는가?
   - hover가 없는 터치 환경에서도 주요 CTA와 navigation 상태가 명확한가?
   - disabled/loading 상태가 layout shift를 만들지 않는가?
   - 5개 주요 화면 외에 계정 설정 탭이 다시 생기지 않았는가?
   - Step 1-6 담당 화면을 광범위하게 다시 리디자인하지 않았는가?
3. 결과에 따라 `phases/9-smartcloset-ui-ux-redesign/index.json`의 해당 단계를 업데이트한다:
   - 성공 -> `"status": "completed"`, `"summary": "Shell/shared focus, hover, disabled, loading 상태를 MVP9 기준으로 정리했다."`
   - 수정 3회 시도 후에도 실패 -> `"status": "error"`, `"error_message": "구체적 에러 내용"`
   - 사용자 개입 필요 -> `"status": "blocked"`, `"blocked_reason": "구체적 사유"` 후 즉시 중단

## 금지사항

- Account settings 화면을 다시 리디자인하지 마라. 이유: Step 6의 별도 범위다.
- Step 1-6 담당 화면 layout을 광범위하게 다시 리디자인하지 마라. 이유: Step 7은 shell/shared 상태 polish로 범위를 제한한다.
- 화면 polish를 위해 API DTO를 변경하지 마라. 이유: MVP9는 프론트 리디자인 MVP다.

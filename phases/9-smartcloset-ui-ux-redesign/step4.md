# 단계 4: preferences-location

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/FRONTEND.md`
- `docs/design/mvp9/README.md`
- `frontend/src/features/preferences/PreferencesPanel.tsx`
- `frontend/src/features/location/LocationPanel.tsx`
- `frontend/src/App.css`

## 작업

- Preferences 화면을 `smartcloset-preferences-mockup.png` 방향으로 리디자인한다.
- Location 화면을 `smartcloset-location-mockup.png` 방향으로 리디자인한다.
- 취향 입력은 scan 가능한 group, chip, toggle, segmented control 중심으로 정리한다.
- 위치 화면은 지도 없이 동네 검색과 현재 위치 후보 찾기 흐름을 유지한다.
- 1440px 데스크톱과 390px 모바일에서 Preferences와 Location 화면의 겹침/잘림을 점검하고 수정한다.

## 인수 기준

```bash
(cd frontend && npm run build)
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. Preferences/Location 체크리스트를 확인한다:
   - 취향 API와 기존 enum/string 계약을 변경하지 않았는가?
   - 위치 검색은 내부 KMA catalog 기준을 유지하는가?
   - 브라우저 GPS 원문 좌표를 저장하는 UI처럼 보이지 않는가?
   - 두 화면이 1440px 데스크톱과 390px 모바일에서 겹침/잘림 없이 보이는가?
3. 결과에 따라 `phases/9-smartcloset-ui-ux-redesign/index.json`의 해당 단계를 업데이트한다:
   - 성공 -> `"status": "completed"`, `"summary": "Preferences와 Location 화면을 MVP9 디자인 기준으로 리디자인했다."`
   - 수정 3회 시도 후에도 실패 -> `"status": "error"`, `"error_message": "구체적 에러 내용"`
   - 사용자 개입 필요 -> `"status": "blocked"`, `"blocked_reason": "구체적 사유"` 후 즉시 중단

## 금지사항

- 외부 지도/주소 API를 추가하지 마라. 이유: 위치는 내부 KMA catalog 기준이다.
- 브라우저 GPS 원문 좌표를 DB에 저장하는 기능을 추가하지 마라. 이유: 좌표는 후보 찾기에만 사용한다.
- Preferences 저장 DTO나 Location API contract를 변경하지 마라. 이유: MVP9는 프론트 리디자인 MVP다.
- Closet 또는 History 화면을 함께 리디자인하지 마라. 이유: 해당 화면은 별도 step 범위다.

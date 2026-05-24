# 단계 12: closet-preferences-card-polish

범위: Should-have / MVP4 P1

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/FRONTEND.md`
- `docs/API.md`
- `docs/design/mvp4/README.md`
- `docs/design/mvp4/desktop/closet.png`
- `docs/design/mvp4/mobile/closet.png`
- `docs/design/mvp4/desktop/preferences.png`
- `docs/design/mvp4/mobile/preferences.png`
- `frontend/src/features/clothes/ClosetPanel.tsx`
- `frontend/src/features/preferences/PreferencesPanel.tsx`
- `frontend/src/components/DisplayTokens.tsx`
- `frontend/src/utils/displayMappings.ts`
- `frontend/src/App.css`

이전 step에서 변경된 Today/status visual polish를 확인하고, 공통 token 스타일과 충돌하지 않게 작업하라.

## 작업
Closet view를 옷 카드 그리드와 빠른 등록 패널로 분리하고, Preferences view를 원형 색상 swatch, 소재 chip, style tag 영역, 명확한 저장 CTA 중심으로 polish한다.

## 변경 예상 파일
- `frontend/src/features/clothes/ClosetPanel.tsx`
- `frontend/src/features/preferences/PreferencesPanel.tsx`
- `frontend/src/components/DisplayTokens.tsx`
- `frontend/src/App.css`

## 구현 메모
- Closet 데스크톱 레이아웃은 왼쪽에 옷 카드 그리드, 오른쪽에 빠른 등록/수정 패널을 둔다.
- 모바일에서는 한 컬럼으로 쌓되, 카테고리 필터와 카드 액션이 hover 없이 터치 가능해야 한다.
- 기존 행 목록은 카드형으로 바꾼다. 각 옷 카드는 name, category, 컬러 블록 또는 원형 swatch, 소재 chip, 기온 범위, 비 적합 badge, 수정/보관 버튼을 보여준다.
- 카테고리 icon-like visual은 텍스트/기호/CSS 블록으로 처리하고, 이미지 업로드나 SVG 자산 생성은 하지 않는다.
- 빠른 등록 패널은 기존 `ClothingRequest` shape를 그대로 사용하고, 계절/기온 프리셋은 helper로만 유지한다.
- Preferences 색상 선택은 텍스트 버튼보다 원형 swatch가 먼저 보이게 한다. 색상명은 접근성과 확인을 위해 작게 유지할 수 있다.
- Preferences 소재 선택은 chip multi-select로 유지하되 선택 상태가 명확해야 한다.
- style tag 입력/삭제 영역은 색상/소재 선택과 시각적으로 분리한다.
- 저장 CTA는 데스크톱에서 우측/하단의 명확한 primary action으로, 모바일에서 하단 sticky 또는 마지막 영역에서 놓치지 않게 배치한다.
- `styleTags`는 저장/조회/표시만 하고 추천 점수나 추천 이유에 반영된다는 표현을 쓰지 않는다.

## 검증 절차
```bash
git diff --check
rg -n 'createClothing|updateClothing|archiveClothing|preferredColors|preferredMaterials|styleTags|swatch|chip|저장' frontend/src/features/clothes frontend/src/features/preferences frontend/src/App.css
! rg -n 'image|upload|file|styleTags.*점수|styleTags.*이유|styleTags.*추천.*반영|preferenceScore.*styleTags' frontend/src/features/clothes frontend/src/features/preferences
(cd frontend && npm run build)
```

가능하면 로컬 프론트에서 데스크톱 1366px와 모바일 375px 화면을 확인한다:
- Closet은 데스크톱에서 카드 그리드와 빠른 등록 패널이 분리되어 보인다.
- 옷 카드의 긴 이름, 기온 범위, 버튼이 카드 밖으로 넘치지 않는다.
- Preferences 색상 swatch와 소재 chip의 선택 상태가 시각적으로 명확하다.
- 저장 CTA가 모바일 하단 탭이나 다른 콘텐츠와 겹치지 않는다.

## 인수 기준
- Closet view가 행 목록 + 긴 폼이 아니라 카드 그리드 + 빠른 등록 패널 구조로 보인다.
- 옷 카드는 이미지 없이 category visual, 색상 swatch/block, 소재 chip, 기온/비 badge를 제공한다.
- 옷 수정과 보관 처리는 기존 API로 계속 동작한다.
- Preferences view에서 색상은 원형 swatch 중심, 소재는 chip 중심, style tag는 별도 입력 영역으로 분리되어 있다.
- Preferences 저장 payload는 `preferredColors`, `preferredMaterials`, `styleTags` 배열 계약을 유지한다.
- `cd frontend && npm run build`가 통과한다.

## 금지사항
- 이미지 업로드, drag-and-drop upload, file metadata를 추가하지 마라. 이유: MVP4 제외 범위다.
- 옷 API request/response shape를 변경하지 마라. 이유: 기존 backend 계약을 유지해야 한다.
- 선호도 별도 테이블이나 새 preferences API를 요구하지 마라. 이유: MVP4는 users JSON string column 기반이다.
- `styleTags`를 score, tie-breaker, recommendation reason과 연결하지 마라. 이유: 현재 baseline에서 저장/조회/표시만 한다.
- 큰 form library나 state management library를 추가하지 마라. 이유: React state와 작은 hook 기준이다.

# 단계 5: frontend-feedback-personalization-ux

## 읽어야 할 파일

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/API.md`
- `docs/FRONTEND.md`
- `docs/DEMO_SCENARIO.md`
- `frontend/src/types/api.ts`
- `frontend/src/api/smartClosetApi.ts`
- `frontend/src/features/clothes/ClosetPanel.tsx`
- `frontend/src/features/recommendation/RecommendationPanel.tsx`
- `frontend/src/features/history/HistoryPanel.tsx`
- `frontend/src/App.css`

## 작업

MVP6 프론트 API 타입과 UX를 구현한다.

- `RecommendationSituation`, feedback enum, request/response type을 추가한다.
- `createRecommendation(accessToken, request?)`가 optional body를 받을 수 있게 한다.
- `replaceRecommendationFeedback` API helper를 추가하고 반환 타입을 `RecommendationFeedbackResponse` wrapper로 둔다.
- `ClothingRequest`, `ClothingResponse`, `OutfitItemResponse`에 `styleTags`를 추가한다.
- Closet view에 styleTags 입력/표시를 추가한다.
- Today recommendation view에 상황 선택 control을 추가한다.
- 추천 결과에 피드백 control을 추가한다.
- History view에 상황, `wornAt`, feedback 상태를 표시한다.
- 저장/실패/인증 만료 상태를 기존 패턴으로 처리한다.
- 모바일 375px에서 버튼/텍스트가 겹치지 않도록 CSS를 조정한다.

## 인수 기준

```bash
(cd frontend && npm run build)
git diff --check
```

## 검증 절차

1. TypeScript build가 통과하는지 확인한다.
2. 상황 기본 선택이 `CASUAL`인지 확인한다.
3. 피드백 버튼이 전체 교체/clear request를 만들 수 있는지 확인한다.
4. History view에서 상황, 착용 여부, 착용 시각, 피드백 상태가 보이는지 확인한다.
5. 기존 이미지 blob fetch와 object URL cleanup이 유지되는지 확인한다.

## 금지사항

- 큰 상태 관리 라이브러리를 추가하지 마라. 이유: 현재 프론트는 React state와 작은 hook 기준이다.
- 보호 이미지를 일반 public `<img src>`로 직접 참조하지 마라. 이유: 이미지 조회는 Authorization header가 필요하다.
- 랜딩 페이지를 만들지 마라. 이유: SmartCloset은 로그인 후 실제 앱 UX가 우선이다.

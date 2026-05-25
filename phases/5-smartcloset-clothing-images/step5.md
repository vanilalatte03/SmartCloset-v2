# 단계 5: recommendation-thumbnail-ux

## 읽어야 할 파일

- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/FRONTEND.md`
- `docs/RECOMMENDATION_RULES.md`
- `frontend/src/types/api.ts`
- `frontend/src/features/recommendation/RecommendationPanel.tsx`
- `frontend/src/features/history/HistoryPanel.tsx`
- `frontend/src/components/DisplayTokens.tsx`
- `frontend/src/App.css`
- `frontend/src/index.css`

## 작업

추천 결과와 추천 이력에 썸네일 표시를 추가한다.

- 추천 outfit item의 `image` metadata를 사용한다.
- 이미지가 있으면 authenticated thumbnail을 표시한다.
- 이미지가 없거나 fetch가 실패하면 fallback visual을 표시한다.
- color swatch와 material chip은 계속 표시한다.
- Today 추천 결과와 History 추천 이력 모두 모바일에서 overflow 없이 보여야 한다.
- 이미지 fetch 실패가 추천 생성 실패처럼 보이지 않게 한다.

## 인수 기준

```bash
(cd frontend && npm run build)
git diff --check
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. 이미지가 있는 추천과 이미지가 없는 추천이 모두 자연스럽게 보이는지 확인한다.
3. 성공하면 phase index의 Step 5를 completed로 갱신한다.

## 금지사항

- 이미지가 없는 옷을 추천 결과에서 숨기지 마라. 이유: 이미지는 추천 필수 조건이 아니다.
- 추천 score UI를 이미지 중심으로 재해석하지 마라. 이유: scoring rule은 변경하지 않는다.

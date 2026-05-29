# 단계 2: recommendation-dashboard

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/FRONTEND.md`
- `docs/RECOMMENDATION_RULES.md`
- `docs/design/mvp9/README.md`
- `frontend/src/features/today/TodayPanel.tsx`
- `frontend/src/features/recommendation/RecommendationPanel.tsx`
- `frontend/src/App.css`

## 작업

- 추천 화면을 `smartcloset-recommend-mockup.png` 방향의 dashboard로 리디자인한다.
- 날씨, 위치, 상황, 예보 시간대, 옷장 준비 상태, 최근 이력을 한 화면에서 스캔할 수 있게 한다.
- 추천 결과는 점수표보다 옷 조합과 "오늘 입기 좋은 이유"를 먼저 보여준다.
- 점수 상세는 보조 panel로 유지한다.
- 추천 상황은 segmented/card control, 예보 시간대는 segmented control로 표시한다.
- 추천 실패는 내부 failure code보다 한국어 안내와 해결 CTA를 우선 표시한다.

## 인수 기준

```bash
(cd frontend && npm run build)
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. 추천 체크리스트를 확인한다:
   - `POST /api/recommendations` 계약을 그대로 사용하는가?
   - `situation`과 `forecastPeriod`가 기존 enum 계약을 유지하는가?
   - 추천 점수, 후보 필터, tie-break 변경이 없는가?
   - 이미지가 없는 옷도 fallback visual로 식별 가능한가?
   - 추천 dashboard가 1440px 데스크톱과 390px 모바일에서 겹침/잘림 없이 보이는가?
3. 결과에 따라 `phases/9-smartcloset-ui-ux-redesign/index.json`의 해당 단계를 업데이트한다:
   - 성공 -> `"status": "completed"`, `"summary": "추천 dashboard를 MVP9 디자인 기준으로 리디자인했다."`
   - 수정 3회 시도 후에도 실패 -> `"status": "error"`, `"error_message": "구체적 에러 내용"`
   - 사용자 개입 필요 -> `"status": "blocked"`, `"blocked_reason": "구체적 사유"` 후 즉시 중단

## 금지사항

- 추천 점수 계산, 후보 필터, tie-break, 추천 이유 생성 로직을 바꾸지 마라. 이유: MVP9는 UI/UX 리디자인 MVP다.
- today 추천 GET endpoint를 추가하지 마라. 이유: 현재 계약은 `POST /api/recommendations`다.
- 이미지 metadata를 추천 점수나 이유에 사용하지 마라. 이유: 이미지 존재 여부는 추천 품질 신호가 아니다.
- AI/GPT 추천처럼 보이는 문구를 추가하지 마라. 이유: 추천은 규칙 기반이다.

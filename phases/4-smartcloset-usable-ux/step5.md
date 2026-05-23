# 단계 5: recommendation-guidance-ux

범위: Must-have / MVP4 P0

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/FRONTEND.md`
- `docs/API.md`
- `docs/RECOMMENDATION_RULES.md`
- `docs/DEMO_SCENARIO.md`
- `frontend/src/features/recommendation/RecommendationPanel.tsx`
- `frontend/src/api/errorHelpers.ts`
- `frontend/src/api/smartClosetApi.ts`
- `frontend/src/types/api.ts`
- `frontend/src/App.css`

이전 단계의 Today readiness와 Closet quick manage 흐름을 확인한 뒤 작업하라.

## 작업
추천 생성 UX를 내부 코드/점수표 중심에서 사용자 안내 중심으로 바꾼다. 성공 시 옷 조합과 "오늘 입기 좋은 이유"를 먼저 보여주고, 실패 시 한국어 메시지와 직접 CTA로 해결 경로를 제공한다.

## 변경 예상 파일
- `frontend/src/features/recommendation/**`
- `frontend/src/features/today/**`
- `frontend/src/api/errorHelpers.ts`
- `frontend/src/App.tsx`
- `frontend/src/App.css`

## 구현 메모
- 추천 생성은 `POST /api/recommendations`만 사용한다.
- 추천 성공 표시 순서:
  1. 추천 옷 조합: 상의, 하의, 아우터
  2. 오늘 입기 좋은 이유: `reasons`
  3. 착용 완료 CTA
  4. 점수 상세: 접힘 또는 보조 영역
- 추천 응답의 `weather` snapshot은 추천 생성 시점 날씨로 표시한다.
- `outfit.outer`는 `null`일 수 있음을 처리한다.
- `score.preferenceScore`를 표시하고 기존 다양성 점수 표현은 쓰지 않는다.
- 추천 실패 코드 5종은 Step 1 mapping을 사용해 한국어 메시지와 CTA로 보여준다.
- 실패 CTA는 `closet` view로 이동하고 가능하면 category 기본값을 맞춘다.
- 401은 인증 만료로 처리해 auth 흐름으로 보낸다.
- `styleTags`가 추천 이유나 점수에 영향을 준다는 문구를 쓰지 않는다.

## 검증 절차
```bash
git diff --check
rg -n '오늘 입기 좋은 이유|preferenceScore|NO_TOP_AVAILABLE|NO_BOTTOM_AVAILABLE|OUTER_REQUIRED_BUT_NOT_AVAILABLE|INSUFFICIENT_CLOSET_ITEMS|NO_WEATHER_SUITABLE_ITEM' frontend/src
! rg -n 'diversity|다양성|recommendations/today|userId|styleTags.*점수|styleTags.*이유' frontend/src
(cd frontend && npm run build)
```

## 인수 기준
- 추천 성공 화면은 옷 조합과 이유를 점수보다 먼저 보여준다.
- 추천 실패 화면은 내부 코드만 노출하지 않고 한국어 메시지와 CTA를 보여준다.
- 실패 CTA가 Closet view로 이동한다.
- 착용 완료 CTA가 성공 결과에서 접근 가능하다. History 화면 전체 polish는 Step 10에서 수행한다.
- 추천 점수 상세에는 `preferenceScore`가 있고 다양성 점수 표현이 없다.
- `styleTags`는 추천 점수/이유에 반영되는 것처럼 보이지 않는다.

## 금지사항
- 추천 scoring, tie-break, 실패 코드를 변경하지 마라. 이유: MVP4는 UI 표시만 변경한다.
- AI/GPT 추천처럼 보이는 문구를 넣지 마라. 이유: 추천은 규칙 기반이다.
- 추천 실패를 frontend에서 임의로 성공 처리하지 마라. 이유: business failure는 backend API 계약이다.
- `GET /api/weather/current`를 추천 결과로 사용하지 마라. 이유: 추천 결과는 `POST /api/recommendations` 응답이다.
- raw failure code를 화면의 유일한 설명으로 노출하지 마라. 이유: MVP4 목표는 사용자 언어 안내다.

# 단계 10: history-worn-ux

범위: Should-have / MVP4 P1

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/FRONTEND.md`
- `docs/API.md`
- `docs/DEMO_SCENARIO.md`
- `frontend/src/features/recommendation/RecommendationPanel.tsx`
- `frontend/src/api/smartClosetApi.ts`
- `frontend/src/types/api.ts`
- `frontend/src/App.css`

이전 P0 release candidate, recommendation guidance, label helper를 확인한 뒤 작업하라.

## 작업
History view를 최신순 추천 이력 카드, 착용 여부, 착용 완료 처리, 상세 보기 또는 확장 카드 중심으로 구성한다.

## 변경 예상 파일
- `frontend/src/features/recommendation/**`
- `frontend/src/features/history/**`
- `frontend/src/App.tsx`
- `frontend/src/App.css`

## 구현 메모
- `GET /api/recommendations?limit=20`을 기본 조회한다.
- limit 정책은 backend 계약을 신뢰한다. UI에서 기본 20을 사용한다.
- 이력은 최신순으로 표시한다.
- 각 카드에는 추천 옷 조합, 날씨 snapshot, "오늘 입기 좋은 이유", 착용 여부를 표시한다.
- 착용 완료는 `PATCH /api/recommendations/{recommendationId}/worn`을 호출한다.
- 착용 완료는 idempotent API이므로 이미 `worn=true`인 항목도 UI가 안정적으로 처리해야 한다.
- 긴 날짜/옷 이름이 모바일 카드 밖으로 넘치지 않도록 CSS를 조정한다.
- Today의 최근 이력 preview와 History view가 같은 데이터 계약을 사용하게 한다.

## 검증 절차
```bash
git diff --check
rg -n 'getRecommendationHistory|markRecommendationWorn|worn|limit=20|착용|이력' frontend/src
! rg -n 'recommendations/today|userId|limit=0|limit=100' frontend/src
(cd frontend && npm run build)
```

## 인수 기준
- History view에서 추천 이력을 최신순으로 확인할 수 있다.
- 이력 카드에서 착용 여부와 추천 옷 조합을 볼 수 있다.
- 착용 완료 처리가 가능하고 처리 후 UI 상태가 갱신된다.
- Today 최근 이력 preview와 History view가 충돌하지 않는다.
- 모바일에서 긴 텍스트가 카드 밖으로 넘치지 않는다.

## 금지사항
- 추천 이력 API limit 범위를 frontend 임의 계약으로 바꾸지 마라. 이유: backend 기준은 기본 20, 1..50, 최신순이다.
- 착용 완료를 frontend-only 상태로만 처리하지 마라. 이유: API로 서버 상태를 갱신해야 한다.
- 이력에서 `userId`를 표시하거나 요구하지 마라. 이유: 현재 사용자 전용 DTO는 `userId`를 노출하지 않는다.
- weather current API를 이력 source로 사용하지 마라. 이유: 이력 날씨는 추천 응답의 weather snapshot이다.

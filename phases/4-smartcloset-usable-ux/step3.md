# 단계 3: today-readiness-weather

범위: Must-have / MVP4 P0

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/FRONTEND.md`
- `docs/API.md`
- `docs/DEMO_SCENARIO.md`
- `docs/design/mvp4/README.md`
- `frontend/src/App.tsx`
- `frontend/src/api/smartClosetApi.ts`
- `frontend/src/types/api.ts`
- `frontend/src/features/recommendation/RecommendationPanel.tsx`
- `frontend/src/features/clothes/ClosetPanel.tsx`
- `frontend/src/features/preferences/PreferencesPanel.tsx`
- `frontend/src/features/location/LocationPanel.tsx`

이전 단계의 app shell과 API foundation을 확인한 뒤 작업하라.

## 작업
Today view를 첫 추천 준비 화면으로 만든다. 현재 위치와 현재 날씨 요약, 첫 추천 준비 체크리스트, 주요 추천 생성 CTA skeleton, 최근 이력 preview 영역을 제공한다.

## 변경 예상 파일
- `frontend/src/features/recommendation/**`
- `frontend/src/features/today/**`
- `frontend/src/App.tsx`
- `frontend/src/App.css`
- `frontend/src/api/smartClosetApi.ts`
- `frontend/src/types/api.ts`

## 구현 메모
- Today view는 로그인 후 기본 view다.
- 현재 위치는 `GET /api/users/me/location` 응답을 사용한다.
- 현재 날씨 요약은 `GET /api/weather/current` 응답을 사용한다.
- 날씨 요약 조회 실패가 체크리스트, 옷장 이동, 추천 CTA 접근을 막지 않게 한다.
- 위치가 변경되면 현재 날씨 요약을 다시 호출할 수 있도록 상태 흐름을 연결한다.
- 체크리스트 항목은 아래 기준으로 계산한다:
  - 위치 확인: location 응답 존재
  - 선호도 저장/확인: 사용자가 저장했거나 기본값을 확인한 상태
  - 상의 등록: `GET /api/clothes` 결과 중 `category=TOP`, `archived=false` 1개 이상
  - 하의 등록: `category=BOTTOM`, `archived=false` 1개 이상
  - 아우터 등록: `category=OUTER`, `archived=false` 1개 이상
- fallback 날씨 `temperature=12`에서는 OUTER가 추천 성공에 필요하므로 아우터 항목을 명확히 보여준다.
- CTA는 필요한 다음 view로 이동할 수 있어야 한다. 옷 등록 CTA는 가능하면 category 기본값을 함께 전달할 수 있는 구조로 둔다.
- 최근 이력 preview는 `GET /api/recommendations?limit=20` 중 일부를 보여주되, 전체 History UX는 Step 10에서 다룬다.

## 검증 절차
```bash
git diff --check
rg -n 'getCurrentWeather|첫 추천|체크리스트|OUTER|weather/current|recommendations\\?limit' frontend/src
! rg -n 'recommendations/today|userId' frontend/src
(cd frontend && npm run build)
```

## 인수 기준
- Today view에서 현재 위치와 현재 날씨 요약이 보인다.
- 체크리스트가 위치, 선호도, TOP, BOTTOM, OUTER 상태를 분리해 보여준다.
- 날씨 조회 실패가 첫 추천 준비 흐름 전체를 막지 않는다.
- 체크리스트 CTA가 해당 view로 이동한다.
- 추천 생성 CTA가 Today 화면의 주요 행동으로 자리 잡는다.
- 최근 이력 preview 영역이 존재하되 전체 이력 관리는 Step 10 범위로 남는다.

## 금지사항
- 추천 생성 API를 `GET`으로 호출하지 마라. 이유: 추천 생성은 `POST /api/recommendations`만 사용한다.
- 날씨 요약 API 응답을 추천 결과처럼 저장하거나 이력에 표시하지 마라. 이유: `GET /api/weather/current`는 현재 날씨 조회 전용이다.
- 옷 등록/수정/보관 폼을 이 단계에서 확장하지 마라. 이유: Closet UX는 Step 4 책임이다.
- `styleTags`를 체크리스트 완료 기준으로 삼지 마라. 이유: 선호도 저장/확인 흐름만 필요하며 style tag scoring은 없다.
- 내부 enum이나 실패 코드를 사용자 화면의 주 문구로 노출하지 마라. 이유: MVP4는 사용자 언어 변환이 목표다.

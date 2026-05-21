# 단계 4: frontend-core-flows

범위: Must-have / 2차 P0

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/FRONTEND.md`
- `docs/API.md`
- `docs/PRD.md`
- `docs/COMMANDS.md`
- `phases/2-smartcloset-location-frontend/step3.md`
- `frontend/package.json`
- `frontend/src/**`

이전 단계에서 만들어진 프론트 구조와 API client 골격을 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업
React 앱에서 2차 핵심 사용자 흐름을 실제로 수행할 수 있게 만든다. 단일 화면 대시보드로 위치 선택, 옷 목록/등록, 추천 생성/결과 표시, 착용 완료 처리를 구현한다.

## 변경 예상 파일
- `frontend/src/types/api.ts`
- `frontend/src/api/client.ts`
- `frontend/src/api/smartClosetApi.ts`
- `frontend/src/features/location/**`
- `frontend/src/features/clothes/**`
- `frontend/src/features/recommendation/**`
- `frontend/src/components/**`
- `frontend/src/App.tsx`
- `frontend/src/App.css` 또는 동등한 CSS 파일

## 구현 메모
- 필수 API client 함수:
  - `getLocations(keyword?: string)`
  - `getUserLocation(userId: number)`
  - `updateUserLocation(userId: number, locationCode: string)`
  - `getClothes(userId: number)`
  - `createClothing(userId: number, request: ClothingRequest)`
  - `createRecommendation(userId: number)`
  - `markRecommendationWorn(userId: number, recommendationId: number)`
- DTO 타입은 `docs/API.md`와 `docs/FRONTEND.md`의 wire shape를 따른다.
- 실패 응답의 `code`, `message`, `details`를 화면 상태로 표시한다.
- 추천 실패 코드 5종은 사용자가 이해할 수 있는 상태로 보여준다.
- `userId=1`을 기본값으로 제공한다. 입력으로 바꿀 수 있게 하는 것은 허용하되 인증처럼 보이게 만들지 않는다.
- 디자인은 운영 도구처럼 조용하고 밀도 있게 구성한다.
- 모바일에서는 위치, 옷장, 추천 패널이 세로로 쌓이게 한다.
- 옷 등록 form에는 category, color, material select와 온도 number input, rainSuitable checkbox를 둔다.

## 검증 절차
```bash
git diff --check
! rg -n 'GET /api/recommendations/(today)' . --glob '!archive/**'
./gradlew test
cd frontend && npm run build
```

## 인수 기준
- 앱 첫 화면에서 현재 사용자 위치가 표시된다.
- keyword로 위치를 검색하고 catalog 항목을 선택할 수 있다.
- 위치 변경 성공 후 현재 위치 표시가 갱신된다.
- 옷 목록이 표시된다.
- 새 옷을 등록하면 목록이 갱신된다.
- 추천 생성 결과에 weather snapshot, outfit top/bottom/outer, score breakdown, reasons가 표시된다.
- 착용 완료 버튼을 누르면 worn 상태가 반영된다.
- API 실패와 추천 실패 코드가 화면에 표시된다.
- `npm run build`가 통과한다.

## 금지사항
- 컴포넌트에서 직접 `fetch`를 호출하지 마라. 이유: API 호출은 typed API client로 집중해야 한다.
- 백엔드 추천 규칙을 프론트에 복제하지 마라. 이유: 점수 계산과 후보 생성은 백엔드 도메인 책임이다.
- React Router를 필수 구조로 만들지 마라. 이유: 2차는 단일 화면 대시보드로 충분하다.
- 랜딩 페이지나 마케팅 hero를 만들지 마라. 이유: 첫 화면에서 바로 작업을 수행해야 한다.
- today 추천 GET 경로를 호출하지 마라. 이유: 금지된 API 계약이다.

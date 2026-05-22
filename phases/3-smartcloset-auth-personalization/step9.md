# 단계 9: frontend-personalization-flows

범위: Must-have / 3차 P0

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/FRONTEND.md`
- `docs/API.md`
- `docs/DEMO_SCENARIO.md`
- `docs/COMMANDS.md`
- `frontend/src/api/**`
- `frontend/src/types/**`
- `frontend/src/App.tsx`
- `frontend/src/features/**`

이전 단계에서 만들어진 코드를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업
로그인 후 제품 화면에서 위치, 선호도, 옷장, 추천 생성, 추천 이력, 착용 완료 흐름을 3차 API에 연결한다. API 호출은 모두 인증 사용자 기준이어야 하며 `userId` query parameter를 사용하지 않는다.

## 변경 예상 파일
- `frontend/src/features/location/**`
- `frontend/src/features/preferences/**`
- `frontend/src/features/clothes/**`
- `frontend/src/features/recommendation/**`
- `frontend/src/components/**`
- `frontend/src/api/smartClosetApi.ts`
- `frontend/src/types/api.ts`
- `frontend/src/App.tsx`
- `frontend/src/App.css` 또는 현재 스타일 파일

## 구현 메모
- Logged-in shell에는 현재 사용자 name/email, API 상태, 로그아웃 버튼을 둔다.
- Location panel:
  - 현재 사용자 위치 조회
  - keyword 검색
  - catalog 선택
  - 401은 인증 만료로 처리
- Preferences panel:
  - 선호 색상 multi-select 또는 checkbox group
  - 선호 소재 multi-select 또는 checkbox group
  - styleTags 입력/목록 표시
  - 저장/조회
- Closet panel:
  - 활성 옷 목록
  - 옷 등록 form
  - category, color, material select
  - min/max temperature input
  - rainSuitable checkbox
  - 상세/수정/archive가 기존 화면에 있다면 3차 API로 유지
- Recommendation panel:
  - `POST /api/recommendations`
  - weather snapshot
  - outfit top/bottom/outer
  - score breakdown
  - `preferenceScore` 표시
  - reasons
  - worn 처리
- Recommendation history panel:
  - `GET /api/recommendations?limit=20` 기본 호출
  - 최신순 목록
  - limit 1..50 처리
- 백엔드 실패 응답의 `code`, `message`, `details`를 화면 상태로 표시한다.
- 운영 도구처럼 조용하고 밀도 있는 화면을 유지하고, 랜딩 페이지나 marketing hero를 만들지 않는다.

## 검증 절차
```bash
git diff --check
! rg -n 'userId=1|\\?userId=|today' frontend/src
rg -n 'preferenceScore|sessionStorage|/api/recommendations' frontend/src
(cd frontend && npm run build)
```

## 인수 기준
- 로그인 후 현재 위치가 표시되고 위치 검색/선택이 동작한다.
- 로그인 전에는 위치 catalog를 호출하지 않는다.
- 선호도 기본값 빈 배열을 표시하고 저장/조회할 수 있다.
- styleTags는 화면에 표시되지만 점수 설명처럼 표현하지 않는다.
- 옷 등록 후 목록이 갱신된다.
- 추천 생성 결과에 weather, outfit, score, reasons, `preferenceScore`가 표시된다.
- 기존 다양성 점수 필드는 표시되지 않는다.
- 추천 이력을 최신순으로 확인할 수 있다.
- 착용 완료 버튼을 누르면 worn 상태가 반영된다.
- 401 응답은 인증 만료로 처리해 로그인 화면으로 전환한다.
- frontend build가 통과한다.

## 금지사항
- 프론트에서 `?userId=`를 붙이지 마라. 이유: 3차 API는 인증 principal 기준이다.
- today 추천 GET 경로를 호출하지 마라. 이유: 추천 생성은 `POST /api/recommendations`만 사용한다.
- `styleTags`가 추천 점수에 반영된 것처럼 문구를 쓰지 마라. 이유: 3차에서는 저장/조회/표시만 한다.
- 대형 상태 관리 라이브러리를 추가하지 마라. 이유: 3차는 React state와 작은 hook으로 충분하다.
- 과도한 랜딩 페이지를 만들지 마라. 이유: 첫 화면은 실제 사용 가능한 auth/product flow여야 한다.

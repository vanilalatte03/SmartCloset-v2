# 단계 1: frontend-api-label-foundation

범위: Must-have / MVP4 P0

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/FRONTEND.md`
- `docs/API.md`
- `docs/RECOMMENDATION_RULES.md`
- `docs/COMMANDS.md`
- `docs/design/mvp4/README.md`
- `frontend/src/api/client.ts`
- `frontend/src/api/errorHelpers.ts`
- `frontend/src/api/smartClosetApi.ts`
- `frontend/src/types/api.ts`
- `frontend/src/features/**`

이전 단계에서 구현된 `GET /api/weather/current` 계약을 확인한 뒤 작업하라.

## 작업
MVP4 화면들이 공통으로 사용할 frontend API 함수, TypeScript 타입, 한국어 라벨, color swatch, material chip, weather label, 추천 실패 CTA mapping을 준비한다. 화면 재배치와 대규모 CSS 변경은 이 단계에서 하지 않는다.

## 변경 예상 파일
- `frontend/src/types/api.ts`
- `frontend/src/api/smartClosetApi.ts`
- `frontend/src/api/errorHelpers.ts`
- `frontend/src/utils/**` 또는 `frontend/src/features/**/labels.ts`
- `frontend/src/components/**`

## 구현 메모
- `getCurrentWeather(accessToken)`를 추가하고 `GET /api/weather/current`를 호출한다.
- `updateClothing(accessToken, clothingId, body)`를 추가하고 `PUT /api/clothes/{clothingId}`를 호출한다.
- `archiveClothing(accessToken, clothingId)`를 추가하고 `PATCH /api/clothes/{clothingId}/archive`를 호출한다.
- API 성공 응답은 `{ data: T }`, 실패 응답은 `{ code, message, details }`로 유지한다.
- 공통 label mapping을 만든다:
  - `ClothingCategory`: `TOP` 상의, `BOTTOM` 하의, `OUTER` 아우터
  - `ClothingColor`: 한국어 라벨과 swatch 색상
  - `ClothingMaterial`: 한국어 라벨
  - `WeatherType`: 한국어 라벨
- 추천 실패 mapping을 만든다:
  - `NO_TOP_AVAILABLE` -> 상의 부족 메시지와 상의 등록 CTA
  - `NO_BOTTOM_AVAILABLE` -> 하의 부족 메시지와 하의 등록 CTA
  - `OUTER_REQUIRED_BUT_NOT_AVAILABLE` -> 아우터 필요 메시지와 아우터 등록 CTA
  - `NO_WEATHER_SUITABLE_ITEM` -> 옷장 확인 CTA
  - `INSUFFICIENT_CLOSET_ITEMS` -> 빠른 등록 CTA
- `styleTags`는 저장/조회/표시만 하는 타입과 label helper로 유지한다. 추천 점수에 연결되는 문구를 만들지 않는다.

## 검증 절차
```bash
git diff --check
rg -n 'getCurrentWeather|updateClothing|archiveClothing|NO_TOP_AVAILABLE|NO_BOTTOM_AVAILABLE|OUTER_REQUIRED_BUT_NOT_AVAILABLE' frontend/src
! rg -n 'userId|recommendations/today|/api/recommendations/today' frontend/src
(cd frontend && npm run build)
```

## 인수 기준
- frontend API client가 MVP4에서 사용하는 보호 API 함수를 모두 제공한다.
- 컴포넌트에서 새 API를 직접 `fetch`하지 않고 API client를 사용할 수 있다.
- enum 라벨, swatch, chip, 추천 실패 CTA mapping이 공통 위치에 정의되어 중복을 줄인다.
- `WeatherResponse` 타입과 `getCurrentWeather` 함수가 API 문서와 일치한다.
- `userId` query parameter와 today 추천 GET 경로가 frontend 코드에 없다.

## 금지사항
- 화면 전체 구조를 이 단계에서 재작성하지 마라. 이유: Step 2 이후에서 view별로 나눠 구현한다.
- API enum 값을 바꾸지 마라. 이유: request/response 계약은 대문자 enum을 유지한다.
- `styleTags`를 점수나 추천 이유 helper에 연결하지 마라. 이유: MVP4에서 저장/조회/표시만 한다.
- 큰 상태 관리 라이브러리를 추가하지 마라. 이유: 현재 기준은 React state와 작은 hook이다.
- 소셜 로그인, 비밀번호 찾기, 이메일 인증 타입을 추가하지 마라. 이유: MVP4 제외 범위다.

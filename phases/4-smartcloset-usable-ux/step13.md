# 단계 13: location-history-summary-polish-and-qa

범위: Should-have / MVP4 P1

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/FRONTEND.md`
- `docs/API.md`
- `docs/DEMO_SCENARIO.md`
- `docs/design/mvp4/README.md`
- `docs/design/mvp4/mobile/location-reference.png`
- `docs/design/mvp4/mobile/history.png`
- `frontend/src/features/location/LocationPanel.tsx`
- `frontend/src/features/history/HistoryPanel.tsx`
- `frontend/src/features/today/TodayPanel.tsx`
- `frontend/src/api/smartClosetApi.ts`
- `frontend/src/types/api.ts`
- `frontend/src/App.css`

이전 step 11-12에서 바뀐 공통 카드, token, responsive 스타일을 확인하고 이어서 작업하라.

## 작업
Location view를 선택된 위치 카드, 현재 날씨 요약 카드, 검색 결과 카드 구조로 정리하고, History view는 추천 요약 카드를 먼저 보여준 뒤 상세/점수를 펼침으로 낮춘다. 마지막으로 MVP4 visual polish 전체의 반응형 QA를 수행한다.

## 변경 예상 파일
- `frontend/src/features/location/LocationPanel.tsx`
- `frontend/src/features/history/HistoryPanel.tsx`
- `frontend/src/features/today/TodayPanel.tsx`
- `frontend/src/App.css`
- 필요 시 `docs/FRONTEND.md` 또는 `docs/DEMO_SCENARIO.md`

## 구현 메모
- Location은 현재 선택된 위치를 가장 먼저 카드로 보여준다.
- Location 안에 현재 날씨 요약 카드를 추가하려면 기존 `GET /api/weather/current` frontend API 함수를 사용한다.
- 날씨 요약 조회 실패가 위치 검색/선택을 막으면 안 된다.
- 검색 결과는 catalog 리스트 느낌을 줄이고 선택 가능한 card list로 표현한다.
- `GET /api/locations?keyword={keyword}`와 `PUT /api/users/me/location` 계약을 그대로 사용한다.
- 위치 선택 성공 후 기존 `onLocationChange` 흐름을 유지해 Today 날씨 요약 갱신이 이어지게 한다.
- History는 각 이력의 날짜/착용 여부/옷 조합/간단 weather badge를 먼저 보여주는 summary card를 기본으로 한다.
- History의 추천 이유, 날씨 snapshot, 점수 상세는 `details/summary` 또는 확장 영역으로 낮춘다.
- 착용 완료는 계속 `PATCH /api/recommendations/{recommendationId}/worn`을 사용한다.
- Today의 최근 이력 preview와 History view가 같은 `RecommendationResponse` 계약을 사용하게 유지한다.
- 최종 QA에서 desktop 1366px 이상과 mobile 375px에서 Today, Closet, Preferences, Location, History를 모두 확인한다.

## 검증 절차
```bash
git diff --check
rg -n 'getLocations|updateUserLocation|getCurrentWeather|getRecommendationHistory|markRecommendationWorn|limit=20|착용|이력|현재 위치|날씨' frontend/src
! rg -n 'navigator\.geolocation|mapbox|kakao|naver.*map|google.*map|latitude|longitude|recommendations/today|userId|limit=0|limit=100' frontend/src
(cd frontend && npm run build)
```

가능하면 로컬 프론트에서 다음을 확인한다:
- 데스크톱 1366px 이상에서 Today, Closet, Preferences, Location, History가 서로 겹치지 않는다.
- 모바일 375px에서 하단 탭, sticky CTA, 저장 CTA가 콘텐츠와 겹치지 않는다.
- Location은 지도처럼 보이는 UI나 브라우저 위치 권한 요청 없이 catalog 선택만 제공한다.
- History는 요약 카드가 먼저 보이고, 이유/날씨/점수는 펼침으로 확인할 수 있다.
- 착용 완료 버튼은 이미 착용 완료된 항목에서도 안정적으로 비활성/완료 상태를 유지한다.

## 인수 기준
- Location view가 선택된 위치 카드, 현재 날씨 요약 카드, 검색 결과 카드 구조를 가진다.
- Location은 외부 지도/주소 API나 브라우저 현재 위치 요청 없이 동작한다.
- 위치 선택 후 app 상태와 Today 날씨 요약 갱신 흐름이 유지된다.
- History view가 추천 요약 카드 우선 구조이며 상세 이유/날씨/점수는 펼침 영역으로 낮아져 있다.
- History 착용 완료 처리와 최신순 이력 조회가 기존 API 계약으로 동작한다.
- 데스크톱 1366px와 모바일 375px에서 주요 화면 텍스트, 카드, 버튼, 하단 탭이 겹치지 않는다.
- `cd frontend && npm run build`가 통과한다.

## 금지사항
- 외부 지도 SDK 또는 주소 검색 API를 추가하지 마라. 이유: MVP4 위치 선택은 서버 내장 catalog만 사용한다.
- 브라우저 현재 위치 권한 요청을 하지 마라. 이유: browser geolocation은 MVP4 제외 범위다.
- latitude/longitude 입력 UX나 KMA 격자 변환 API를 만들지 마라. 이유: 위경도-KMA 변환은 범위 밖이다.
- 추천 이력 API limit 범위를 frontend 임의 계약으로 바꾸지 마라. 이유: backend 기준은 기본 20, 1..50, 최신순이다.
- weather current API를 이력 source로 사용하지 마라. 이유: 이력 날씨는 추천 응답의 weather snapshot이다.
- 신규 backend 기능으로 시각 polish를 해결하지 마라. 이유: 이 step은 기존 API 위 frontend polish다.

# 단계 9: location-catalog-ux

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
- `frontend/src/features/location/LocationPanel.tsx`
- `frontend/src/api/smartClosetApi.ts`
- `frontend/src/types/api.ts`
- `frontend/src/App.css`

이전 P0 release candidate와 Today weather refresh 흐름을 확인한 뒤 작업하라.

## 작업
Location view를 현재 위치 표시, keyword 검색, 내장 대표 격자 catalog 선택, 위치 선택 CTA 중심으로 정리한다.

## 변경 예상 파일
- `frontend/src/features/location/**`
- `frontend/src/App.tsx`
- `frontend/src/App.css`

## 구현 메모
- 현재 위치는 `GET /api/users/me/location` 응답으로 표시한다.
- catalog는 `GET /api/locations?keyword={keyword}`를 사용한다.
- 위치 선택은 `PUT /api/users/me/location`으로 처리한다.
- `GET /api/locations`의 `401`은 위치 검색 실패가 아니라 인증 만료로 처리한다.
- 위치 선택 성공 후 Today view의 위치와 현재 날씨 요약이 갱신될 수 있게 상위 상태를 연결한다.
- 검색은 code 또는 name 기준 backend 검색 정책을 그대로 사용한다.
- 외부 지도 UI처럼 보이는 interactive map을 만들지 않는다.
- browser geolocation permission prompt를 요청하지 않는다.

## 검증 절차
```bash
git diff --check
rg -n 'getLocations|updateUserLocation|locationCode|현재 위치|검색|서울특별시|부산광역시' frontend/src/features/location frontend/src
! rg -n 'navigator\\.geolocation|mapbox|kakao|naver.*map|google.*map|latitude|longitude' frontend/src
(cd frontend && npm run build)
```

## 인수 기준
- Location view에서 현재 위치, 검색, catalog 목록, 선택 CTA가 보인다.
- 로그인 후에만 catalog를 호출한다.
- 위치 선택 후 app 상태와 Today 날씨 요약이 갱신된다.
- 인증 만료와 검색 실패가 구분되어 처리된다.
- 외부 지도/주소 API나 브라우저 현재 위치 요청이 없다.

## 금지사항
- 외부 지도 SDK 또는 주소 검색 API를 추가하지 마라. 이유: MVP4 위치 선택은 서버 내장 catalog만 사용한다.
- 브라우저 현재 위치 권한 요청을 하지 마라. 이유: browser geolocation은 MVP4 제외 범위다.
- 회원가입 화면에서 위치 catalog를 호출하지 마라. 이유: 신규 사용자는 서버 기본 위치 서울로 시작한다.
- latitude/longitude 입력 UX를 만들지 마라. 이유: 위경도-KMA 격자 변환 API는 범위 밖이다.

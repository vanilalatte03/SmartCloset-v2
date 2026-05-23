# PRD: SmartCloset MVP4 실사용 UX

## 문서 목적
이 문서는 SmartCloset MVP4의 확정 범위를 정의한다. MVP4는 백엔드 추천 규칙을 새로 만드는 단계가 아니라, MVP-3 인증 사용자 baseline을 사용자가 실제로 이해하고 사용할 수 있는 반응형 웹 제품으로 바꾸는 단계다.

현재 구현 baseline은 MVP-3 완료 상태다. 회원가입/로그인, JWT Bearer access token, 인증 사용자 기준 옷장/위치/선호도/추천 이력/착용 이력 분리, `preferenceScore`, 추천 이력 조회, React `sessionStorage` 세션 흐름은 이미 구현되어 있다.

## MVP4 한 줄 정의
회원가입 또는 로그인 후 사용자가 2분 안에 자신의 옷을 최소 등록하고 첫 추천을 성공시킬 수 있는 실사용 UX를 만든다.

## 목표
- 신규 사용자가 현재 필요한 다음 행동을 즉시 알 수 있어야 한다.
- API enum, 실패 코드, 점수표 중심 화면을 사용자 언어로 바꾼다.
- 데스크톱과 모바일 모두에서 같은 React 웹앱을 자연스럽게 사용할 수 있어야 한다.
- 앱 출시나 PWA 설치 경험은 이번 범위가 아니다.

## 현재 baseline
- 공개 API는 `POST /api/auth/signup`, `POST /api/auth/login`뿐이다.
- 그 외 API는 `Authorization: Bearer {accessToken}` header를 요구한다.
- 공개 HTTP API는 `userId` query parameter를 받지 않는다.
- 현재 사용자 전용 응답 DTO는 `userId`를 노출하지 않는다.
- 추천 생성 API는 `POST /api/recommendations`다.
- 추천 이력 조회 API는 `GET /api/recommendations?limit={limit}`이며 기본 20, 최소 1, 최대 50, 최신순이다.
- 현재 날씨 요약 API는 `GET /api/weather/current`이며 보호 API다.
- 프론트 access token 저장 위치는 `sessionStorage`다.
- 선호도는 `users` 테이블의 `preferred_colors_json`, `preferred_materials_json`, `style_tags_json` JSON 문자열 컬럼에 저장한다.
- `preferredColors`와 `preferredMaterials`만 `preferenceScore`에 반영한다.
- `styleTags`는 저장/조회/표시만 하며 추천 점수와 추천 이유에는 반영하지 않는다.
- 외부 Weather API는 기상청 단기예보 `getVilageFcst` JSON 연동만 사용한다.
- 위치 선택은 외부 지도/주소 API 없이 서버 내장 대표 격자 catalog를 사용한다.
- Docker Compose 공유 방식을 유지한다.

## 해결하려는 문제
- 로그인 후 위치, 선호도, 옷장, 추천 패널이 나뉘어 있어 첫 추천까지의 다음 행동이 분명하지 않다.
- `TOP`, `BOTTOM`, `OUTER`, `NO_TOP_AVAILABLE` 같은 내부 코드가 사용자에게 그대로 보인다.
- 추천 결과가 점수표 중심이라 왜 오늘 입기 좋은지 빠르게 이해하기 어렵다.
- 옷 등록 폼이 빠른 입력을 돕지 못하고, 등록 이후 수정/보관 흐름이 약하다.
- 모바일에서는 패널을 세로로 쌓는 수준이라 실제 사용 동선이 앱처럼 느껴지지 않는다.

## 핵심 사용자 시나리오
1. 신규 사용자가 회원가입 후 오늘 추천 화면에서 첫 추천 준비 체크리스트를 본다.
2. 사용자가 위치 확인, 선호도 저장, 상의/하의/아우터 최소 등록을 완료한다.
3. 사용자가 추천 생성을 누르고, 실패하면 부족한 항목 CTA를 통해 바로 옷장으로 이동한다.
4. 추천 성공 시 사용자는 점수보다 "오늘 입기 좋은 이유"와 추천 옷 조합을 먼저 본다.
5. 사용자는 추천 결과를 착용 완료 처리하고, 이력에서 최근 추천과 착용 상태를 확인한다.

## MVP4 우선순위

### P0: 첫 추천 성공 UX
- 로그인 후 첫 화면을 `오늘 추천` 중심으로 재구성한다.
- 첫 추천 준비 체크리스트를 제공한다: 위치 확인, 선호도 저장, TOP/BOTTOM/OUTER 최소 등록.
- 추천 생성 CTA를 오늘 추천 화면의 가장 중요한 행동으로 둔다.
- 추천 실패 코드는 한국어 설명과 직접 CTA로 변환한다.
- 추천 결과는 점수표보다 "오늘 입기 좋은 이유"와 옷 조합을 먼저 보여준다.

### P0: 옷장 실사용 UX
- 옷 목록에서 한국어 category/color/material 라벨을 사용한다.
- 색상은 swatch, 소재는 chip으로 표시한다.
- 옷 등록 폼에 계절/기온 프리셋과 빠른 등록 흐름을 제공한다.
- 옷 수정과 보관 처리를 화면에서 수행할 수 있게 한다.
- 모바일에서 hover에 의존하지 않는 카드 액션을 제공한다.

### P0: 반응형 앱 셸
- 데스크톱은 좌측 사이드바와 상단 상태바를 사용한다.
- 모바일은 상단 앱바, 단일 컬럼 콘텐츠, 하단 탭을 사용한다.
- 모바일 하단 탭은 `오늘`, `옷장`, `선호도`, `위치`, `이력` 5개로 고정한다.
- 큰 상태 관리 라이브러리 없이 React state와 작은 hook으로 구현한다.

### P1: 화면별 사용성 보강
- 로그인/회원가입 화면을 한국어로 정리한다.
- 선호도 화면은 색상 swatch, 소재 chip, style tag 입력/삭제로 구성한다.
- 위치 화면은 지도 UI 없이 현재 위치, 검색, 내장 catalog 선택 중심으로 구성한다.
- 이력 화면은 최근 추천 카드, 착용 여부, 다시 착용 처리로 구성한다.
- 로딩, 빈 상태, 저장 성공, 인증 만료 상태를 사용자 문장으로 표시한다.

### P2: 후속 후보
아래 항목은 MVP4 확정 범위가 아니다.

- Refresh token
- 소셜 로그인
- 이메일 인증
- 비밀번호 재설정
- 선호도 별도 테이블 정규화
- styleTags 기반 개인화 고도화
- 외부 주소/지도 검색 API
- 사용자 현재 위치 자동 감지
- 옷 이미지 업로드
- AI/GPT 추천
- Redis 캐싱
- AWS 배포와 CD 자동화

## 포함 범위
- React 반응형 웹 UI 재구성
- `오늘 추천`, `옷장`, `선호도`, `위치`, `이력`, `인증` 화면 정의
- 기존 API client 타입과 함수 보강
- 옷 수정 API와 보관 API의 프론트 사용
- 사용자 친화적인 enum 라벨, 색상 swatch, 소재 chip
- 추천 실패 코드별 CTA
- 모바일 하단 탭과 sticky 주요 CTA
- 데모/공유 문서의 첫 추천 성공 시나리오 갱신

## 제외 범위
아래 항목은 별도 승인 전까지 MVP4에서 제외한다.

- refresh token
- social login
- email verification
- password reset
- external address/map APIs
- browser/current-location auto detection
- latitude/longitude to KMA grid conversion APIs
- KMA `getVilageFcst` 외 weather APIs
- weather source DB persistence
- Redis
- AWS deployment
- CD automation
- AI/GPT recommendations
- image upload
- shopping recommendations
- preference normalization tables
- styleTags scoring
- styleTags recommendation reasons
- native mobile app release
- PWA install/push notification

## API 변경 계획
MVP4는 새 공개 API, DB schema, 추천 규칙을 추가하지 않는다. 단, Today 화면의 현재 날씨 요약을 위해 보호 API `GET /api/weather/current`를 추가한다.

원칙:
- 새 공개 API를 추가하지 않는다.
- 보호 API는 계속 Bearer token을 요구한다.
- `userId` query parameter를 공개 HTTP 계약에 되살리지 않는다.
- 현재 사용자 전용 response DTO에 `userId`를 되살리지 않는다.
- today 추천 GET 경로를 추가하지 않는다.
- 추천 생성은 `POST /api/recommendations`만 사용한다.
- 현재 날씨 요약은 추천 생성/저장 없이 현재 인증 사용자 위치의 `WeatherResponse`만 반환한다.

프론트에서 MVP4에 반드시 사용하는 보호 API:
- `GET /api/users/me/location`
- `PUT /api/users/me/location`
- `GET /api/weather/current`
- `GET /api/users/me/preferences`
- `PUT /api/users/me/preferences`
- `GET /api/clothes`
- `POST /api/clothes`
- `PUT /api/clothes/{clothingId}`
- `PATCH /api/clothes/{clothingId}/archive`
- `POST /api/recommendations`
- `GET /api/recommendations?limit={limit}`
- `PATCH /api/recommendations/{recommendationId}/worn`

## 데이터/ERD 변경 계획
DB schema 변경은 없다.

- 새 테이블을 추가하지 않는다.
- 새 컬럼을 추가하지 않는다.
- image URL 또는 file metadata 컬럼을 추가하지 않는다.
- 선호도는 계속 `users` 테이블 JSON 문자열 컬럼에 저장한다.
- 운영 DB migration 정책은 이번 MVP4 범위에서 다루지 않는다.
- 로컬 공유/데모 기준은 Docker Compose volume 초기화 방식을 유지한다.

## 프론트엔드 변경 계획
화면 구조:

- `Today`: 오늘 추천, 현재 날씨 요약, 첫 추천 준비 체크리스트, 추천 생성, 추천 결과, 실패 CTA
- `Closet`: 카테고리 필터, 옷 목록, 빠른 등록, 수정, 보관
- `Preferences`: 색상 swatch, 소재 chip, style tag 저장/삭제
- `Location`: 현재 위치, 위치 검색, 내장 catalog 선택
- `History`: 추천 이력, 착용 여부, 착용 완료 처리
- `Auth`: 로그인, 회원가입, API 연결/인증 오류

반응형 구조:

- 데스크톱: sidebar navigation, top status bar, 2 column 이상의 작업 영역
- 모바일: top app bar, single column content, bottom tab navigation, sticky primary CTA

표시 규칙:

- API enum 값은 request/response에서 유지한다.
- 사용자 화면에서는 한국어 라벨을 사용한다.
- 색상은 라벨과 swatch를 함께 표시한다.
- 소재와 style tag는 chip으로 표시한다.
- 추천 점수는 보조 정보로 낮추고, 추천 이유와 옷 조합을 우선한다.

## 추천 규칙 변경 계획
추천 도메인 규칙, 점수, tie-break, 실패 코드는 변경하지 않는다.

UI 표시만 변경한다.

- `reasons`를 "오늘 입기 좋은 이유"로 표시한다.
- `score`는 상세/접힘 영역 또는 보조 영역에 표시한다.
- 추천 실패 코드별로 사용자가 바로 해결할 수 있는 CTA를 제공한다.
- 현재 날씨 요약은 추천 결과가 아니며 추천 이력에 저장하지 않는다.

추천 실패 CTA 기준:

| Code | 사용자 메시지 | CTA |
| --- | --- | --- |
| `NO_TOP_AVAILABLE` | 현재 날씨에 맞는 상의가 부족해요. | 상의 등록하기 |
| `NO_BOTTOM_AVAILABLE` | 현재 날씨에 맞는 하의가 부족해요. | 하의 등록하기 |
| `OUTER_REQUIRED_BUT_NOT_AVAILABLE` | 오늘은 아우터가 필요한 날씨예요. | 아우터 등록하기 |
| `NO_WEATHER_SUITABLE_ITEM` | 현재 기온에 맞는 옷이 부족해요. | 옷장 확인하기 |
| `INSUFFICIENT_CLOSET_ITEMS` | 추천을 만들려면 옷을 더 등록해야 해요. | 빠른 등록하기 |

## 완료 기준
- 신규 사용자가 React 웹앱에서 회원가입 또는 로그인 후 2분 안에 첫 추천을 성공시킬 수 있다.
- 첫 추천 준비 체크리스트가 부족한 항목과 다음 CTA를 정확히 보여준다.
- 추천 실패 시 내부 코드만 노출하지 않는다.
- 로그인 후 Today 화면에서 현재 위치 기준 날씨 요약이 보인다.
- 옷 등록, 수정, 보관을 프론트에서 수행할 수 있다.
- enum이 사용자 화면에서 한국어 라벨, swatch, chip으로 표현된다.
- 모바일 375px 너비에서 주요 화면과 버튼 텍스트가 겹치지 않는다.
- 데스크톱과 모바일 모두 같은 API 계약을 사용한다.

## 테스트/검증 기준
- `cd frontend && npm run build`
- 데스크톱 1280px 이상에서 `오늘`, `옷장`, `선호도`, `위치`, `이력`을 탐색한다.
- 모바일 375px에서 하단 탭과 sticky CTA가 정상 배치되는지 확인한다.
- 신규 사용자로 위치 확인, 선호도 저장, TOP/BOTTOM/OUTER 등록, 추천 생성을 완료한다.
- `GET /api/weather/current`가 인증 사용자 위치 기준 날씨를 반환하고 추천 이력을 만들지 않는지 확인한다.
- TOP, BOTTOM, OUTER를 각각 부족하게 만든 뒤 실패 CTA가 올바르게 표시되는지 확인한다.
- 옷 수정과 보관 후 목록과 추천 후보 상태가 갱신되는지 확인한다.
- `styleTags` 변경이 추천 점수와 추천 이유를 바꾸지 않는지 확인한다.

## 결정 완료 사항
- MVP4 P0 범위: 실사용 UX와 반응형 웹
- API 계약 변경: 현재 날씨 요약 보호 API 1개 추가
- DB migration: 없음
- 추천 규칙 변경: 없음
- Docker Compose 공유 기준 변경: 없음
- native app/PWA 출시: 범위 밖

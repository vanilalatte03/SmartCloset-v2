# Frontend: SmartCloset MVP4 Responsive UX

## 목표
MVP4 프론트엔드는 인증 사용자 baseline 위에서 "회원가입 또는 로그인 후 2분 안에 첫 추천 성공"을 돕는 React+Vite+TypeScript 반응형 웹앱이다.

프론트는 native app이 아니며, 이번 범위에서 PWA 설치, push notification, 앱스토어 배포를 목표로 하지 않는다.

## 기술 기준
- React
- Vite
- TypeScript
- TypeScript `strict`
- CSS는 기존 앱 CSS 체계를 우선 사용한다.
- 큰 상태 관리 라이브러리는 추가하지 않는다.
- React Router는 필수가 아니다. 탭/view state 기반 전환으로 시작할 수 있다.
- API 요청/응답 DTO는 `src/types/api.ts`에 명시한다.
- API 함수는 `src/api/smartClosetApi.ts`에서만 정의한다.

## 인증 상태 기준
프론트 access token 저장 위치는 `sessionStorage`로 고정한다.

권장 key:

```text
smartcloset.accessToken
```

흐름:

1. 앱 시작 시 `sessionStorage`에서 access token을 읽는다.
2. token이 있으면 `GET /api/users/me`를 호출해 현재 사용자 정보를 복구한다.
3. token이 없거나 `GET /api/users/me`가 `401`이면 인증 화면을 보여준다.
4. 로그인 성공 시 access token을 `sessionStorage`에 저장하고 사용자 정보를 상태에 보관한다.
5. 모든 보호 API 요청에는 `Authorization: Bearer {accessToken}`을 붙인다.
6. 로그아웃 시 `sessionStorage` token과 사용자 상태를 제거한다.

Refresh token은 MVP4 범위가 아니다.

## 앱 셸
로그인 후 앱은 아래 view를 가진다.

```ts
type AppView = 'today' | 'closet' | 'preferences' | 'location' | 'history';
```

기본 view는 `today`다.

### Desktop
- 좌측 sidebar: `오늘`, `옷장`, `선호도`, `위치`, `이력`
- 상단 status bar: 사용자 이름/email, 현재 위치, API 상태, 로그아웃
- 콘텐츠 영역: 오늘 추천은 2-column 이상, 나머지 화면은 콘텐츠 성격에 맞게 grid 사용

### Mobile
- 상단 app bar: SmartCloset, 현재 위치/날씨 요약, 로그아웃 또는 사용자 메뉴
- 본문: 단일 컬럼
- 하단 tab bar: `오늘`, `옷장`, `선호도`, `위치`, `이력`
- 주요 CTA: 화면 성격에 따라 sticky 또는 floating 영역 사용

모바일 하단 탭은 hover에 의존하지 않고 터치 target을 충분히 크게 둔다.

## 화면 구성

### Auth View
- 로그인과 회원가입을 제공한다.
- 소셜 로그인, 비밀번호 찾기, 이메일 인증은 표시하지 않는다.
- 회원가입 화면에서는 `GET /api/locations`를 호출하지 않는다.
- 신규 사용자는 서버 기본값인 서울 위치와 빈 선호도 배열로 시작한다.
- 모든 문구는 한국어를 기본으로 한다.

### Today View
MVP4의 기본 화면이다.

필수 구성:
- 오늘 날씨 요약: 위치 이름, 기온, 날씨 타입, 비/바람 상태
- 첫 추천 준비 체크리스트
- 추천 생성 CTA
- 추천 결과 카드
- "오늘 입기 좋은 이유" 영역
- 추천 실패 CTA
- 최근 이력 preview

오늘 날씨 요약:
- 위치 이름은 `GET /api/users/me/location` 응답을 사용한다.
- 기온, 날씨 타입, 비/바람 상태는 `GET /api/weather/current` 응답을 사용한다.
- 날씨 요약 조회에 실패해도 추천 준비 체크리스트, 옷장 이동, 추천 CTA 접근을 막지 않는다.
- 위치를 변경하면 `GET /api/weather/current`를 다시 호출한다.
- 추천 성공 후 결과 카드에는 추천 응답의 `weather` snapshot을 표시한다.

첫 추천 준비 체크리스트:

| 항목 | 데이터 기준 | 완료 기준 |
| --- | --- | --- |
| 위치 확인 | `GET /api/users/me/location` | 위치 응답 존재 |
| 선호도 저장 | `GET /api/users/me/preferences` | 사용자가 저장을 눌렀거나, 기본값 확인 상태 |
| 상의 등록 | `GET /api/clothes` | `category=TOP` 활성 옷 1개 이상 |
| 하의 등록 | `GET /api/clothes` | `category=BOTTOM` 활성 옷 1개 이상 |
| 아우터 등록 | `GET /api/clothes` | `category=OUTER` 활성 옷 1개 이상 |

fallback 날씨 `temperature=12`에서는 OUTER가 추천 성공에 필요하므로 데모 UX에서 아우터 등록을 명확히 안내한다.

추천 결과 표시 순서:
1. 추천 옷 조합: 상의, 하의, 아우터
2. 오늘 입기 좋은 이유: `reasons`
3. 착용 완료 CTA
4. 점수 상세: 접힘 또는 보조 영역

### Closet View
필수 구성:
- 카테고리 filter: 전체, 상의, 하의, 아우터
- 활성 옷 목록
- 빠른 등록 form
- 옷 수정
- 옷 보관 처리

옷 목록:
- name을 가장 크게 표시한다.
- category/color/material은 한국어 라벨로 표시한다.
- color는 swatch와 함께 표시한다.
- material은 chip으로 표시한다.
- min/max temperature와 rainSuitable은 보조 정보로 표시한다.
- 수정/보관 액션은 모바일에서도 항상 접근 가능해야 한다.

옷 등록:
- API request shape는 `ClothingRequest`를 그대로 사용한다.
- category/color/material은 select, segmented control, swatch/chip 중 화면에 맞는 UI로 표현한다.
- 계절/기온 프리셋은 UI helper이며 서버 enum이나 DB schema를 바꾸지 않는다.
- 프리셋 선택 시 `minTemperature`, `maxTemperature`, `rainSuitable` 기본값만 채운다.

권장 프리셋:

| Label | minTemperature | maxTemperature | rainSuitable |
| --- | ---: | ---: | --- |
| 한겨울 | -10 | 5 | false |
| 쌀쌀한 날 | 0 | 12 | false |
| 간절기 | 8 | 20 | false |
| 따뜻한 날 | 17 | 28 | false |
| 비 오는 날 | 5 | 24 | true |

### Preferences View
필수 구성:
- 선호 색상 swatch multi-select
- 선호 소재 chip multi-select
- style tag 입력/삭제
- 저장 CTA

주의:
- `preferredColors`, `preferredMaterials`만 `preferenceScore`에 반영된다.
- `styleTags`는 저장/조회/표시만 한다.
- style tag가 추천 점수나 추천 이유에 반영되는 것처럼 쓰지 않는다.

### Location View
필수 구성:
- 현재 위치 표시
- keyword 검색
- 내장 대표 격자 catalog 목록
- 위치 선택 CTA

금지:
- 외부 지도 UI처럼 보이는 인터랙티브 map을 구현하지 않는다.
- 브라우저 현재 위치 권한 요청을 하지 않는다.
- 외부 주소/지도 API를 호출하지 않는다.

### History View
필수 구성:
- `GET /api/recommendations?limit=20` 기본 조회
- 최신순 추천 이력 목록
- 착용 여부 표시
- 이력 항목의 착용 완료 처리
- 상세 보기 또는 확장 카드

Limit 정책:
- 기본값 `20`
- 최소 `1`
- 최대 `50`
- 범위 밖 또는 숫자가 아닌 값은 `400 INVALID_REQUEST`

## API Client 기준
- 컴포넌트에서 `fetch`를 직접 호출하지 않는다.
- 성공 응답은 `{ data: T }` 형태로 파싱한다.
- 실패 응답은 `{ code, message, details }` 형태로 파싱해 화면 상태로 전달한다.
- 보호 API는 access token을 받아 `Authorization: Bearer ...` header를 붙인다.
- `401`은 인증 만료로 취급하고 인증 화면으로 전환한다.

MVP4에서 프론트가 사용하는 옷 API 함수:
- `getClothes(accessToken)`
- `createClothing(accessToken, body)`
- `updateClothing(accessToken, clothingId, body)`
- `archiveClothing(accessToken, clothingId)`

`updateClothing`은 `PUT /api/clothes/{clothingId}`, `archiveClothing`은 `PATCH /api/clothes/{clothingId}/archive`를 호출한다.

MVP4에서 프론트가 사용하는 날씨 API 함수:
- `getCurrentWeather(accessToken)`

`getCurrentWeather`는 `GET /api/weather/current`를 호출한다. 이 API는 추천을 생성하거나 추천 이력을 저장하지 않는다.

## UI 라벨 기준
API enum 값은 변경하지 않는다. 화면에서만 한국어 라벨을 사용한다.

### ClothingCategory
| API | Label |
| --- | --- |
| `TOP` | 상의 |
| `BOTTOM` | 하의 |
| `OUTER` | 아우터 |

### ClothingColor
| API | Label |
| --- | --- |
| `BLACK` | 블랙 |
| `WHITE` | 화이트 |
| `GRAY` | 그레이 |
| `NAVY` | 네이비 |
| `BLUE` | 블루 |
| `BROWN` | 브라운 |
| `BEIGE` | 베이지 |
| `RED` | 레드 |
| `GREEN` | 그린 |
| `YELLOW` | 옐로우 |
| `UNKNOWN` | 기타 |

### ClothingMaterial
| API | Label |
| --- | --- |
| `COTTON` | 면 |
| `DENIM` | 데님 |
| `KNIT` | 니트 |
| `WOOL` | 울 |
| `POLYESTER` | 폴리에스터 |
| `NYLON` | 나일론 |
| `UNKNOWN` | 기타 |

## 추천 실패 UX
추천 실패는 HTTP `422` business failure다. UI는 내부 코드만 보여주지 않는다.

| Code | 사용자 메시지 | CTA |
| --- | --- | --- |
| `NO_TOP_AVAILABLE` | 현재 날씨에 맞는 상의가 부족해요. | 상의 등록하기 |
| `NO_BOTTOM_AVAILABLE` | 현재 날씨에 맞는 하의가 부족해요. | 하의 등록하기 |
| `OUTER_REQUIRED_BUT_NOT_AVAILABLE` | 오늘은 아우터가 필요한 날씨예요. | 아우터 등록하기 |
| `NO_WEATHER_SUITABLE_ITEM` | 현재 기온에 맞는 옷이 부족해요. | 옷장 확인하기 |
| `INSUFFICIENT_CLOSET_ITEMS` | 추천을 만들려면 옷을 더 등록해야 해요. | 빠른 등록하기 |

CTA는 `closet` view로 이동하고 가능하면 category 기본값을 맞춘다.

## UX 기준
- 운영 도구처럼 조용하고 밀도 있는 화면을 유지한다.
- 과도한 랜딩 페이지나 마케팅 hero를 만들지 않는다.
- 첫 화면은 제품 설명이 아니라 오늘 추천 작업이어야 한다.
- 텍스트는 모바일과 데스크톱에서 부모 요소를 넘치지 않아야 한다.
- 카드 안에 또 다른 카드가 중첩되는 구조를 피한다.
- 주요 버튼, input, select, checkbox는 native 접근성을 해치지 않는다.
- 이미지 업로드 UI는 MVP4 범위가 아니다.
- "AI", "98% 매치", "스타일 예보"처럼 실제 기능보다 큰 기대를 주는 표현은 사용하지 않는다.

## Backend 계약
공개 API:

- `POST /api/auth/signup`
- `POST /api/auth/login`

보호 API:

- `GET /api/users/me`
- `GET /api/locations?keyword={keyword}`
- `GET /api/users/me/location`
- `PUT /api/users/me/location`
- `GET /api/users/me/preferences`
- `PUT /api/users/me/preferences`
- `GET /api/weather/current`
- `GET /api/clothes`
- `POST /api/clothes`
- `GET /api/clothes/{clothingId}`
- `PUT /api/clothes/{clothingId}`
- `PATCH /api/clothes/{clothingId}/archive`
- `POST /api/recommendations`
- `GET /api/recommendations?limit={limit}`
- `PATCH /api/recommendations/{recommendationId}/worn`

프론트는 `userId` query parameter를 붙이지 않는다. 프론트 타입에도 현재 사용자 전용 response의 `userId` 필드를 두지 않는다.

프론트는 today 추천 GET 경로를 호출하지 않는다.

## 검증 기준
프론트 변경 후 아래 명령이 통과해야 한다.

```bash
cd frontend
npm install
npm run build
```

수동 확인:

1. 회원가입 또는 로그인 후 `today` view가 기본으로 열린다.
2. access token이 `sessionStorage`에 저장되고 새로고침 후 복구된다.
3. 로그인 전에는 위치 catalog를 호출하지 않는다.
4. 오늘 추천 화면에서 첫 추천 준비 체크리스트가 보인다.
5. 현재 위치 기준 날씨 요약이 보이고, 실패해도 주요 흐름이 막히지 않는다.
6. TOP/BOTTOM/OUTER 최소 등록 상태가 옷 목록 기준으로 계산된다.
7. 옷 등록, 수정, 보관 처리가 가능하다.
8. 추천 실패 시 한국어 메시지와 CTA가 보인다.
9. 추천 성공 시 이유와 옷 조합이 점수표보다 먼저 보인다.
10. 추천 이력과 착용 완료 상태가 최신 상태로 갱신된다.
11. 모바일 375px에서 하단 탭, sticky CTA, 카드 텍스트가 겹치지 않는다.

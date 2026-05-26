# Frontend: SmartCloset MVP7 Location Weather Trust

## 목표

MVP7 프론트엔드는 MVP6 반응형 웹앱 위에 동네 단위 위치 검색, 브라우저 현재 위치 후보 찾기, 오전/오후/저녁 예보 선택, 추천 결과와 이력의 위치/날씨 source 표시를 추가한다.

사용자는 Today view에서 "이 추천은 어느 동네, 어떤 예보 시각, KMA인지 fallback인지"를 바로 확인할 수 있어야 한다.

## 기술 기준

- React
- Vite
- TypeScript strict
- 기존 CSS 체계 우선
- 큰 상태 관리 라이브러리 추가 금지
- API 요청/응답 DTO는 `src/types/api.ts`에 명시
- API 함수는 `src/api/smartClosetApi.ts`에서 정의
- access token 저장 위치는 `sessionStorage`

## 인증 상태 기준

위치 검색, 좌표 resolve, 현재 날씨, 추천 생성, 이미지 API는 모두 보호 API다. 모든 보호 API 요청에는 `Authorization: Bearer {accessToken}` header가 필요하다.

`401`은 기존 보호 API와 동일하게 인증 만료로 처리한다.

## API Client 기준

MVP7에서 추가/변경할 함수:

- `searchLocations(accessToken, keyword?)`
- `resolveLocation(accessToken, request)`
- `getUserLocation(accessToken)`
- `updateUserLocation(accessToken, request)`
- `getCurrentWeather(accessToken)`
- `createRecommendation(accessToken, request?)`
- 기존 `replaceRecommendationFeedback(accessToken, recommendationId, request)` 유지
- 기존 `markRecommendationWorn(accessToken, recommendationId)` 유지

추천 생성:

- body 없이 호출 가능해야 한다.
- 사용자가 상황만 선택하면 `{ situation }` body를 보낸다.
- 사용자가 시간대만 선택하면 `{ forecastPeriod }` body를 보낸다.
- situation이 없으면 서버 기본값 `CASUAL`을 신뢰한다.
- forecastPeriod가 없으면 서버 기본값 `CURRENT`를 신뢰한다.

위치 resolve:

- 브라우저 Geolocation API 성공 후 `{ latitude, longitude }`를 전송한다.
- resolve 결과는 자동 저장하지 않는다.
- 사용자가 후보를 선택하면 `PUT /api/users/me/location`을 호출한다.

이미지 blob fetch:

- MVP5와 동일하게 Authorization header를 붙인다.
- object URL은 cleanup한다.

## 타입 기준

```ts
export type LocationSource = 'MANUAL_SEARCH' | 'BROWSER_GEOLOCATION';

export type ForecastPeriod = 'CURRENT' | 'MORNING' | 'AFTERNOON' | 'EVENING';

export type LocationOptionResponse = {
  code: string;
  name: string;
  fullName: string;
  region1: string;
  region2: string | null;
  region3: string | null;
  nx: number;
  ny: number;
  latitude: number | null;
  longitude: number | null;
};

export type LocationResolveRequest = {
  latitude: number;
  longitude: number;
};

export type LocationGridResponse = {
  nx: number;
  ny: number;
};

export type LocationResolveResponse = {
  grid: LocationGridResponse;
  nearest: LocationOptionResponse | null;
  candidates: LocationOptionResponse[];
};

export type UserLocationResponse = {
  code: string;
  name: string;
  fullName: string;
  region1: string;
  region2: string | null;
  region3: string | null;
  nx: number;
  ny: number;
  source: LocationSource;
  updatedAt: string;
};

export type UpdateUserLocationRequest = {
  locationCode: string;
  source?: LocationSource;
};

export type WeatherLocationSnapshotResponse = {
  code: string;
  name: string;
  fullName: string;
  nx: number;
  ny: number;
  source: LocationSource;
};

export type WeatherProvider = 'KMA_VILAGE_FORECAST' | 'STATIC_FALLBACK';

export type WeatherSourceResponse = {
  provider: WeatherProvider;
  kmaUsed: boolean;
  fallbackUsed: boolean;
  baseDate: string | null;
  baseTime: string | null;
  forecastDate: string | null;
  forecastTime: string | null;
};

export type WeatherResponse = {
  temperature: number;
  weatherType: WeatherType;
  rainy: boolean;
  windy: boolean;
  location: WeatherLocationSnapshotResponse;
  source: WeatherSourceResponse;
};

export type RecommendationRequest = {
  situation?: RecommendationSituation;
  forecastPeriod?: ForecastPeriod;
};

export type RecommendationResponse = {
  recommendationId: number;
  situation: RecommendationSituation;
  forecastPeriod: ForecastPeriod;
  weather: WeatherResponse;
  outfit: RecommendationOutfitResponse;
  score: RecommendationScoreResponse;
  reasons: string[];
  worn: boolean;
  wornAt: string | null;
  feedback: RecommendationFeedbackStateResponse | null;
  createdAt: string;
};
```

MVP6 `ClothingRequest`, `OutfitItemResponse`, feedback types는 유지한다.

## Location View

Location view에 검색과 현재 위치로 찾기 control을 둔다.

검색 UX:

- 검색 입력 placeholder는 행정구역 예시를 보여준다.
- 검색 결과는 `fullName`, `nx/ny`를 함께 표시해 동명이인을 구분한다.
- 후보 선택 시 저장 버튼 또는 후보 row action으로 `PUT /api/users/me/location`을 호출한다.
- 수동 검색으로 저장하면 `source=MANUAL_SEARCH`다.

현재 위치 UX:

- 브라우저 위치 권한 요청은 "현재 위치로 찾기" 버튼 클릭 뒤에만 수행한다.
- 권한 대기 중에는 버튼과 후보 UI가 안정적인 높이를 유지한다.
- 권한 거부 또는 브라우저 미지원이면 수동 검색 안내를 보여준다.
- 권한 허용 후 `POST /api/locations/resolve` 후보를 표시한다.
- resolve 후보 선택 저장 시 `source=BROWSER_GEOLOCATION`이다.
- resolve 결과를 자동 저장하지 않는다.

## Forecast Period UX

Today view에 예보 시간대 선택 control을 둔다.

| ForecastPeriod | Label |
| --- | --- |
| `CURRENT` | 현재 |
| `MORNING` | 오전 |
| `AFTERNOON` | 오후 |
| `EVENING` | 저녁 |

UX 기준:

- 기본 선택은 `CURRENT`다.
- 추천 생성 중에는 상황 선택, 시간대 선택, 생성 버튼을 비활성화한다.
- 추천 결과에는 생성 당시 forecastPeriod label을 표시한다.
- 모바일에서 한 줄 overflow가 나지 않도록 wrap 또는 segmented layout을 사용한다.

## Weather Trust Display

추천 결과와 History card에 source snapshot을 표시한다.

표시 항목:

- 위치 fullName 또는 name
- KMA grid `nx`, `ny`
- 위치 source label: 직접 선택, 현재 위치로 찾음
- weather provider label: KMA 단기예보, fallback
- KMA 사용 여부
- fallback 여부
- 예보 기준: `baseDate baseTime`
- 예보 대상: `forecastDate forecastTime`

문구 기준:

- `kmaUsed=true`: "KMA 단기예보 사용"
- `fallbackUsed=true`: "기본 날씨 fallback 사용"
- `baseDate/baseTime`이 있으면 "발표 기준 YYYY-MM-DD HH:mm" 형태로 표시한다.
- `forecastDate/forecastTime`이 있으면 "예보 대상 YYYY-MM-DD HH:mm" 형태로 표시한다.
- null 값은 빈 칸이 아니라 "확인 불가" 같은 neutral 문구로 표시한다.

## Today Recommendation View

추천 결과에 아래를 표시한다.

- situation label
- forecastPeriod label
- 위치/날씨 source snapshot
- outfit item thumbnail 또는 fallback
- outfit item styleTags
- 추천 이유
- 착용 완료 control
- 피드백 control
- 점수 상세

현재 날씨 패널과 추천 결과 weather card는 같은 DTO 구조를 사용한다. 다만 현재 날씨 조회는 추천을 저장하지 않고, 추천 결과는 생성 당시 snapshot을 표시한다.

## History View

추천 이력 카드에 아래를 표시한다.

- 추천 생성 시각
- situation label
- forecastPeriod label
- 위치/날씨 source snapshot
- 착용 전/착용 완료
- nullable wornAt
- nullable feedback
- outfit item thumbnail 또는 fallback
- outfit item styleTags

과거 이력의 위치/source는 현재 사용자 위치가 아니라 추천 생성 당시 snapshot임을 UI 구조로 자연스럽게 드러낸다.

## Feedback UX

MVP6 피드백 UX를 유지한다.

- sentiment는 마음에 들어요/별로예요 중 하나만 선택 가능하다.
- thermal은 추웠어요/더웠어요 중 하나만 선택 가능하다.
- "피드백 지우기"는 양쪽 `null`로 clear한다.
- 저장 중에는 해당 추천의 피드백 버튼을 비활성화한다.

## 반응형 기준

- 모바일 375px에서 위치 후보, 현재 위치 버튼, 시간대 선택, 피드백 버튼, source snapshot이 겹치지 않아야 한다.
- 버튼 그룹은 stable height를 유지한다.
- 위치 후보 row는 fullName이 길면 wrap을 허용한다.
- source snapshot은 작은 화면에서 2열 고정 대신 wrap되는 key-value list를 사용한다.
- 추천 이력 카드의 status row는 wrap을 허용한다.
- 이미지 thumbnail 영역은 기존 fixed aspect-ratio 기준을 유지한다.

## 접근성 기준

- 현재 위치 버튼은 권한 요청 중 상태를 알 수 있어야 한다.
- 위치 후보 선택은 button 또는 radio semantics를 사용한다.
- forecastPeriod 선택은 현재 선택 상태를 `aria-pressed` 또는 radio semantics로 표현한다.
- source snapshot은 색상만으로 KMA/fallback을 구분하지 않는다.
- 저장 성공/실패 문구는 status 영역으로 표시한다.
- 이미지 `alt`는 옷 이름 기반으로 제공한다.

## 제외 범위

- 외부 지도/주소 검색 UI
- 지도 렌더링
- 좌표 원문 저장 설정 UI
- AI 추천 설명 UI
- 피드백 analytics dashboard
- drag and drop tag reorder
- 옷 styleTags 자동 추천
- 이미지 크롭 UI
- 다중 이미지 carousel
- S3/CDN 직접 업로드

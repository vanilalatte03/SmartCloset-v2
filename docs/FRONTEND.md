# Frontend: SmartCloset MVP6 Feedback Personalization

## 목표

MVP6 프론트엔드는 MVP5 반응형 웹앱 위에 추천 상황 선택, 추천 피드백 저장/clear, 옷별 `styleTags`, 추천 이력의 착용/피드백 표시를 추가한다.

사용자는 Today view에서 상황을 고르고 추천을 생성한 뒤, 추천 결과에 대해 "마음에 들어요", "별로예요", "추웠어요", "더웠어요" 피드백을 남길 수 있어야 한다.

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

추천 피드백 API와 이미지 API는 모두 보호 API다. 모든 보호 API 요청에는 `Authorization: Bearer {accessToken}` header가 필요하다.

`401`은 기존 보호 API와 동일하게 인증 만료로 처리한다.

## API Client 기준

MVP6에서 추가/변경할 함수:

- `createRecommendation(accessToken, request?)`
- `replaceRecommendationFeedback(accessToken, recommendationId, request)`는 `RecommendationFeedbackResponse`를 반환한다.
- 기존 `markRecommendationWorn(accessToken, recommendationId)` 유지
- 옷 등록/수정 request에 `styleTags` 포함

추천 생성:

- body 없이 호출 가능해야 한다.
- 사용자가 상황을 선택하면 `{ situation }` body를 보낸다.
- situation이 없으면 서버 기본값 `CASUAL`을 신뢰한다.

피드백 저장:

- PUT은 전체 교체다.
- 선택하지 않은 필드는 보내지 않아도 되지만 client type에서는 명시적으로 `null`을 보내도 된다.
- clear action은 `{ sentiment: null, thermal: null }` 또는 `{}`로 보낼 수 있다.
- 저장 후 Today 결과와 History 카드 상태를 갱신한다.

이미지 blob fetch:

- MVP5와 동일하게 Authorization header를 붙인다.
- object URL은 cleanup한다.

## 타입 기준

```ts
export type RecommendationSituation =
  | 'WORK'
  | 'CASUAL'
  | 'WORKOUT'
  | 'DATE'
  | 'FORMAL';

export type RecommendationFeedbackSentiment = 'LIKED' | 'DISLIKED';

export type RecommendationThermalFeedback = 'TOO_COLD' | 'TOO_HOT';

export type RecommendationRequest = {
  situation?: RecommendationSituation;
};

export type RecommendationFeedbackRequest = {
  sentiment?: RecommendationFeedbackSentiment | null;
  thermal?: RecommendationThermalFeedback | null;
};

export type RecommendationFeedbackStateResponse = {
  sentiment: RecommendationFeedbackSentiment | null;
  thermal: RecommendationThermalFeedback | null;
  updatedAt: string;
};

export type RecommendationFeedbackResponse = {
  recommendationId: number;
  feedback: RecommendationFeedbackStateResponse | null;
};

export type ClothingRequest = {
  name: string;
  category: ClothingCategory;
  color: ClothingColor;
  material: ClothingMaterial;
  minTemperature: number;
  maxTemperature: number;
  rainSuitable: boolean;
  styleTags: string[];
};

export type OutfitItemResponse = {
  id: number;
  name: string;
  category: ClothingCategory;
  color: ClothingColor;
  material: ClothingMaterial;
  styleTags: string[];
  image: ClothingImageResponse | null;
};

export type RecommendationResponse = {
  recommendationId: number;
  situation: RecommendationSituation;
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

## Situation UX

Today view에 상황 선택 control을 둔다.

| Situation | Label |
| --- | --- |
| `WORK` | 출근 |
| `CASUAL` | 캐주얼 |
| `WORKOUT` | 운동 |
| `DATE` | 데이트 |
| `FORMAL` | 격식 |

UX 기준:

- 기본 선택은 `CASUAL`이다.
- 추천 생성 중에는 상황 선택과 생성 버튼을 비활성화한다.
- 추천 결과에는 생성 당시 situation label을 표시한다.
- 상황 선택은 모바일에서 한 줄 overflow가 나지 않도록 wrap 또는 segmented layout을 사용한다.

## Feedback UX

추천 결과 카드에 피드백 control을 둔다.

Sentiment:

- 마음에 들어요 -> `LIKED`
- 별로예요 -> `DISLIKED`

Thermal:

- 추웠어요 -> `TOO_COLD`
- 더웠어요 -> `TOO_HOT`

규칙:

- sentiment는 둘 중 하나만 선택 가능하다.
- thermal은 둘 중 하나만 선택 가능하다.
- sentiment와 thermal은 동시에 저장될 수 있다.
- 같은 버튼을 다시 누르면 해당 필드를 `null`로 바꾼 전체 상태를 PUT한다.
- "피드백 지우기"는 양쪽 `null`로 clear한다.
- 저장 중에는 해당 추천의 피드백 버튼을 비활성화한다.
- 저장 실패 시 기존 `ApiErrorMessage` 패턴을 사용한다.

## Closet View

옷 등록/수정 폼에 `styleTags` 입력을 추가한다.

기준:

- 상황별 추천 style tag chip을 제공해 사용자가 직접 문자열을 외우지 않아도 추가할 수 있게 한다.
- 선택된 추천 style tag chip은 다시 클릭하면 제거된다.
- 쉼표 또는 Enter 기반 tag 추가 중 기존 UX와 가장 잘 맞는 방식을 사용한다.
- blank tag는 추가하지 않는다.
- tag는 trim한다.
- 중복 tag는 추가하지 않는다.
- 저장 전 요약에 tag 개수를 표시한다.
- 옷 카드와 수정 패널에 styleTags chip을 표시한다.
- 기존 이미지 업로드/교체/삭제 UX는 유지한다.

## Preferences View

선호도 화면의 `styleTags`는 추천 개인화에 반영되는 취향 정보로 표시한다.

기준:

- 옷 등록/수정 폼과 같은 상황별 추천 style tag chip을 제공한다.
- 선택된 추천 style tag chip은 다시 클릭하면 제거된다.
- 쉼표 또는 Enter 기반 직접 입력을 지원한다.
- blank tag, 중복 tag, 단일 tag 길이 제한은 옷 등록/수정 폼과 동일하게 처리한다.

## Today Recommendation View

추천 결과에 아래를 표시한다.

- situation label
- outfit item thumbnail 또는 fallback
- outfit item styleTags
- 추천 이유
- 착용 완료 control
- 피드백 control
- 점수 상세

추천 결과 피드백 저장 후 `RecommendationResponse.feedback` 또는 feedback response 기준으로 UI 상태를 갱신한다.

## History View

추천 이력 카드에 아래를 표시한다.

- 추천 생성 시각
- situation label
- 착용 전/착용 완료
- nullable wornAt
- nullable feedback
- outfit item thumbnail 또는 fallback
- outfit item styleTags

History에서 피드백을 수정하거나 clear할 수 있다면 Today와 같은 API helper를 재사용한다. MVP6 P0에서는 최소한 이력에서 피드백/착용 여부를 한눈에 보여야 한다.

## 공통 표시 기준

- `LIKED`: 마음에 들어요
- `DISLIKED`: 별로예요
- `TOO_COLD`: 추웠어요
- `TOO_HOT`: 더웠어요
- feedback이 없으면 "피드백 없음" 또는 neutral 상태로 표시한다.

## 반응형 기준

- 모바일 375px에서 상황 선택, 피드백 버튼, 착용 완료 버튼, 이력 상태 pill이 겹치지 않아야 한다.
- 버튼 그룹은 stable height를 유지한다.
- 추천 이력 카드의 status row는 wrap을 허용한다.
- 이미지 thumbnail 영역은 기존 fixed aspect-ratio 기준을 유지한다.

## 접근성 기준

- 상황 선택은 현재 선택 상태를 `aria-pressed` 또는 radio semantics로 표현한다.
- 피드백 버튼은 현재 선택 상태를 알 수 있어야 한다.
- 저장 성공/실패 문구는 status 영역으로 표시한다.
- 이미지 `alt`는 옷 이름 기반으로 제공한다.

## 제외 범위

- AI 추천 설명 UI
- 피드백 analytics dashboard
- drag and drop tag reorder
- 옷 styleTags 자동 추천
- 이미지 크롭 UI
- 다중 이미지 carousel
- S3/CDN 직접 업로드

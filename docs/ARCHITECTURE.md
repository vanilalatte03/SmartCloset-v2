# 아키텍처: SmartCloset MVP6

## 전체 아키텍처 개요

SmartCloset MVP6는 Spring Boot 4.0.6 백엔드와 React+Vite+TypeScript 프론트엔드 앱으로 구성한다. MVP6의 변경 지점은 추천 상황, 추천 피드백 snapshot, 옷별 `styleTags`, 개인화 점수 반영, 이력 표시다.

기존 인증, KMA weather, 위치 catalog, 옷 이미지 저장, 추천 이력 구조는 유지한다.

```text
Controller -> Application Service -> Domain Service -> Repository / Provider
```

추천 점수 계산은 recommendation domain service에 둔다. Controller와 Repository에는 점수 계산 로직을 두지 않는다.

## 권장 패키지 구조

```text
com.smartcloset
├── auth
├── common
├── security
├── user
├── location
├── weather
├── clothing
└── recommendation
    ├── domain
    ├── repository
    ├── application
    ├── presentation
    └── dto
```

MVP6 recommendation domain에는 아래 개념을 둔다.

- `RecommendationSituation`
- `RecommendationFeedbackSentiment`
- `RecommendationThermalFeedback`
- 피드백 snapshot score model
- situation style tag mapping

프론트엔드:

```text
frontend/src
├── api
├── components
├── features
│   ├── auth
│   ├── clothes
│   ├── location
│   ├── preferences
│   ├── recommendation
│   └── history
├── types
└── main.tsx
```

## 인증 경계

공개 API:

- `POST /api/auth/signup`
- `POST /api/auth/login`

그 외 `/api/**` endpoint는 보호 API다.

MVP6 신규/변경 보호 API:

- `POST /api/recommendations` with optional situation body
- `PUT /api/recommendations/{recommendationId}/feedback`
- 옷 JSON API의 `styleTags`

모든 사용자 소유 데이터는 인증 principal의 현재 사용자 id로 제한한다.

## 옷 styleTags 흐름

```text
ClothingController
  -> ClothingService
      -> request styleTags normalize
      -> ClothingItem.updateDetails(..., styleTagsJson)
      -> ClothingResponse
```

규칙:

- 요청 누락은 빈 배열로 처리한다.
- blank tag는 제거한다.
- tag는 trim한다.
- ASCII 비교는 case-insensitive이지만 저장 표현은 request normalization 정책을 따른다.
- 기존 옷 등록/수정 JSON API는 multipart로 바꾸지 않는다.
- 이미지 API는 style tag를 변경하지 않는다.

## 추천 생성 흐름

```text
POST /api/recommendations
  -> RecommendationController
  -> RecommendationService.createRecommendation(userId, situation)
      -> WeatherProvider.getCurrentWeather(userId)
      -> UserRepository / ClothingItemRepository
      -> recent wear/recommendation/feedback snapshots
      -> WeatherSuitabilityFilter
      -> OutfitCandidateGenerator
      -> RecommendationScorer
      -> RecommendationReasonGenerator
      -> RecommendationResultRepository
      -> RecommendationResultItemRepository
```

정책:

- request body가 없거나 `situation`이 없으면 `CASUAL`을 사용한다.
- 추천 상황은 `recommendation_results.situation`에 snapshot으로 저장한다.
- score snapshot은 기존 field를 유지한다.
- `preferenceScore` 내부에서 색상, 소재, style tag, 피드백 보정을 계산한다.
- 추천 생성은 피드백이나 착용 이력을 생성하지 않는다.

## 피드백 저장 흐름

```text
PUT /api/recommendations/{recommendationId}/feedback
  -> RecommendationController
  -> RecommendationService.replaceFeedback(userId, recommendationId, request)
      -> RecommendationResultRepository.findByIdAndUserId
      -> RecommendationResult.replaceFeedback(sentiment, thermal, now)
      -> RecommendationFeedbackResponse
```

정책:

- 현재 사용자 소유 추천만 수정 가능하다.
- 다른 사용자 추천 또는 존재하지 않는 추천은 `RECOMMENDATION_NOT_FOUND`로 실패한다.
- PUT은 전체 교체다.
- 누락 필드는 `null`로 간주한다.
- `sentiment`와 `thermal`이 모두 `null`이면 clear한다.
- clear 시 feedback columns와 `feedback_updated_at`을 모두 `NULL`로 되돌린다.
- `RecommendationFeedbackResponse`는 `recommendationId`와 nullable `RecommendationFeedbackStateResponse`를 감싼다.
- 피드백 저장은 새로운 추천 결과나 착용 이력을 만들지 않는다.

## 착용 완료 흐름

기존 `PATCH /api/recommendations/{recommendationId}/worn` 흐름을 유지한다.

- idempotent하다.
- 이미 착용 완료된 추천은 기존 `WearHistory.wornAt`을 반환한다.
- 추천 이력 DTO에는 nullable `wornAt`을 포함한다.

## 추천 이력 조회 흐름

```text
GET /api/recommendations?limit=20
  -> RecommendationService.getRecommendationHistory
      -> recommendation result ids latest first
      -> result items with clothing item
      -> wear history by recommendation id
      -> RecommendationResponse
```

MVP6 이력 응답은 아래를 포함한다.

- `situation`
- `worn`
- `wornAt`
- `feedback`
- outfit item image metadata
- outfit item styleTags

## 추천 도메인 영향

MVP6에서 추천 도메인은 아래 입력을 추가로 받는다.

- `RecommendationSituation`
- 사용자 선호 styleTags
- 옷별 styleTags
- 최근 14일 recommendation feedback snapshots

아래는 추천 도메인 입력이 아니다.

- 이미지 metadata
- 외부 API 원본 응답
- 프론트 표시 상태

## 트랜잭션 경계

- 추천 생성: weather 조회 후 write transaction으로 추천 결과와 result items 저장
- 추천 이력 조회: readOnly transaction
- 추천 피드백 저장/clear: write transaction
- 착용 완료: write transaction, idempotent
- 옷 등록/수정: write transaction, styleTags 포함
- 옷 이미지 업로드/교체/조회/삭제: MVP5 transaction 및 파일 보상 처리 유지

## 프론트 구조

- `src/types/api.ts`에 MVP6 request/response type을 명시한다.
- `src/api/smartClosetApi.ts`에 `createRecommendation(accessToken, request?)`, `replaceRecommendationFeedback(...)`를 둔다.
- Today view는 상황 선택과 추천 결과 피드백 control을 제공한다.
- Closet view는 옷별 styleTags 입력/표시를 제공한다.
- History view는 상황, 착용 여부, 착용 시각, 피드백 상태를 표시한다.
- 보호 이미지 조회는 계속 Authorization header가 있는 blob fetch와 object URL을 사용한다.

## Storage 정책

옷 이미지 storage 정책은 MVP5를 유지한다.

```yaml
smartcloset:
  clothing:
    image:
      storage-dir: ${CLOTHING_IMAGE_STORAGE_DIR:./uploads/clothing-images}
      max-size-bytes: ${CLOTHING_IMAGE_MAX_SIZE_BYTES:5242880}
```

Docker Compose에서는 `clothing-image-data` volume을 `/data/smartcloset/clothing-images`에 mount한다.

## 금지 사항

- 공개 API를 추가하지 않는다. 이유: 인증 사용자 데이터 경계를 유지해야 한다.
- 공개 `userId` query parameter를 추가하지 않는다. 이유: 현재 사용자 경계는 JWT principal이다.
- 피드백 이벤트 로그 테이블을 만들지 않는다. 이유: MVP6는 최신 snapshot 기반 개인화만 목표로 한다.
- 추천 점수 계산 로직을 Controller나 Repository에 두지 않는다. 이유: 추천 도메인 규칙을 테스트 가능하게 유지해야 한다.
- 이미지 metadata를 추천 점수나 이유에 사용하지 않는다. 이유: MVP6 개인화 입력은 상황, styleTags, 피드백이다.
- AI/GPT 추천을 추가하지 않는다. 이유: MVP6는 설명 가능한 규칙 기반 추천이다.
- 기상청 `getVilageFcst` 외 외부 Weather API를 추가하지 않는다. 이유: weather provider 계약을 유지한다.

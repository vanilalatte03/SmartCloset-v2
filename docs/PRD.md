# PRD: SmartCloset MVP6 추천 피드백/개인화

## 문서 목적

이 문서는 SmartCloset MVP6의 확정 범위를 정의한다. MVP6는 MVP5 옷 이미지 업로드 완료 baseline 위에 추천 상황 선택, 추천 피드백 저장, 최근 피드백 기반 개인화, 옷별 `styleTags` 점수 반영, 추천 이력의 착용/피드백 표시를 추가하는 단계다.

현재 코드 baseline은 MVP6 추천 피드백/개인화 구현 완료 상태다. 이 문서와 `docs/` 아래 문서가 구현 source of truth다.

## 문서 책임

| 계약 영역 | Source of truth |
| --- | --- |
| HTTP endpoint, request/response DTO, 인증/에러 계약 | `docs/API.md` |
| 추천 후보, 점수, 추천 이유, 피드백 반영 | `docs/RECOMMENDATION_RULES.md` |
| 백엔드 구조, transaction, storage/provider 정책 | `docs/ARCHITECTURE.md` |
| DB schema와 JPA/entity 기준 | `docs/ERD.md` |
| 프론트 API client, 타입, UX, 반응형 기준 | `docs/FRONTEND.md` |
| 데모/공유 검증 | `docs/DEMO_SCENARIO.md`, `docs/SHARING_GUIDE.md` |
| 결정 배경 | `docs/ADR.md`, `docs/adr/011-mvp6-feedback-personalization.md` |

## MVP6 한 줄 정의

사용자가 추천 상황과 추천 후 피드백을 남기면, SmartCloset이 최근 피드백과 옷별 style tag를 다음 추천 점수와 이유에 반영한다.

## 목표

- 사용자가 "마음에 들어요", "별로예요", "추웠어요", "더웠어요" 같은 착용 후 피드백을 저장할 수 있게 한다.
- 최근 피드백과 상황을 이용해 같은 실수를 줄이고 선호하는 조합을 더 잘 추천한다.
- 기존 선호도 `styleTags`를 저장/표시용에서 추천 점수 입력으로 승격한다.
- 추천 이력에서 착용 여부와 피드백 상태를 빠르게 확인하게 한다.

## 현재 Baseline

- 공개 API는 `POST /api/auth/signup`, `POST /api/auth/login`뿐이다.
- 그 외 API는 `Authorization: Bearer {accessToken}` header를 요구한다.
- 공개 HTTP API는 `userId` query parameter를 받지 않는다.
- 현재 사용자 전용 응답 DTO는 `userId`를 노출하지 않는다.
- 사용자 소유 옷장, 위치, 선호도, 추천 이력, 착용 이력은 인증 사용자별로 분리한다.
- 추천 생성 API는 `POST /api/recommendations`다.
- 추천 이력 조회 API는 `GET /api/recommendations?limit={limit}`이며 기본 20, 최소 1, 최대 50, 최신순이다.
- 현재 날씨 요약 API는 `GET /api/weather/current`이며 보호 API다.
- 프론트 access token 저장 위치는 `sessionStorage`다.
- 옷 이미지 업로드/교체/조회/삭제, 추천/이력 썸네일, Docker Compose image volume은 MVP5 계약을 유지한다.

## 해결하려는 문제

- 현재 추천은 생성과 착용 완료만 저장하므로 사용자가 싫어한 조합이나 불편했던 온도 경험을 다음 추천에 반영하지 못한다.
- `styleTags`가 사용자 선호도에 저장되지만 추천 후보별 점수 차이를 만들지 못한다.
- 추천 생성 시 출근, 운동, 격식 같은 상황을 고를 수 없어 같은 옷장이라도 다른 목적의 추천을 만들기 어렵다.
- 추천 이력에서 착용 여부와 피드백 상태를 함께 파악하기 어렵다.

## 핵심 사용자 시나리오

1. 사용자가 Closet view에서 옷을 등록하거나 수정하며 `styleTags`를 입력한다.
2. 사용자가 Today view에서 상황을 선택한다.
3. 사용자가 추천을 생성한다.
4. 추천 결과에 상황과 추천 이유가 표시된다.
5. 사용자가 추천을 착용 완료 처리한다.
6. 사용자가 마음에 들어요, 별로예요, 추웠어요, 더웠어요 피드백을 저장하거나 clear한다.
7. 다음 추천은 최근 14일 피드백과 style tag 점수를 반영한다.
8. History view에서 추천별 상황, 착용 여부, 착용 시각, 피드백을 확인한다.

## MVP6 우선순위

### P0: 피드백 저장

- 추천 1건당 최신 피드백 snapshot 1개를 저장한다.
- `sentiment`는 `LIKED`, `DISLIKED`, `null`을 허용한다.
- `thermal`은 `TOO_COLD`, `TOO_HOT`, `null`을 허용한다.
- PUT feedback은 전체 교체다.
- 누락 필드는 `null`로 간주한다.
- `{}` 또는 양쪽 `null` 요청은 피드백 전체 clear다.
- 현재 사용자 소유 추천만 피드백을 저장할 수 있다.

### P0: 상황 선택

- 추천 상황 enum은 `WORK`, `CASUAL`, `WORKOUT`, `DATE`, `FORMAL`이다.
- `POST /api/recommendations` body가 없거나 `situation`이 누락되면 `CASUAL`을 사용한다.
- 추천 결과는 생성 당시 상황 snapshot을 저장하고 응답한다.

### P0: 옷별 styleTags

- `ClothingRequest`와 `ClothingResponse`는 `styleTags: string[]`를 포함한다.
- 누락된 `styleTags`는 빈 배열로 처리한다.
- blank tag는 저장하지 않는다.
- 중복 tag는 제거한다.
- 단일 tag 최대 길이는 30자다.
- 비교는 trim 후 수행하고 ASCII는 case-insensitive다.
- 사용자 선호 `styleTags`와 옷별 `styleTags`를 추천 점수에 반영한다.

### P0: 개인화 점수와 이유

- 총점 100점과 기존 score response field를 유지한다.
- `preferenceScore` 최대 10점 내부에 색상, 소재, style tag, 최근 피드백 보정을 반영한다.
- 피드백 반영 window는 최근 14일이다.
- 추천 이유는 template 기반으로 유지하며 상황, style tag, 최근 피드백 반영 문구를 추가한다.

### P0: History UX

- 추천 이력은 상황, 착용 여부, 착용 시각, 피드백 상태를 표시한다.
- 피드백 저장/clear 후 이력 카드 상태가 갱신된다.
- 모바일 375px에서 피드백 버튼과 이력 상태가 겹치지 않는다.

## 포함 범위

- `clothing_items.style_tags_json`
- `recommendation_results.situation`
- `recommendation_results.sentiment_feedback`
- `recommendation_results.thermal_feedback`
- `recommendation_results.feedback_updated_at`
- `RecommendationSituation`, `RecommendationFeedbackSentiment`, `RecommendationThermalFeedback` enum
- `RecommendationRequest`, `RecommendationFeedbackRequest`, `RecommendationFeedbackStateResponse`, `RecommendationFeedbackResponse`
- `RecommendationResponse.situation`, `wornAt`, `feedback`
- 최근 피드백 snapshot 기반 `preferenceScore` 반영
- 상황별 styleTags 매핑표
- 프론트 상황 선택, 피드백 저장/clear, 이력 표시 UX
- MVP6 phase 문서와 docs-check 규칙

## 제외 범위

- AI/GPT 추천
- AI 자동 태깅
- 피드백 이벤트 로그 테이블과 analytics
- 옷별 styleTags 자동 추론
- preference normalization table 분리
- 쇼핑 추천
- refresh token
- social login
- email verification
- password reset
- Redis
- 외부 주소/지도 검색 API
- AWS 배포와 CD 자동화
- S3/CDN 이미지 hosting
- 다중 이미지 업로드

## API 변경 기준

- 새 공개 API는 추가하지 않는다.
- 피드백 API는 보호 API다.
- 추천 feedback, situation, clothing styleTags는 현재 사용자 소유 데이터로만 처리한다.
- 기존 `PATCH /api/recommendations/{recommendationId}/worn`는 유지하고 idempotent해야 한다.
- `POST /api/recommendations`는 body 없이도 기존 호출이 성공해야 한다.
- 현재 사용자 전용 DTO에 `userId`를 되살리지 않고 공개 `userId` query parameter를 추가하지 않는다.

## 데이터/ERD 기준

- 별도 피드백 이벤트 로그 테이블은 만들지 않는다.
- 추천 결과 row에 최신 feedback snapshot과 update 시각을 둔다.
- 착용 완료 시각은 추천 이력 표시를 위해 응답 DTO에 포함한다.
- 옷별 styleTags는 `clothing_items.style_tags_json` JSON array string으로 저장한다.
- 운영 DB migration 전략은 기존과 같이 Hibernate `ddl-auto=update`와 로컬 Docker Compose reset 기준으로 검증한다.

## 프론트엔드 기준

- Today view에는 상황 선택 control을 둔다.
- 추천 결과에는 피드백 control을 둔다.
- History view는 추천별 상황, 착용 여부, 착용 시각, 피드백을 함께 보여준다.
- Closet view는 옷 등록/수정 시 `styleTags`를 입력하고 표시한다.
- 큰 state-management library를 추가하지 않는다.

## 추천 규칙 기준

상세 추천 계약은 `docs/RECOMMENDATION_RULES.md`를 따른다.

- `preferenceScore = clamp(color 0/2 + material 0/2 + styleTag 0..3 + feedbackAdjustment -3..3, 0, 10)`
- 부정 피드백은 긍정 피드백보다 우선한다.
- 여러 부정 signal이 있으면 가장 강한 감점을 사용한다.
- 이미지 존재 여부는 추천 점수, 후보 필터링, 추천 이유에 영향을 주지 않는다.

## 완료 기준

- 로그인한 사용자가 상황을 선택해 추천을 생성할 수 있다.
- body 없이 `POST /api/recommendations`를 호출하면 `CASUAL` 추천이 생성된다.
- 옷 등록/수정/조회에서 `styleTags`가 저장되고 응답된다.
- 추천 결과와 추천 이력에 `situation`, `wornAt`, `feedback`이 포함된다.
- 추천 피드백 PUT 전체 교체, 누락/null 처리, clear가 동작한다.
- 다른 사용자 추천에는 피드백을 저장할 수 없다.
- 최근 피드백과 style tag가 `preferenceScore`와 추천 이유에 반영된다.
- 기존 착용 완료 API는 계속 idempotent하다.
- MVP5 이미지 업로드와 썸네일 흐름은 유지된다.

## 테스트/검증 기준

- `git diff --check`
- `./gradlew test`
- `./gradlew build`
- `cd frontend && npm run build`
- `docker compose config --quiet`
- `python3 scripts/checks.py --docs-check-config phases/6-smartcloset-feedback-personalization/docs-checks.json --docs-check`

## 결정 완료 사항

- 피드백 모델: 추천 결과별 최신 상태 snapshot
- feedback PUT 의미: 전체 교체, 누락 필드는 `null`, 양쪽 `null`은 clear
- 점수 계약: 총점 100점과 기존 score DTO 유지, `preferenceScore` 내부 확장
- styleTags 점수화: 옷별 styleTags와 사용자 선호 styleTags, 상황 매핑 tag를 비교
- 기본 상황: `CASUAL`

# MVP6를 추천 피드백/개인화 MVP로 정의

## 상태
승인됨

## 맥락

MVP5에서는 사용자가 옷 이미지를 업로드하고 추천 결과와 추천 이력에서 썸네일을 확인할 수 있게 됐다. 다만 추천은 여전히 생성과 착용 완료만 저장한다. 실사용 단계에서는 사용자가 추천이 좋았는지, 별로였는지, 춥거나 더웠는지를 남길 수 있어야 다음 추천이 개선된다.

또한 MVP3부터 사용자 선호도에 `styleTags`를 저장했지만 MVP5까지는 표시용 데이터였다. MVP6에서는 옷에도 style tag를 부여하고, 사용자 선호 tag와 상황 tag를 추천 점수와 이유에 반영한다.

## 결정

MVP6는 추천 피드백/개인화 MVP다.

- `ClothingRequest`와 `ClothingResponse`에 `styleTags: string[]`를 추가한다.
- 옷별 style tag는 `clothing_items.style_tags_json` JSON array string으로 저장한다.
- 추천 생성은 계속 `POST /api/recommendations`를 사용한다.
- 추천 생성 request body는 선택이며 `situation`을 받을 수 있다.
- body가 없거나 `situation`이 누락되면 `CASUAL`을 사용한다.
- 추천 상황 enum은 `WORK`, `CASUAL`, `WORKOUT`, `DATE`, `FORMAL`이다.
- 추천 결과에는 생성 당시 situation snapshot을 저장한다.
- 추천 피드백은 `PUT /api/recommendations/{recommendationId}/feedback` 보호 API로 저장한다.
- 피드백은 추천 결과별 최신 상태 snapshot으로 저장한다.
- 별도 feedback event log table은 만들지 않는다.
- PUT feedback은 전체 교체다.
- 누락 필드는 `null`로 간주한다.
- `sentiment`와 `thermal`이 모두 `null`이면 피드백 clear다.
- `RecommendationResponse`에 `situation`, nullable `wornAt`, nullable `feedback`을 추가한다.

## 점수 결정

총점 100점과 기존 score response field는 유지한다.

`preferenceScore` 10점 내부만 확장한다.

```text
preferenceScore = clamp(color 0/2 + material 0/2 + styleTag 0..3 + feedbackAdjustment -3..3, 0, 10)
```

최근 피드백 반영 window는 14일이다. 긍정/부정 signal이 충돌하면 부정 signal을 우선하고, 여러 부정 signal은 가장 강한 감점을 사용한다.

## 상황별 styleTags 매핑

| Situation | Label | Matching styleTags |
| --- | --- | --- |
| `WORK` | 출근 | `WORK`, `OFFICE`, `MINIMAL`, `SMART`, `출근`, `오피스`, `미니멀`, `단정` |
| `CASUAL` | 캐주얼 | `CASUAL`, `DAILY`, `COMFORT`, `MINIMAL`, `캐주얼`, `데일리`, `편안함`, `미니멀` |
| `WORKOUT` | 운동 | `WORKOUT`, `SPORTY`, `ACTIVE`, `COMFORT`, `운동`, `스포티`, `활동적`, `편안함` |
| `DATE` | 데이트 | `DATE`, `NEAT`, `POINT`, `MINIMAL`, `데이트`, `깔끔`, `포인트`, `미니멀` |
| `FORMAL` | 격식 | `FORMAL`, `OFFICIAL`, `SMART`, `MINIMAL`, `격식`, `포멀`, `단정`, `미니멀` |

## 결과

- 기존 인증 사용자 API 경계를 유지하면서 추천 피드백을 저장할 수 있다.
- 사용자 선호 style tag와 옷별 style tag가 실제 추천 점수에 반영된다.
- 추천 상황이 점수와 이유의 입력으로 고정된다.
- 추천 이력에서 착용 여부와 피드백 상태를 함께 볼 수 있다.
- 피드백을 이벤트 로그가 아니라 최신 snapshot으로 저장해 MVP 구현 범위를 제한한다.

## 범위 제외

- AI/GPT 추천
- AI 자동 태깅
- 피드백 이벤트 로그 분석 플랫폼
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

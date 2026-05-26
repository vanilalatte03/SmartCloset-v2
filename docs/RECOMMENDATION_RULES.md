# 추천 규칙: SmartCloset MVP6

## 1. 추천 규칙의 목적

SmartCloset MVP6의 추천은 AI/GPT 추천이 아니라 설명 가능하고 테스트 가능한 규칙 기반 추천이다.

MVP6는 추천 상황, 옷별 `styleTags`, 최근 추천 피드백을 `preferenceScore`에 반영한다. 총점 100점과 기존 score response field는 유지한다.

## MVP6 결정

- 추천 생성 API는 `POST /api/recommendations`만 사용한다.
- `POST /api/recommendations` body가 없거나 `situation`이 누락되면 `CASUAL`이다.
- 추천 상황은 추천 결과에 snapshot으로 저장한다.
- 추천 피드백은 추천 결과별 최신 상태 snapshot으로 저장한다.
- 최근 피드백 반영 window는 14일이다.
- 옷별 `styleTags`는 추천 점수와 추천 이유에 반영한다.
- 이미지 존재 여부는 추천 점수, 후보 필터링, tie-break, 추천 이유에 영향을 주지 않는다.
- 현재 날씨 요약 API `GET /api/weather/current`는 추천 결과, 추천 이력, 착용 이력, 피드백을 생성하거나 변경하지 않는다.

## 2. 입력 데이터

추천 로직은 아래 데이터를 입력으로 사용한다.

- 현재 인증 사용자 id
- 현재 인증 사용자의 활성 옷 목록
- 각 옷의 `styleTags`
- 현재 인증 사용자의 위치 snapshot
- 현재 인증 사용자의 최근 착용 이력
- 현재 인증 사용자의 최근 추천 이력
- 현재 인증 사용자의 최근 추천 피드백 snapshot
- 현재 인증 사용자의 선호 색상/소재/styleTags
- 추천 상황 `RecommendationSituation`
- 내부 `WeatherCondition`

추천 로직은 아래 데이터를 입력으로 사용하지 않는다.

- 옷 이미지 존재 여부
- 옷 이미지 MIME type
- 옷 이미지 크기
- 옷 이미지 업로드 시각

## 3. RecommendationSituation

| Value | Label | Matching styleTags |
| --- | --- | --- |
| `WORK` | 출근 | `WORK`, `OFFICE`, `MINIMAL`, `SMART`, `출근`, `오피스`, `미니멀`, `단정` |
| `CASUAL` | 캐주얼 | `CASUAL`, `DAILY`, `COMFORT`, `MINIMAL`, `캐주얼`, `데일리`, `편안함`, `미니멀` |
| `WORKOUT` | 운동 | `WORKOUT`, `SPORTY`, `ACTIVE`, `COMFORT`, `운동`, `스포티`, `활동적`, `편안함` |
| `DATE` | 데이트 | `DATE`, `NEAT`, `POINT`, `MINIMAL`, `데이트`, `깔끔`, `포인트`, `미니멀` |
| `FORMAL` | 격식 | `FORMAL`, `OFFICIAL`, `SMART`, `MINIMAL`, `격식`, `포멀`, `단정`, `미니멀` |

Style tag 비교 정책:

- 비교 전 trim한다.
- blank tag는 저장하거나 비교하지 않는다.
- ASCII 알파벳은 case-insensitive로 비교한다.
- 한글 등 non-ASCII는 trim 후 exact match로 비교한다.
- 중복 tag는 점수 중복 가산하지 않는다.

## 4. Weather source 정책

추천 도메인은 외부 API 응답 모델에 직접 의존하지 않는다. 추천 규칙은 항상 내부 `WeatherCondition`만 입력으로 받는다.

현재 기본 weather source는 인증 사용자 위치 `nx`, `ny`로 조회한 기상청 단기예보 조회서비스 `getVilageFcst` JSON 응답이다. 서비스키 미설정, 외부 API 실패, `NODATA`, 필수 category 누락, 파싱 실패 시에는 `StaticWeatherProvider` fallback 날씨를 사용한다.

fallback 값:

| Field | Value |
| --- | --- |
| `temperature` | `12` |
| `weatherType` | `CLOUDY` |
| `rainy` | `false` |
| `windy` | `false` |

## 5. ClothingItem 추천 속성

추천 규칙에서 사용하는 `ClothingItem` 속성:

- `id`
- `name`
- `category`
- `color`
- `material`
- `minTemperature`
- `maxTemperature`
- `rainSuitable`
- `archived`
- `styleTags`

`category` enum:

- `TOP`
- `BOTTOM`
- `OUTER`

`color` enum:

- `BLACK`
- `WHITE`
- `GRAY`
- `NAVY`
- `BLUE`
- `BROWN`
- `BEIGE`
- `RED`
- `GREEN`
- `YELLOW`
- `UNKNOWN`

`material` enum:

- `COTTON`
- `DENIM`
- `KNIT`
- `WOOL`
- `POLYESTER`
- `NYLON`
- `UNKNOWN`

## 6. 사용자 선호도 입력

선호도 API는 배열로 주고받는다.

```json
{
  "preferredColors": ["NAVY", "BLACK"],
  "preferredMaterials": ["COTTON"],
  "styleTags": ["MINIMAL", "CASUAL"]
}
```

저장은 `users` 테이블 JSON 문자열 컬럼으로 한다.

- `preferred_colors_json`
- `preferred_materials_json`
- `style_tags_json`

MVP6에서는 `preferredColors`, `preferredMaterials`, `styleTags` 모두 `preferenceScore`에 반영한다.

## 7. 후보 필터링 규칙

후보 필터링은 조합 생성 전에 수행하는 hard filter다.

- `archived=true`인 옷은 제외한다.
- 현재 `temperature`가 `minTemperature`보다 낮으면 제외한다.
- 현재 `temperature`가 `maxTemperature`보다 높으면 제외한다.
- hard filter 이후 TOP이 1개도 없으면 `NO_TOP_AVAILABLE` 실패로 처리한다.
- hard filter 이후 BOTTOM이 1개도 없으면 `NO_BOTTOM_AVAILABLE` 실패로 처리한다.
- OUTER 필수 조건에서 hard filter를 통과한 OUTER가 1개도 없으면 `OUTER_REQUIRED_BUT_NOT_AVAILABLE` 실패로 처리한다.

style tag, 이미지, 피드백은 hard filter가 아니다.

## 8. OUTER 필수/선택 규칙

| Condition | Rule |
| --- | --- |
| `temperature <= 12` | OUTER 필수 |
| `13 <= temperature <= 16` | OUTER 선택, 포함 시 weatherScore 가산 |
| `temperature >= 17` | OUTER optional, 미포함을 기본적으로 더 자연스럽게 평가 |
| `rainy=true` | OUTER 포함 조합에 weatherScore 가산 |
| `windy=true` | OUTER 포함 조합에 weatherScore 가산 |

`rainy=true` 또는 `windy=true`여도 `temperature >= 17`이면 OUTER를 필수로 만들지 않는다.

## 9. 후보 조합 생성 규칙

- 기본 조합은 TOP + BOTTOM이다.
- OUTER 필수 조건이면 TOP + BOTTOM + OUTER 조합만 생성한다.
- OUTER 선택 조건이면 TOP + BOTTOM 조합과 TOP + BOTTOM + OUTER 조합을 모두 생성할 수 있다.
- 생성 가능한 후보 조합이 없으면 실패 코드를 반환한다.
- 후보 생성 순서는 정렬된 `id` 오름차순 TOP, BOTTOM, OUTER 목록을 순회해 결정 가능하게 만든다.

## 10. 점수 계산 기준

추천 총점은 100점 기준이다.

| Score | Max |
| --- | ---: |
| `weatherScore` | 35 |
| `colorScore` | 25 |
| `wearHistoryScore` | 20 |
| `recommendationHistoryScore` | 10 |
| `preferenceScore` | 10 |

```text
totalScore = weatherScore
           + colorScore
           + wearHistoryScore
           + recommendationHistoryScore
           + preferenceScore
```

## 11. preferenceScore

MVP6의 `preferenceScore`는 최대 10점이다.

```text
preferenceScore = clamp(
    colorPreferenceScore
  + materialPreferenceScore
  + styleTagScore
  + feedbackAdjustment,
  0,
  10
)
```

세부 기준:

| Component | Range | Rule |
| --- | ---: | --- |
| colorPreferenceScore | 0 or 2 | 후보 옷 중 하나 이상이 `preferredColors`와 일치하면 2점 |
| materialPreferenceScore | 0 or 2 | 후보 옷 중 하나 이상이 `preferredMaterials`와 일치하면 2점 |
| styleTagScore | 0..3 | 사용자 선호 tag 매칭 2점 + 상황 tag 매칭 1점 |
| feedbackAdjustment | -3..3 | 최근 14일 피드백 기반 보정 |

Style tag 점수:

- 후보 옷 중 하나 이상이 사용자 선호 `styleTags`와 겹치면 2점이다.
- 후보 옷 중 하나 이상이 선택 상황의 matching styleTags와 겹치면 1점이다.
- 같은 tag가 여러 옷에 있어도 중복 가산하지 않는다.

## 12. 피드백 반영 기준

피드백은 추천 결과별 최신 snapshot만 사용한다.

피드백 enum:

- `sentiment`: `LIKED`, `DISLIKED`
- `thermal`: `TOO_COLD`, `TOO_HOT`

최근 피드백 window:

- `feedback_updated_at >= requestedAt - 14 days`
- clear된 피드백은 반영하지 않는다.

옷 겹침 판단:

- 같은 옷 조합: 후보의 item id set이 피드백 추천 결과의 item id set과 같다.
- 일부 옷 겹침: 하나 이상의 item id가 겹치지만 set이 같지는 않다.

점수 보정:

| Feedback | Condition | Adjustment |
| --- | --- | ---: |
| `LIKED` | 같은 옷 조합 | +3 |
| `LIKED` | 일부 옷 겹침 | +1 |
| `DISLIKED` | 같은 옷 조합 | -3 |
| `DISLIKED` | 일부 옷 겹침 | -1 |
| `TOO_COLD` | 현재 기온이 피드백 당시 기온 +3도 이하이고 같은 옷 조합 | -2 |
| `TOO_COLD` | 현재 기온이 피드백 당시 기온 +3도 이하이고 일부 옷 겹침 | -1 |
| `TOO_HOT` | 현재 기온이 피드백 당시 기온 -3도 이상이고 같은 옷 조합 | -2 |
| `TOO_HOT` | 현재 기온이 피드백 당시 기온 -3도 이상이고 일부 옷 겹침 | -1 |

충돌 처리:

- 긍정/부정 signal이 충돌하면 부정 signal을 우선한다.
- 여러 부정 signal이 있으면 가장 강한 감점을 사용한다.
- 여러 긍정 signal만 있으면 가장 큰 가산을 사용한다.
- 최종 `feedbackAdjustment`는 -3..3으로 clamp한다.

## 13. Tie-break rule

동점 처리 순서는 결정적이어야 한다.

1. `totalScore` 높은 순
2. `weatherScore` 높은 순
3. `preferenceScore` 높은 순
4. `colorScore` 높은 순
5. `wearHistoryScore` 높은 순
6. `recommendationHistoryScore` 높은 순
7. TOP id 오름차순
8. BOTTOM id 오름차순
9. OUTER 정책과 OUTER id 오름차순
10. 후보 생성 순서

## 14. 추천 이유

추천 이유는 template 기반이며 AI-generated가 아니다.

MVP6 이유 후보:

- 날씨와 기온에 맞는 조합
- 색상 조합
- 최근 착용/추천 이력 다양성
- 선호 색상/소재 반영
- 상황별 style tag 반영
- 사용자 선호 style tag 반영
- 최근 좋아요 피드백 반영
- 최근 별로예요/춥다/덥다 피드백 회피

추천 이유는 3개 이상 5개 이하로 반환한다.

## 15. 추천 이력

추천 이력은 기존처럼 `recommendation_result_items`가 `clothing_items`를 참조한다.

MVP6 추천 이력 DTO는 아래 상태를 함께 반환한다.

- `situation`
- `worn`
- nullable `wornAt`
- nullable `feedback`
- outfit item의 현재 image metadata
- outfit item의 현재 `styleTags`

과거 추천 당시 이미지 snapshot과 옷 style tag snapshot은 별도로 저장하지 않는다. 추천 상황과 점수, 피드백은 `recommendation_results` row의 snapshot을 사용한다.

## 16. 테스트 기대사항

MVP6 구현 후 추천 규칙 테스트는 아래를 증명해야 한다.

- body 없는 추천 생성은 `CASUAL`을 사용한다.
- 상황별 styleTags 매핑이 점수와 이유에 반영된다.
- 사용자 선호 `styleTags`와 옷별 `styleTags` 교집합이 점수와 이유에 반영된다.
- 피드백 PUT 전체 교체와 clear가 동작한다.
- 최근 14일 피드백만 `preferenceScore`에 반영된다.
- `LIKED`, `DISLIKED`, `TOO_COLD`, `TOO_HOT` 보정 기준이 결정적으로 동작한다.
- 긍정/부정 충돌 시 부정 signal이 우선한다.
- 이미지 metadata는 점수와 이유에 영향을 주지 않는다.
- 기존 deterministic recommendation test는 유지된다.

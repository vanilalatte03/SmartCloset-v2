# 추천 규칙: SmartCloset MVP7

## 1. 추천 규칙의 목적

SmartCloset MVP7의 추천은 AI/GPT 추천이 아니라 설명 가능하고 테스트 가능한 규칙 기반 추천이다.

MVP7은 MVP6의 상황, 옷별 `styleTags`, 최근 추천 피드백 기반 `preferenceScore`를 유지하면서, 추천에 사용할 예보 시간대 `forecastPeriod`와 추천 결과의 위치/날씨 source snapshot을 추가한다. 총점 100점과 기존 score response field는 유지한다.

## MVP7 결정

- 추천 생성 API는 `POST /api/recommendations`만 사용한다.
- `POST /api/recommendations` body가 없거나 `situation`이 누락되면 `CASUAL`이다.
- `forecastPeriod`가 누락되면 `CURRENT`다.
- 추천 상황은 추천 결과에 snapshot으로 저장한다.
- 예보 시간대와 실제 사용한 forecast date/time도 추천 결과에 snapshot으로 저장한다.
- 추천 위치와 weather source metadata를 추천 결과에 snapshot으로 저장한다.
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
- 현재 인증 사용자의 위치 source
- 현재 인증 사용자의 최근 착용 이력
- 현재 인증 사용자의 최근 추천 이력
- 현재 인증 사용자의 최근 추천 피드백 snapshot
- 현재 인증 사용자의 선호 색상/소재/styleTags
- 추천 상황 `RecommendationSituation`
- 예보 시간대 `ForecastPeriod`
- 내부 `WeatherCondition`
- weather source metadata snapshot

추천 점수 로직은 아래 데이터를 입력으로 사용하지 않는다.

- 옷 이미지 존재 여부
- 옷 이미지 MIME type
- 옷 이미지 크기
- 옷 이미지 업로드 시각
- raw KMA 응답 JSON
- 브라우저 GPS 원문 좌표

## 3. ForecastPeriod

| Value | Label | Weather target policy |
| --- | --- | --- |
| `CURRENT` | 현재 | 현재 KST 시각 이후 가장 가까운 예보 |
| `MORNING` | 오전 | 오늘 오전 대표 예보 |
| `AFTERNOON` | 오후 | 오늘 오후 대표 예보 |
| `EVENING` | 저녁 | 오늘 저녁 대표 예보 |

권장 대표 forecast target:

| ForecastPeriod | Target time |
| --- | --- |
| `MORNING` | `0900` |
| `AFTERNOON` | `1500` |
| `EVENING` | `2100` |

선택 정책:

- KMA base date/time은 기존 단기예보 발표 시각 계산 규칙을 유지한다.
- `CURRENT`는 현재 시각 이후 가장 가까운 forecast group을 선택한다.
- `MORNING`, `AFTERNOON`, `EVENING`은 해당 target time과 같은 날짜의 forecast group을 우선한다.
- target time이 없으면 같은 날짜의 가장 가까운 이후 forecast group을 사용한다.
- 이후 forecast group도 없으면 같은 날짜의 가장 가까운 이전 forecast group을 사용한다.
- 실제 선택된 forecast date/time은 `WeatherSourceResponse`와 추천 snapshot에 저장한다.

## 4. RecommendationSituation

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

## 5. Weather source 정책

추천 도메인은 외부 API 응답 모델에 직접 의존하지 않는다. 추천 점수는 항상 내부 `WeatherCondition`만 입력으로 받는다.

Weather provider는 MVP7에서 `WeatherCondition`과 함께 아래 source metadata를 application layer에 제공한다.

- location code/name/fullName
- location nx/ny
- location source
- provider: `KMA_VILAGE_FORECAST` 또는 `STATIC_FALLBACK`
- `kmaUsed`
- `fallbackUsed`
- `baseDate`
- `baseTime`
- `forecastDate`
- `forecastTime`

현재 기본 weather source는 인증 사용자 위치 `nx`, `ny`로 조회한 기상청 단기예보 조회서비스 `getVilageFcst` JSON 응답이다. 서비스키 미설정, 외부 API 실패, `NODATA`, 필수 category 누락, 파싱 실패 시에는 `StaticWeatherProvider` fallback 날씨를 사용한다.

fallback 값:

| Field | Value |
| --- | --- |
| `temperature` | `12` |
| `weatherType` | `CLOUDY` |
| `rainy` | `false` |
| `windy` | `false` |

raw KMA 응답 JSON은 추천 규칙 입력, 추천 결과 DB snapshot, API response에 포함하지 않는다.

## 6. ClothingItem 추천 속성

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

## 7. 사용자 선호도 입력

선호도 API는 배열로 주고받는다.

```json
{
  "preferredColors": ["NAVY", "BLACK"],
  "preferredMaterials": ["COTTON"],
  "styleTags": ["MINIMAL", "CASUAL"]
}
```

MVP6와 동일하게 `preferredColors`, `preferredMaterials`, `styleTags` 모두 `preferenceScore`에 반영한다.

## 8. 후보 필터링 규칙

후보 필터링은 조합 생성 전에 수행하는 hard filter다.

- `archived=true`인 옷은 제외한다.
- 선택된 `forecastPeriod`의 `temperature`가 `minTemperature`보다 낮으면 제외한다.
- 선택된 `forecastPeriod`의 `temperature`가 `maxTemperature`보다 높으면 제외한다.
- hard filter 이후 TOP이 1개도 없으면 `NO_TOP_AVAILABLE` 실패로 처리한다.
- hard filter 이후 BOTTOM이 1개도 없으면 `NO_BOTTOM_AVAILABLE` 실패로 처리한다.
- OUTER 필수 조건에서 hard filter를 통과한 OUTER가 1개도 없으면 `OUTER_REQUIRED_BUT_NOT_AVAILABLE` 실패로 처리한다.

style tag, 이미지, 피드백, 위치 source는 hard filter가 아니다.

## 9. OUTER 필수/선택 규칙

| Condition | Rule |
| --- | --- |
| `temperature <= 12` | OUTER 필수 |
| `13 <= temperature <= 16` | OUTER 선택, 포함 시 weatherScore 가산 |
| `temperature >= 17` | OUTER optional, 미포함을 기본적으로 더 자연스럽게 평가 |
| `rainy=true` | OUTER 포함 조합에 weatherScore 가산 |
| `windy=true` | OUTER 포함 조합에 weatherScore 가산 |

`rainy=true` 또는 `windy=true`여도 `temperature >= 17`이면 OUTER를 필수로 만들지 않는다.

## 10. 후보 조합 생성 규칙

- 기본 조합은 TOP + BOTTOM이다.
- OUTER 필수 조건이면 TOP + BOTTOM + OUTER 조합만 생성한다.
- OUTER 선택 조건이면 TOP + BOTTOM 조합과 TOP + BOTTOM + OUTER 조합을 모두 생성할 수 있다.
- 생성 가능한 후보 조합이 없으면 실패 코드를 반환한다.
- 후보 생성 순서는 정렬된 `id` 오름차순 TOP, BOTTOM, OUTER 목록을 순회해 결정 가능하게 만든다.

## 11. 점수 계산 기준

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

MVP7은 source 신뢰도 정보를 새 score field로 만들지 않는다.

## 12. preferenceScore

MVP6의 `preferenceScore` 계약을 유지한다.

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

| Component | Range | Rule |
| --- | ---: | --- |
| colorPreferenceScore | 0 or 2 | 후보 옷 중 하나 이상이 `preferredColors`와 일치하면 2점 |
| materialPreferenceScore | 0 or 2 | 후보 옷 중 하나 이상이 `preferredMaterials`와 일치하면 2점 |
| styleTagScore | 0..3 | 사용자 선호 tag 매칭 2점 + 상황 tag 매칭 1점 |
| feedbackAdjustment | -3..3 | 최근 14일 피드백 기반 보정 |

## 13. 피드백 반영 기준

피드백은 추천 결과별 최신 snapshot만 사용한다.

최근 피드백 window:

- `feedback_updated_at >= requestedAt - 14 days`
- clear된 피드백은 반영하지 않는다.

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

## 14. Tie-break rule

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

위치 source, KMA/fallback 여부, forecastPeriod label은 tie-break에 사용하지 않는다.

## 15. 추천 이유

추천 이유는 template 기반이며 AI-generated가 아니다.

MVP7 이유 후보:

- 선택한 예보 시간대와 forecast time
- 날씨와 기온에 맞는 조합
- 색상 조합
- 최근 착용/추천 이력 다양성
- 선호 색상/소재 반영
- 상황별 style tag 반영
- 사용자 선호 style tag 반영
- 최근 좋아요 피드백 반영
- 최근 별로예요/춥다/덥다 피드백 회피

추천 이유는 3개 이상 5개 이하로 반환한다. KMA/fallback 여부는 추천 이유가 아니라 weather source 표시 영역에서 다룬다.

## 16. 추천 이력

추천 이력은 기존처럼 `recommendation_result_items`가 `clothing_items`를 참조한다.

MVP7 추천 이력 DTO는 아래 상태를 함께 반환한다.

- `situation`
- `forecastPeriod`
- `weather.location`
- `weather.source`
- `worn`
- nullable `wornAt`
- nullable `feedback`
- outfit item의 현재 image metadata
- outfit item의 현재 `styleTags`

과거 추천 당시 이미지 snapshot과 옷 style tag snapshot은 별도로 저장하지 않는다. 추천 상황, 예보 시간대, 위치/날씨 source, 점수, 피드백은 `recommendation_results` row의 snapshot을 사용한다.

## 17. 테스트 기대사항

MVP7 구현 후 추천/날씨 규칙 테스트는 아래를 증명해야 한다.

- body 없는 추천 생성은 `CASUAL`, `CURRENT`를 사용한다.
- `MORNING`, `AFTERNOON`, `EVENING`이 선택한 forecast target을 source snapshot에 남긴다.
- KMA 성공 시 `kmaUsed=true`, `fallbackUsed=false`다.
- fallback 사용 시 `fallbackUsed=true`이며 fallback weather 값이 유지된다.
- 추천 결과는 생성 당시 location/source snapshot을 저장한다.
- 사용자 위치가 바뀌어도 과거 추천 이력의 location/source snapshot은 바뀌지 않는다.
- 기존 상황별 styleTags 매핑이 점수와 이유에 반영된다.
- 최근 14일 피드백만 `preferenceScore`에 반영된다.
- 이미지 metadata는 점수와 이유에 영향을 주지 않는다.
- 기존 deterministic recommendation test는 유지된다.

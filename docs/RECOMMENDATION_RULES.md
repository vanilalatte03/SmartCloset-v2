# 추천 규칙: SmartCloset 1.5차 MVP

## 1. 추천 규칙의 목적
SmartCloset 1.5차 MVP의 추천은 AI/GPT 추천이 아니라 설명 가능하고 테스트 가능한 규칙 기반 추천이다.

1.5차는 날씨 입력 source를 기상청 단기예보 JSON으로 확장하지만, 후보 생성, 점수 계산, 실패 판단, 최종 tie-break는 1차 MVP와 같은 결정 가능한 규칙을 유지한다.

추천 이유는 AI가 생성하는 자유 문장이 아니다. 각 규칙의 판단 결과를 `RecommendationReasonGenerator`가 템플릿 문장으로 변환한다.

## 2. Weather source 정책
추천 도메인은 외부 API 응답 모델에 직접 의존하지 않는다. 추천 규칙은 항상 내부 `WeatherCondition`만 입력으로 받는다.

1.5차 기본 weather source는 기상청 단기예보 조회서비스 `getVilageFcst` JSON 응답이다. 서비스키 미설정, 외부 API 실패, `NODATA`, 필수 category 누락, 파싱 실패 시에는 `StaticWeatherProvider` fallback 날씨를 사용한다.

KMA forecast group 선택은 provider 책임이다. provider는 현재 KST 이후 가장 가까운 `fcstDate`, `fcstTime` group을 선택해 `WeatherCondition`을 만든다. 선택 group에 필수 category가 누락되면 다른 forecast group으로 이동하지 않고 fallback 또는 strict mode 실패로 처리한다.

fallback 값은 1차 MVP와 동일하다.

| Field | Value |
| --- | --- |
| `temperature` | `12` |
| `weatherType` | `CLOUDY` |
| `rainy` | `false` |
| `windy` | `false` |

fallback 값은 API key 없이도 Docker Compose 데모와 OUTER 필수 흐름을 재현하기 위해 유지한다.

비, 바람, 더운 날씨, 추운 날씨의 세부 점수 테스트는 단위 테스트에서 별도 `WeatherCondition`을 직접 구성해 검증한다.

## 3. WeatherCondition 필드
`WeatherCondition`은 추천 로직에서 사용하는 내부 날씨 모델이다. KMA 응답 DTO와 분리한다.

최소 필드는 다음과 같다.

- `temperature`: 현재 기온. 정수 섭씨 기준.
- `weatherType`: 대표 날씨 타입.
- `rainy`: 비 또는 눈처럼 젖는 조건이 있는지 여부.
- `windy`: 바람이 강한지 여부.

`weatherType` enum은 아래 값으로 제한한다.

- `SUNNY`
- `CLOUDY`
- `RAINY`
- `SNOWY`
- `WINDY`

`weatherType=RAINY` 또는 `weatherType=SNOWY`이면 `rainy=true`로 다루는 것을 권장한다. `weatherType=WINDY`이면 `windy=true`로 다루는 것을 권장한다.

## 4. KMA category 매핑
KMA `getVilageFcst` JSON 응답의 `response.body.items.item[]`에서 같은 forecast time의 category를 묶어 사용한다.

forecast time 선택, KMA 오류 처리, fallback 또는 strict mode 실패 판단은 추천 도메인이 아니라 `WeatherProvider` 구현 책임이다.

1.5차 추천에 필요한 category는 아래 5개다.

| Category | Name | WeatherCondition field |
| --- | --- | --- |
| `TMP` | 1시간 기온 | `temperature` |
| `PTY` | 강수형태 | `weatherType`, `rainy` |
| `SKY` | 하늘상태 | `weatherType` |
| `PCP` | 1시간 강수량 | `rainy` |
| `WSD` | 풍속 | `windy` |

`temperature`:

- `TMP` 값을 정수 섭씨로 변환한다.
- 소수값이 들어오면 가장 가까운 정수로 반올림하거나 구현에서 정한 단일 정책으로 변환하되, 테스트에서 고정한다.

`weatherType`:

| KMA value | WeatherType |
| --- | --- |
| `PTY=1`, `PTY=2`, `PTY=4` | `RAINY` |
| `PTY=3` | `SNOWY` |
| `PTY=0`, `SKY=1` | `SUNNY` |
| `PTY=0`, `SKY=3` 또는 `SKY=4` | `CLOUDY` |

`rainy`:

- `PTY != 0`이면 `true`다.
- `PTY=0`이어도 `PCP`가 유효 강수량이면 `true`다.
- `PCP` 값이 `-`, `null`, `0`, `강수없음`이면 강수 없음으로 본다.

`windy`:

- `WSD >= 4.0`이면 `true`다.
- `WSD < 4.0`이면 `false`다.

`POP`, `REH`, `TMN`, `TMX`, `SNO`, `UUU`, `VVV`, `VEC` 등은 1.5차 추천 점수에는 사용하지 않는다.

## 5. ClothingItem 최소 속성
추천 규칙에서 사용하는 `ClothingItem` 최소 속성은 다음과 같다.

- `id`
- `userId`
- `name`
- `category`
- `color`
- `material`
- `minTemperature`
- `maxTemperature`
- `rainSuitable`
- `archived`

`category` enum은 아래 값으로 제한한다.

- `TOP`
- `BOTTOM`
- `OUTER`

`color` enum은 아래 값으로 제한한다.

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

`material` enum은 아래 값으로 제한한다.

- `COTTON`
- `DENIM`
- `KNIT`
- `WOOL`
- `POLYESTER`
- `NYLON`
- `UNKNOWN`

`UNKNOWN`은 사용자가 색상 또는 재질을 확신하지 못하는 경우를 위한 값이다. `UNKNOWN`은 강한 가산점이나 감점 없이 중립 처리한다.

## 6. 후보 필터링 규칙
후보 필터링은 조합 생성 전에 수행하는 hard filter다. hard filter를 통과하지 못한 옷은 어떤 조합에도 포함하지 않는다.

Hard filter 규칙은 다음과 같다.

- `archived=true`인 옷은 제외한다.
- 현재 `temperature`가 `minTemperature`보다 낮으면 제외한다.
- 현재 `temperature`가 `maxTemperature`보다 높으면 제외한다.
- hard filter 이후 TOP이 1개도 없으면 `NO_TOP_AVAILABLE` 실패로 처리한다.
- hard filter 이후 BOTTOM이 1개도 없으면 `NO_BOTTOM_AVAILABLE` 실패로 처리한다.
- OUTER 필수 조건에서 hard filter를 통과한 OUTER가 1개도 없으면 `OUTER_REQUIRED_BUT_NOT_AVAILABLE` 실패로 처리한다.

`rainSuitable`과 `material`은 hard filter가 아니라 `weatherScore` 보정에 사용한다. 즉, 비 오는 날 `rainSuitable=false`이거나 `WOOL` 소재인 옷도 바로 제외하지 않고 감점한다. 단, 온도 범위를 벗어난 옷은 항상 제외한다.

## 7. OUTER 필수/선택 규칙
OUTER 포함 기준은 현재 `temperature`, `rainy`, `windy`를 기준으로 판단한다.

| Condition | Rule |
| --- | --- |
| `temperature <= 12` | OUTER 필수 |
| `13 <= temperature <= 16` | OUTER 선택, 포함 시 weatherScore 가산 |
| `temperature >= 17` | OUTER optional, 미포함을 기본적으로 더 자연스럽게 평가 |
| `rainy=true` | OUTER 포함 조합에 weatherScore 가산 |
| `windy=true` | OUTER 포함 조합에 weatherScore 가산 |

`rainy=true` 또는 `windy=true`여도 `temperature >= 17`이면 OUTER를 필수로 만들지 않는다. 이 경우 OUTER 포함은 가산점 요소일 뿐이다.

## 8. 후보 조합 생성 규칙
`OutfitCandidate`는 추천 계산 중 생성되는 도메인 모델 또는 value object다. DB Entity로 만들 필요는 없다.

조합 생성 규칙은 다음과 같다.

- 기본 조합은 TOP + BOTTOM이다.
- OUTER 필수 조건이면 TOP + BOTTOM + OUTER 조합만 생성한다.
- OUTER 선택 조건이면 TOP + BOTTOM 조합과 TOP + BOTTOM + OUTER 조합을 모두 생성할 수 있다.
- `archived=true`이거나 온도 hard filter를 통과하지 못한 옷은 조합에 포함하지 않는다.
- 생성 가능한 후보 조합이 없으면 실패 코드를 반환한다.

후보 생성 순서는 결정 가능해야 한다. 구현 시 정렬된 `id` 오름차순 TOP, BOTTOM, OUTER 목록을 순회해 후보를 생성한다.

## 9. 점수 계산 기준
추천 총점은 100점 기준이다.

| Score | Max |
| --- | ---: |
| `weatherScore` | 35 |
| `colorScore` | 25 |
| `wearHistoryScore` | 20 |
| `recommendationHistoryScore` | 10 |
| `diversityScore` | 10 |

최종 점수는 아래 방식으로 계산한다.

```text
totalScore = weatherScore
           + colorScore
           + wearHistoryScore
           + recommendationHistoryScore
           + diversityScore
```

각 세부 점수는 0점 미만이 될 수 없고, 각 최대 점수를 넘을 수 없다.

## 10. weatherScore 규칙
`weatherScore`는 35점 만점이며, 온도 범위 적합성, OUTER 정책, 비 적합성, material 기반 날씨 보정을 포함한다.

```text
weatherScore = temperatureRangeScore
             + outerScore
             + rainScore
             + materialWeatherScore
```

각 하위 점수는 아래 기준으로 계산하고, 최종 `weatherScore`는 0점에서 35점 사이로 clamp한다.

| Component | Max | Rule |
| --- | ---: | --- |
| `temperatureRangeScore` | 15 | hard filter를 통과한 후보는 기본 15점 |
| `outerScore` | 8 | OUTER 필수/선택 규칙 만족도 |
| `rainScore` | 6 | 비 조건과 `rainSuitable` 적합도 |
| `materialWeatherScore` | 6 | 소재 기반 보온/방수/불편 가능성 보정 |

현재 규칙에서는 온도 적합성을 hard filter에서 이미 보장한다. 따라서 `temperatureRangeScore`는 hard filter를 통과한 후보에 대해 기본 15점으로 처리한다. 세밀한 체감 온도, 일교차, 습도 기반 온도 점수는 향후 MVP에서 고도화한다.

`outerScore`는 다음과 같이 계산한다.

| Condition | Candidate | Score |
| --- | --- | ---: |
| `temperature <= 12` | OUTER 포함 | 8 |
| `13 <= temperature <= 16` | OUTER 포함 | 7 |
| `13 <= temperature <= 16` | OUTER 미포함 | 5 |
| `temperature >= 17` | OUTER 미포함 | 7 |
| `temperature >= 17` | OUTER 포함 | 5 |

`rainy=true` 또는 `windy=true`이고 후보에 OUTER가 포함되어 있으면 `outerScore`에 1점을 더하되, 최대 8점을 넘지 않는다.

`rainScore`는 다음과 같이 계산한다.

| Condition | Score |
| --- | ---: |
| `rainy=false` | 6 |
| `rainy=true`이고 후보의 모든 옷이 `rainSuitable=true` | 6 |
| `rainy=true`이고 일부 옷만 `rainSuitable=true` | 3 |
| `rainy=true`이고 모든 옷이 `rainSuitable=false` | 1 |

`materialWeatherScore`는 3점에서 시작해 소재 규칙을 더하고 뺀 뒤 0점에서 6점 사이로 clamp한다.

- `temperature <= 12`에서 `KNIT` 또는 `WOOL` 소재가 포함되면 옷 1개당 +1점, 최대 +3점.
- `temperature >= 25`에서 `WOOL` 또는 `KNIT` 소재가 포함되면 옷 1개당 -2점.
- `rainy=true`에서 `NYLON` 소재가 포함되면 옷 1개당 +2점, 최대 +2점.
- `rainy=true`에서 `WOOL` 소재가 포함되면 옷 1개당 -2점.
- `UNKNOWN` material은 가산점이나 감점을 적용하지 않는다.

## 11. colorScore 규칙
`colorScore`는 25점 만점이며, 후보에 포함된 옷 색상들의 조합 안정성을 평가한다.

Color group은 다음과 같다.

| Group | Colors |
| --- | --- |
| `neutral` | `BLACK`, `WHITE`, `GRAY` |
| `blue` | `NAVY`, `BLUE` |
| `earth` | `BROWN`, `BEIGE` |
| `accent` | `RED`, `GREEN`, `YELLOW` |
| `unknown` | `UNKNOWN` |

후보에 OUTER가 없으면 TOP/BOTTOM 색상 pair만 평가한다. OUTER가 있으면 TOP/BOTTOM, TOP/OUTER, BOTTOM/OUTER pair를 모두 평가하고 평균을 반올림해 `colorScore`로 사용한다.

Pair score는 다음과 같다.

| Pair Type | Score |
| --- | ---: |
| neutral + non-neutral | 25 |
| neutral + neutral | 24 |
| blue + earth | 22 |
| same non-neutral group | 20 |
| complementary pair | 17 |
| UNKNOWN 포함 | 15 |
| strong clash | 10 |
| other mixed color | 15 |

Complementary pair는 아래 조합으로 제한한다.

- `RED` + `GREEN`
- `BLUE` + `YELLOW`
- `NAVY` + `YELLOW`

Strong clash는 아래 조합으로 제한한다.

- `RED` + `YELLOW`
- `GREEN` + `YELLOW`

같은 pair가 여러 규칙에 걸릴 경우 더 구체적인 규칙을 우선한다. 예를 들어 `UNKNOWN`이 포함되면 15점으로 처리하고 다른 color group 규칙을 적용하지 않는다.

## 12. wearHistoryScore 규칙
`wearHistoryScore`는 20점 만점이며, 실제 착용 완료 이력을 기준으로 반복 착용 부담을 줄인다.

현재 규칙에서는 구현 단순성을 위해 후보에 포함된 옷 중 하나라도 최근 착용 이력에 걸리면 가장 큰 감점 규칙을 적용한다.

| Most Recent Worn Item In Candidate | Score |
| --- | ---: |
| 최근 1일 이내 착용 | 5 |
| 최근 3일 이내 착용 | 10 |
| 최근 7일 이내 착용 | 15 |
| 착용 이력 없음 또는 7일 초과 | 20 |

최근 N일 기준은 추천 요청일을 기준으로 계산한다. 후보 내 여러 옷에 착용 이력이 있으면 가장 최근 착용된 옷을 기준으로 점수를 계산한다.

## 13. recommendationHistoryScore 규칙
`recommendationHistoryScore`는 10점 만점이며, 최근 추천된 동일 조합이나 일부 옷의 반복 노출을 줄인다. 실제 착용 이력보다 약하게 반영한다.

| Recent Recommendation History | Score |
| --- | ---: |
| 동일 조합이 최근 3일 이내 추천됨 | 2 |
| 동일 조합이 최근 7일 이내 추천됨 | 5 |
| 후보의 일부 옷이 최근 3일 이내 추천됨 | 7 |
| 후보의 일부 옷이 최근 7일 이내 추천됨 | 8 |
| 최근 추천 이력 없음 또는 7일 초과 | 10 |

동일 조합 판단은 TOP, BOTTOM, OUTER의 id 집합이 모두 같은 경우로 한다. OUTER가 없는 조합과 OUTER가 있는 조합은 서로 다른 조합으로 판단한다.

## 14. diversityScore 규칙
`diversityScore`는 10점 만점이며, 동점 또는 유사 점수 후보가 반복 추천되는 것을 줄이기 위한 보조 점수다.

현재 규칙에서는 동일 조합 반복 방지에만 사용한다.

| Recent Recommendation History | Score |
| --- | ---: |
| 동일 조합이 최근 5개 추천 결과에 없음 | 10 |
| 동일 조합이 최근 5개 추천 결과에 있음 | 0 |

동일 조합 판단은 TOP, BOTTOM, OUTER의 id 집합이 모두 같은 경우로 한다. OUTER가 없는 조합과 OUTER가 있는 조합은 서로 다른 조합이다.

색상/재질 기반 다양성은 1.5차 MVP에서 구현하지 않고 2차 MVP 후보로 이동한다.

## 15. 최종 후보 선택 규칙
후보가 여러 개일 때 아래 순서로 정렬한다.

1. `totalScore` 높은 순
2. `weatherScore` 높은 순
3. `wearHistoryScore` 높은 순
4. `recommendationHistoryScore` 높은 순
5. `colorScore` 높은 순
6. 후보 생성 순서 빠른 순

후보 생성 순서는 TOP id 오름차순, BOTTOM id 오름차순, OUTER id 오름차순 순회 기준이다. OUTER가 없는 후보는 OUTER id를 `null`로 보고, 같은 TOP/BOTTOM 조합에서는 OUTER 없는 후보를 먼저 생성한다. 이 규칙으로 같은 입력에서 항상 같은 추천 결과를 반환한다.

## 16. 추천 실패 케이스
추천 실패는 아래 우선순위로 판단한다.

1. 활성 옷 기준 TOP 또는 BOTTOM 구성이 불가능하면 `INSUFFICIENT_CLOSET_ITEMS`
2. hard filter 이후 TOP이 없으면 `NO_TOP_AVAILABLE`
3. hard filter 이후 BOTTOM이 없으면 `NO_BOTTOM_AVAILABLE`
4. OUTER 필수 조건인데 hard filter 이후 OUTER가 없으면 `OUTER_REQUIRED_BUT_NOT_AVAILABLE`
5. hard filter 이후 추천 가능한 옷이 없으면 `NO_WEATHER_SUITABLE_ITEM`
6. 위 조건을 통과했지만 조합 생성이 불가능하면 `INSUFFICIENT_CLOSET_ITEMS`

| Failure Code | When | Example Message |
| --- | --- | --- |
| `NO_TOP_AVAILABLE` | 추천 가능한 TOP이 없음 | "현재 날씨에 입을 수 있는 상의가 없습니다." |
| `NO_BOTTOM_AVAILABLE` | 추천 가능한 BOTTOM이 없음 | "현재 날씨에 입을 수 있는 하의가 없습니다." |
| `NO_WEATHER_SUITABLE_ITEM` | 날씨 hard filter 이후 남은 옷이 없음 | "현재 기온에 맞는 옷이 없습니다." |
| `OUTER_REQUIRED_BUT_NOT_AVAILABLE` | OUTER 필수 날씨지만 추천 가능한 OUTER가 없음 | "현재 기온에는 아우터가 필요하지만 추천 가능한 아우터가 없습니다." |
| `INSUFFICIENT_CLOSET_ITEMS` | 활성 옷 자체가 너무 적거나 조합 생성이 불가능함 | "추천을 만들기 위해 옷을 더 등록해주세요." |

실패 응답에는 실패 코드와 사용자 메시지를 포함한다. 임의 조합을 만들어 성공 응답으로 반환하지 않는다.

## 17. 추천 이유 생성 규칙
`RecommendationReasonGenerator`는 점수 규칙 결과를 템플릿 문장으로 변환한다. 한 추천 결과에는 3개 이상 5개 이하의 추천 이유를 포함한다.

추천 이유는 아래 우선순위로 선택한다.

1. 날씨/OUTER 이유
2. 색상 조합 이유
3. 최근 착용 또는 최근 추천 이력 이유
4. material 기반 이유
5. 다양성 이유

### 날씨 기반 이유
- OUTER 필수 조건 충족: "현재 기온이 낮아 아우터를 포함한 조합을 추천했습니다."
- 온도 범위 적합: "선택된 옷들이 현재 기온에 맞는 온도 범위에 있습니다."
- 비 적합성 긍정: "비가 오는 조건에서도 착용하기 좋은 옷이 포함되어 있습니다."
- 비 적합성 감점: "비에 적합하지 않은 옷이 포함되어 날씨 점수가 일부 낮아졌습니다."

### 색상 조합 이유
- 무채색 중심: "상의와 하의 색상이 무채색 중심이라 안정적인 조합입니다."
- 무채색 + 유채색: "무채색과 포인트 색상이 함께 있어 균형 잡힌 조합입니다."
- 유사 계열: "비슷한 색상 계열이어서 자연스럽게 이어지는 조합입니다."
- 보색 조합: "대비가 있는 색상 조합이라 포인트가 분명합니다."
- 충돌 조합: "색상 대비가 강해 색상 점수가 낮게 반영되었습니다."
- UNKNOWN 포함: "색상 정보가 부족해 색상 점수는 중립으로 반영했습니다."

### 최근 착용 이력 이유
- 최근 착용 적음: "최근 착용 이력이 적어 반복 착용 부담이 낮습니다."
- 최근 7일 이내 착용: "최근 7일 이내 착용한 옷이 포함되어 착용 이력 점수가 일부 낮아졌습니다."
- 최근 3일 이내 착용: "최근 3일 이내 착용한 옷이 포함되어 반복 착용 부담을 반영했습니다."
- 최근 1일 이내 착용: "어제 또는 오늘 착용한 옷이 포함되어 착용 이력 점수가 크게 낮아졌습니다."

### OUTER 포함/제외 이유
- 필수 포함: "현재 기온이 낮아 아우터를 포함한 조합만 후보로 평가했습니다."
- 선택 포함: "선선한 날씨라 아우터 포함 조합에 날씨 점수를 더했습니다."
- 더운 날 제외: "기온이 높아 아우터가 없는 조합을 더 자연스럽게 평가했습니다."
- 비/바람 보정: "비나 바람 조건을 고려해 아우터 포함 조합에 날씨 점수를 더했습니다."

### material 기반 이유
- 추운 날 KNIT/WOOL: "니트 또는 울 소재가 현재 기온에 적합해 보온성을 보완합니다."
- 더운 날 KNIT/WOOL: "더운 날씨에는 니트 또는 울 소재가 부담스러울 수 있어 날씨 점수가 낮아졌습니다."
- 비 오는 날 NYLON: "나일론 소재는 비 오는 날 착용에 유리해 날씨 점수에 긍정적으로 반영되었습니다."
- 비 오는 날 WOOL: "비 오는 날 울 소재는 젖었을 때 불편할 수 있어 날씨 점수가 낮아졌습니다."
- UNKNOWN material: "소재 정보가 부족해 소재 기반 보정은 적용하지 않았습니다."

### 다양성 이유
- 동일 조합 반복 없음: "최근 추천된 동일 조합이 아니어서 반복 추천 부담이 낮습니다."
- 동일 조합 반복 있음: "최근 추천된 동일 조합이라 다양성 점수는 낮게 반영되었습니다."

## 18. 테스트 케이스 목록
추천 규칙 구현 시 아래 테스트를 작성한다.

- `archived=true`인 옷은 추천 후보에서 제외된다.
- 현재 기온이 `minTemperature`보다 낮은 옷은 제외된다.
- 현재 기온이 `maxTemperature`보다 높은 옷은 제외된다.
- TOP이 없으면 `NO_TOP_AVAILABLE`을 반환한다.
- BOTTOM이 없으면 `NO_BOTTOM_AVAILABLE`을 반환한다.
- OUTER 필수 조건에서 OUTER가 없으면 `OUTER_REQUIRED_BUT_NOT_AVAILABLE`을 반환한다.
- 활성 옷이 2개 미만이면 `INSUFFICIENT_CLOSET_ITEMS`를 반환한다.
- 모든 옷이 온도 hard filter에서 제외되면 `NO_WEATHER_SUITABLE_ITEM`을 반환한다.
- `temperature=12`, `CLOUDY`, `rainy=false`, `windy=false`에서 OUTER 필수 조합만 생성된다.
- `weatherScore`가 온도, OUTER, rainSuitable, material 보정을 반영한다.
- `colorScore`가 neutral, blue, earth, accent, UNKNOWN 조합을 규칙대로 계산한다.
- `material=UNKNOWN`일 때 material 기반 weather 보정이 적용되지 않는다.
- 추운 날 `KNIT` 또는 `WOOL` 소재가 weatherScore에 가산된다.
- 더운 날 `KNIT` 또는 `WOOL` 소재가 weatherScore에서 감점된다.
- 비 오는 날 `NYLON` 소재가 weatherScore에 가산된다.
- 비 오는 날 `WOOL` 소재가 weatherScore에서 감점된다.
- 최근 1일, 3일, 7일 착용 이력에 따라 `wearHistoryScore`가 감점된다.
- 동일 조합 또는 일부 옷의 최근 추천 이력에 따라 `recommendationHistoryScore`가 감점된다.
- 동일 조합이 최근 5개 추천 결과에 없으면 `diversityScore`가 10점이다.
- 동일 조합이 최근 5개 추천 결과에 있으면 `diversityScore`가 0점이다.
- 추천 이유가 3개 이상 5개 이하로 생성된다.
- 추천 이유가 날씨/OUTER, 색상, 이력, material, 다양성 우선순위를 따른다.
- 동일 seed data와 동일 `WeatherCondition` 입력에서 동일 추천 결과를 반환한다.

## 정합성 메모
- PRD와 ARCHITECTURE와 충돌하는 내용은 없다.
- 추천 생성 API 계약은 `POST /api/recommendations?userId={userId}`를 기준으로 한다.
- 색상별 세부 선호도나 계절성 개인화는 1.5차 MVP 범위에서 제외한다.
- `RecommendationResult`의 물리 DB 저장 구조는 `docs/ERD.md`를 따른다.

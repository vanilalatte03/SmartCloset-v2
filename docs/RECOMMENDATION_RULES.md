# 추천 규칙: SmartCloset MVP5

## 1. 추천 규칙의 목적

SmartCloset MVP5의 추천은 AI/GPT 추천이 아니라 설명 가능하고 테스트 가능한 규칙 기반 추천이다.

MVP5는 옷 이미지 업로드와 썸네일 표시를 추가하지만 추천 후보 필터링, 점수 계산, tie-break, 추천 이유 생성 규칙은 변경하지 않는다.

## MVP5 결정

- 이미지 존재 여부는 추천 점수에 영향을 주지 않는다.
- 이미지 존재 여부는 추천 후보 생성과 필터링에 영향을 주지 않는다.
- 이미지 존재 여부는 추천 이유에 포함하지 않는다.
- `styleTags`는 계속 저장/조회/표시만 하며 점수와 이유에는 반영하지 않는다.
- 추천 생성 API는 `POST /api/recommendations`만 사용한다.
- today 추천 GET 경로는 API 계약으로 사용하지 않는다.
- 현재 날씨 요약 API `GET /api/weather/current`는 추천 결과, 추천 이력, 착용 이력, 점수 계산을 만들거나 변경하지 않는다.

추천 응답 DTO에는 옷 이미지 metadata가 포함될 수 있지만, 이는 표시용 데이터다.

## 2. 입력 데이터

추천 로직은 아래 데이터를 입력으로 사용한다.

- 현재 인증 사용자 id
- 현재 인증 사용자의 활성 옷 목록
- 현재 인증 사용자의 위치 snapshot
- 현재 인증 사용자의 최근 착용 이력
- 현재 인증 사용자의 최근 추천 이력
- 현재 인증 사용자의 선호 색상/소재
- 내부 `WeatherCondition`

추천 로직은 아래 데이터를 입력으로 사용하지 않는다.

- 옷 이미지 존재 여부
- 옷 이미지 MIME type
- 옷 이미지 크기
- 옷 이미지 업로드 시각
- `styleTags`

## 3. Weather source 정책

추천 도메인은 외부 API 응답 모델에 직접 의존하지 않는다. 추천 규칙은 항상 내부 `WeatherCondition`만 입력으로 받는다.

현재 기본 weather source는 인증 사용자 위치 `nx`, `ny`로 조회한 기상청 단기예보 조회서비스 `getVilageFcst` JSON 응답이다. 서비스키 미설정, 외부 API 실패, `NODATA`, 필수 category 누락, 파싱 실패 시에는 `StaticWeatherProvider` fallback 날씨를 사용한다.

fallback 값은 유지한다.

| Field | Value |
| --- | --- |
| `temperature` | `12` |
| `weatherType` | `CLOUDY` |
| `rainy` | `false` |
| `windy` | `false` |

## 4. WeatherCondition 필드

- `temperature`: 현재 기온. 정수 섭씨 기준
- `weatherType`: 대표 날씨 타입
- `rainy`: 비 또는 눈처럼 젖는 조건이 있는지 여부
- `windy`: 바람이 강한지 여부

`weatherType` enum:

- `SUNNY`
- `CLOUDY`
- `RAINY`
- `SNOWY`
- `WINDY`

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

MVP5에서 `ClothingItem`은 이미지 메타데이터도 가질 수 있지만 추천 계산에는 사용하지 않는다.

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

`preferredColors`와 `preferredMaterials`만 `preferenceScore`에 반영한다. `styleTags`는 저장/조회/표시만 하며 `preferenceScore`와 추천 이유에는 반영하지 않는다.

## 7. 후보 필터링 규칙

후보 필터링은 조합 생성 전에 수행하는 hard filter다.

- `archived=true`인 옷은 제외한다.
- 현재 `temperature`가 `minTemperature`보다 낮으면 제외한다.
- 현재 `temperature`가 `maxTemperature`보다 높으면 제외한다.
- hard filter 이후 TOP이 1개도 없으면 `NO_TOP_AVAILABLE` 실패로 처리한다.
- hard filter 이후 BOTTOM이 1개도 없으면 `NO_BOTTOM_AVAILABLE` 실패로 처리한다.
- OUTER 필수 조건에서 hard filter를 통과한 OUTER가 1개도 없으면 `OUTER_REQUIRED_BUT_NOT_AVAILABLE` 실패로 처리한다.

이미지가 없는 옷도 필터링에서 제외하지 않는다.

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

이미지 관련 필드는 이 계산에 포함하지 않는다.

## 11. preferenceScore

- 선호 색상 배열과 선호 소재 배열이 모두 비어 있으면 `preferenceScore=0`이다.
- 후보 옷 중 하나 이상이 `preferredColors`와 일치하면 5점이다.
- 후보 옷 중 하나 이상이 `preferredMaterials`와 일치하면 5점이다.
- 둘 다 일치하면 10점이다.
- `styleTags`는 반영하지 않는다.

## 12. 추천 이력과 이미지 표시

추천 이력은 기존처럼 `recommendation_result_items`가 `clothing_items`를 참조한다.

MVP5에서는 추천 이력 DTO에 포함되는 outfit item이 현재 `clothing_items`의 image metadata를 함께 노출한다. 과거 추천 당시 이미지 snapshot은 별도로 저장하지 않는다.

따라서 추천 후 옷 이미지를 교체하거나 삭제하면 과거 추천 이력 화면에도 최신 이미지 상태가 표시된다.

## 13. 테스트 기대사항

MVP5 구현 후 추천 규칙 테스트는 아래를 증명해야 한다.

- 이미지가 없는 옷도 기존과 동일하게 추천 후보가 된다.
- 이미지가 있는 옷도 score가 달라지지 않는다.
- 이미지 metadata는 추천 이유에 포함되지 않는다.
- `styleTags`는 계속 score와 reason에 영향을 주지 않는다.
- 기존 deterministic recommendation test는 유지된다.

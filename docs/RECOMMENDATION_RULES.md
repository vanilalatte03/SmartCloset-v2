# 추천 규칙: SmartCloset Current Baseline

## 1. 추천 규칙의 목적
SmartCloset 현재 baseline의 추천은 AI/GPT 추천이 아니라 설명 가능하고 테스트 가능한 규칙 기반 추천이다.

현재 baseline은 인증 사용자 기준으로 옷장, 위치, 추천 이력, 착용 이력, 선호도를 분리한다. 추천 생성 API는 `POST /api/recommendations`이며, today 추천 GET 경로는 API 계약으로 사용하지 않는다.

## MVP4 작성 메모
MVP4 추천 규칙 변경은 아직 확정되지 않았다. 점수 배분, tie-break, 추천 이유, 실패 코드, styleTags 반영 여부는 `docs/PRD.md`와 ADR에서 승인된 뒤 이 문서에 반영한다.

추천 이유는 AI가 생성하는 자유 문장이 아니다. 각 규칙의 판단 결과를 `RecommendationReasonGenerator`가 템플릿 문장으로 변환한다.

## 2. 입력 데이터
추천 로직은 아래 데이터를 입력으로 사용한다.

- 현재 인증 사용자 id
- 현재 인증 사용자의 활성 옷 목록
- 현재 인증 사용자의 위치 snapshot
- 현재 인증 사용자의 최근 착용 이력
- 현재 인증 사용자의 최근 추천 이력
- 현재 인증 사용자의 선호 색상/소재
- 내부 `WeatherCondition`

`styleTags`는 현재 baseline에서 저장/조회/표시만 한다. 추천 점수와 추천 이유에는 반영하지 않는다.

## 3. Weather source 정책
추천 도메인은 외부 API 응답 모델에 직접 의존하지 않는다. 추천 규칙은 항상 내부 `WeatherCondition`만 입력으로 받는다.

현재 기본 weather source는 인증 사용자 위치 `nx`, `ny`로 조회한 기상청 단기예보 조회서비스 `getVilageFcst` JSON 응답이다. 서비스키 미설정, 외부 API 실패, `NODATA`, 필수 category 누락, 파싱 실패 시에는 `StaticWeatherProvider` fallback 날씨를 사용한다.

KMA forecast group 선택은 provider 책임이다. provider는 현재 KST 이후 가장 가까운 `fcstDate`, `fcstTime` group을 선택해 `WeatherCondition`을 만든다. 선택 group에 필수 category가 누락되면 다른 forecast group으로 이동하지 않고 fallback 또는 strict mode 실패로 처리한다.

fallback 값은 유지한다.

| Field | Value |
| --- | --- |
| `temperature` | `12` |
| `weatherType` | `CLOUDY` |
| `rainy` | `false` |
| `windy` | `false` |

## 4. WeatherCondition 필드
`WeatherCondition`은 추천 로직에서 사용하는 내부 날씨 모델이다. KMA 응답 DTO와 분리한다.

- `temperature`: 현재 기온. 정수 섭씨 기준
- `weatherType`: 대표 날씨 타입
- `rainy`: 비 또는 눈처럼 젖는 조건이 있는지 여부
- `windy`: 바람이 강한지 여부

`weatherType` enum은 아래 값으로 제한한다.

- `SUNNY`
- `CLOUDY`
- `RAINY`
- `SNOWY`
- `WINDY`

## 5. KMA category 매핑
KMA `getVilageFcst` JSON 응답의 `response.body.items.item[]`에서 같은 forecast time의 category를 묶어 사용한다.

현재 추천에 필요한 category는 아래 5개다.

| Category | Name | WeatherCondition field |
| --- | --- | --- |
| `TMP` | 1시간 기온 | `temperature` |
| `PTY` | 강수형태 | `weatherType`, `rainy` |
| `SKY` | 하늘상태 | `weatherType` |
| `PCP` | 1시간 강수량 | `rainy` |
| `WSD` | 풍속 | `windy` |

Weather type:

| KMA value | WeatherType |
| --- | --- |
| `PTY=1`, `PTY=2`, `PTY=4` | `RAINY` |
| `PTY=3` | `SNOWY` |
| `PTY=0`, `SKY=1` | `SUNNY` |
| `PTY=0`, `SKY=3` 또는 `SKY=4` | `CLOUDY` |

`PCP`가 `-`, `null`, `0`, `강수없음`이면 강수 없음으로 본다. `WSD >= 4.0`이면 `windy=true`로 본다.

## 6. ClothingItem 최소 속성
추천 규칙에서 사용하는 `ClothingItem` 최소 속성은 다음과 같다.

- `id`
- `name`
- `category`
- `color`
- `material`
- `minTemperature`
- `maxTemperature`
- `rainSuitable`
- `archived`

현재 사용자 전용 API 응답에는 `userId`를 노출하지 않는다. 내부 Entity와 Repository는 소유자 검증을 위해 사용자 id를 계속 가진다.

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

## 7. 사용자 선호도 입력
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

신규 사용자의 기본값은 모두 빈 배열이다.

```json
{
  "preferredColors": [],
  "preferredMaterials": [],
  "styleTags": []
}
```

`preferredColors`와 `preferredMaterials`만 `preferenceScore`에 반영한다. `styleTags`는 저장/조회/표시만 하며 `preferenceScore`와 추천 이유에는 반영하지 않는다.

## 8. 후보 필터링 규칙
후보 필터링은 조합 생성 전에 수행하는 hard filter다. hard filter를 통과하지 못한 옷은 어떤 조합에도 포함하지 않는다.

- `archived=true`인 옷은 제외한다.
- 현재 `temperature`가 `minTemperature`보다 낮으면 제외한다.
- 현재 `temperature`가 `maxTemperature`보다 높으면 제외한다.
- hard filter 이후 TOP이 1개도 없으면 `NO_TOP_AVAILABLE` 실패로 처리한다.
- hard filter 이후 BOTTOM이 1개도 없으면 `NO_BOTTOM_AVAILABLE` 실패로 처리한다.
- OUTER 필수 조건에서 hard filter를 통과한 OUTER가 1개도 없으면 `OUTER_REQUIRED_BUT_NOT_AVAILABLE` 실패로 처리한다.

`rainSuitable`과 `material`은 hard filter가 아니라 `weatherScore` 보정에 사용한다.

## 9. OUTER 필수/선택 규칙
OUTER 포함 기준은 현재 `temperature`, `rainy`, `windy`를 기준으로 판단한다.

| Condition | Rule |
| --- | --- |
| `temperature <= 12` | OUTER 필수 |
| `13 <= temperature <= 16` | OUTER 선택, 포함 시 weatherScore 가산 |
| `temperature >= 17` | OUTER optional, 미포함을 기본적으로 더 자연스럽게 평가 |
| `rainy=true` | OUTER 포함 조합에 weatherScore 가산 |
| `windy=true` | OUTER 포함 조합에 weatherScore 가산 |

`rainy=true` 또는 `windy=true`여도 `temperature >= 17`이면 OUTER를 필수로 만들지 않는다.

## 10. 후보 조합 생성 규칙
`OutfitCandidate`는 추천 계산 중 생성되는 도메인 모델 또는 value object다. DB Entity로 만들 필요는 없다.

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

최종 점수는 아래 방식으로 계산한다.

```text
totalScore = weatherScore
           + colorScore
           + wearHistoryScore
           + recommendationHistoryScore
           + preferenceScore
```

각 세부 점수는 0점 미만이 될 수 없고, 각 최대 점수를 넘을 수 없다.

## 12. weatherScore 규칙
`weatherScore`는 35점 만점이며, 온도 범위 적합성, OUTER 정책, 비 적합성, material 기반 날씨 보정을 포함한다.

| Component | Max | Rule |
| --- | ---: | --- |
| `temperatureRangeScore` | 15 | hard filter를 통과한 후보는 기본 15점 |
| `outerScore` | 8 | OUTER 필수/선택 규칙 만족도 |
| `rainScore` | 6 | 비 조건과 `rainSuitable` 적합도 |
| `materialWeatherScore` | 6 | 소재 기반 보온/방수/불편 가능성 보정 |

최종 `weatherScore`는 0점에서 35점 사이로 clamp한다.

## 13. colorScore 규칙
`colorScore`는 25점 만점이며 TOP/BOTTOM/OUTER 색상 조합을 평가한다.

권장 기준:

- 무채색 조합은 안정적인 기본 점수를 받는다.
- `NAVY`, `BLUE`, `BEIGE`, `BROWN` 등 자연스러운 조합은 가산한다.
- 강한 색상이 여러 개 섞이면 감점할 수 있다.
- `UNKNOWN`은 중립 처리한다.

색상 점수는 결정 가능해야 하며 동일 입력에서 동일 점수를 반환해야 한다.

## 14. wearHistoryScore 규칙
`wearHistoryScore`는 20점 만점이며 최근 착용 이력이 적은 옷을 우대한다.

- 최근 착용 이력이 없는 후보는 높은 점수를 받는다.
- 최근 착용한 옷이 포함된 후보는 감점한다.
- 착용 이력 조회는 현재 인증 사용자 기준으로만 수행한다.

## 15. recommendationHistoryScore 규칙
`recommendationHistoryScore`는 10점 만점이며 최근 추천 반복을 줄인다.

- 최근 추천 이력에 같은 조합이 없으면 높은 점수를 받는다.
- 최근 추천 이력에 같은 조합이 있으면 감점한다.
- 추천 이력 조회는 현재 인증 사용자 기준으로만 수행한다.

## 16. preferenceScore 규칙
`preferenceScore`는 10점 만점이며 현재 인증 사용자의 선호 색상/소재만 반영한다.

계산 규칙:

- 선호 색상/소재가 모두 비어 있으면 0점
- 추천 후보 옷 중 `preferredColors`와 일치하는 색상이 하나 이상 있으면 5점
- 추천 후보 옷 중 `preferredMaterials`와 일치하는 소재가 하나 이상 있으면 5점
- 색상과 소재 조건을 모두 만족하면 10점
- `styleTags`는 점수에 반영하지 않는다.

예시:

| preferredColors | preferredMaterials | Candidate | preferenceScore |
| --- | --- | --- | ---: |
| `[]` | `[]` | any | 0 |
| `[NAVY]` | `[]` | NAVY outer 포함 | 5 |
| `[]` | `[COTTON]` | COTTON top 포함 | 5 |
| `[BLACK]` | `[DENIM]` | BLACK bottom, DENIM bottom 포함 | 10 |

## 17. 추천 실패 코드
추천 실패는 비즈니스 실패이므로 HTTP `422 Unprocessable Entity`로 응답한다.

| Code | Condition |
| --- | --- |
| `NO_TOP_AVAILABLE` | hard filter 이후 TOP 없음 |
| `NO_BOTTOM_AVAILABLE` | hard filter 이후 BOTTOM 없음 |
| `NO_WEATHER_SUITABLE_ITEM` | 날씨 hard filter 이후 추천 가능한 옷 없음 |
| `OUTER_REQUIRED_BUT_NOT_AVAILABLE` | OUTER 필수 조건에서 OUTER 없음 |
| `INSUFFICIENT_CLOSET_ITEMS` | 추천 조합 생성을 위한 최소 옷 수 부족 |

## 18. 추천 이유 생성
추천 이유는 3개 이상 5개 이하로 생성한다.

포함 가능한 이유:

- 현재 기온과 날씨 조건에 맞는 옷이라는 설명
- OUTER 포함/미포함 판단 설명
- 색상 조합 설명
- 최근 착용 이력 또는 추천 이력 설명
- 선호 색상 또는 선호 소재와 맞는 옷이 포함되었다는 설명

포함하지 않는 이유:

- AI/GPT 판단처럼 보이는 자유 문장
- `styleTags`와 맞는다는 설명
- KMA 원본 category를 직접 언급하는 설명

## 19. 최종 후보 선택과 tie-break
최종 후보는 `totalScore`가 가장 높은 후보를 선택한다.

동점이면 아래 순서로 결정한다.

1. `weatherScore` 높은 후보
2. `preferenceScore` 높은 후보
3. `colorScore` 높은 후보
4. `wearHistoryScore` 높은 후보
5. `recommendationHistoryScore` 높은 후보
6. TOP id 낮은 후보
7. BOTTOM id 낮은 후보
8. OUTER가 둘 다 있으면 OUTER id 낮은 후보
9. 한쪽만 OUTER가 있으면 날씨 정책상 더 자연스러운 후보

tie-break는 결정 가능해야 하며 동일 입력에서 동일 추천 결과가 나와야 한다.

## 20. 추천 이력 조회
추천 이력 조회 API는 현재 인증 사용자 기준이다.

```text
GET /api/recommendations?limit={limit}
```

Limit 정책:

- 기본값 `20`
- 최소 `1`
- 최대 `50`
- 최신순 정렬
- 범위 밖 또는 숫자가 아닌 값은 `400 INVALID_REQUEST`

## 21. 유지되는 비범위
- AI/GPT 추천
- 이미지 기반 자동 태깅
- styleTags 기반 개인화 고도화
- 선호도 별도 테이블 정규화
- Weather source DB 저장
- 추천 결과 위치 source snapshot 저장
- today 추천 GET 경로

# 추천 규칙: SmartCloset MVP10

## 문서 목적

SmartCloset MVP10의 추천은 AI/GPT 추천이 아니라 설명 가능하고 테스트 가능한 규칙 기반 추천이다.

MVP10은 사진 기반 AI 옷 등록 보조 MVP이며 추천 점수, 후보 생성, tie-break를 변경하지 않는다. MVP6의 상황, 옷별 `styleTags`, 최근 추천 피드백 기반 `preferenceScore`, MVP7의 `forecastPeriod`와 위치/날씨 source snapshot, MVP8 계정 안정성 계약, MVP9 UI/UX 기준을 그대로 유지한다. 추천 이유는 기존 template 기반 원칙을 유지한다.

## MVP10 결정

- 총점은 100점이며 기존 score response field를 유지한다.
- `weatherScore` 최대값은 35점이다.
- `colorScore` 최대값은 25점이다.
- `wearHistoryScore` 최대값은 20점이다.
- `recommendationHistoryScore` 최대값은 10점이다.
- `preferenceScore` 최대값은 10점이다.
- 추천 상황은 `WORK`, `CASUAL`, `WORKOUT`, `DATE`, `FORMAL`이다.
- 예보 시간대는 `CURRENT`, `MORNING`, `AFTERNOON`, `EVENING`이다.
- `forecastPeriod`는 weather input 선택에만 관여하며 score field를 새로 만들지 않는다.
- 위치/source snapshot은 추천 근거 표시와 이력 신뢰도에만 사용한다.
- MVP8 계정 기능, MVP9 UI/UX 변경, MVP10 AI 옷 등록 보조는 추천 후보 필터링과 tie-break에 영향을 주지 않는다.
- Image metadata는 추천 점수, 후보 필터링, tie-break, 추천 이유에 사용하지 않는다.
- AI 분석 결과, confidence, reviewRequiredFields는 추천 입력으로 사용하지 않는다.
- Recommendation reason은 template 기반이며 AI-generated가 아니다.

## 추천 입력

추천 생성 API:

```http
POST /api/recommendations
Authorization: Bearer {accessToken}
Content-Type: application/json
```

Request body는 선택이다.

```json
{
  "situation": "WORK",
  "forecastPeriod": "AFTERNOON"
}
```

기본값:

- body가 없거나 `situation`이 누락되면 `CASUAL`
- body가 없거나 `forecastPeriod`가 누락되면 `CURRENT`

## 점수 체계

| Field | Max | 설명 |
| --- | ---: | --- |
| `weatherScore` | 35 | 현재 weather condition과 옷의 온도/비 적합성 |
| `colorScore` | 25 | 색상 선호와 조합 규칙 |
| `wearHistoryScore` | 20 | 최근 착용 이력 |
| `recommendationHistoryScore` | 10 | 최근 추천 중복 방지 |
| `preferenceScore` | 10 | 선호 색상/소재/styleTags와 최근 피드백 |

총점은 각 field 합산이며 100점을 넘지 않는다.

## Weather와 ForecastPeriod

- Weather provider는 사용자의 저장 위치를 기준으로 weather input을 만든다.
- `ForecastPeriod`는 어떤 예보 시각의 weather input을 사용할지 결정한다.
- KMA 실패 또는 서비스키 미설정 시 fallback weather를 사용할 수 있다.
- Fallback weather는 `temperature=12`, `weatherType=CLOUDY`, `rainy=false`, `windy=false`다.
- Raw KMA 응답 JSON은 추천 domain, DB, API response에 저장하거나 노출하지 않는다.
- 아우터 정책은 다음 경계를 따른다.
  - `12°C` 이하는 아우터가 필수이며 추천 가능한 아우터가 없으면 실패한다.
  - `13°C..18°C`는 아우터가 필수는 아니지만, 추천 가능한 아우터가 있으면 weather score와 tie-break에서 아우터 포함 후보를 선호한다.
  - `19°C` 이상은 가벼운 상하의-only 후보를 선호하되, 각 옷의 온도 범위를 통과한 아우터 후보도 점수 경쟁에는 참여할 수 있다.

## 위치/날씨 source snapshot

추천 결과에는 추천 생성 당시 source snapshot을 저장한다.

- location code/name/fullName
- location nx/ny
- location source
- weather provider
- KMA 사용 여부
- fallback 사용 여부
- KMA base date/time
- forecast date/time

Source snapshot은 score field가 아니다. 사용자 위치가 나중에 바뀌어도 과거 추천 이력의 snapshot은 바뀌지 않는다.

## PreferenceScore

`preferenceScore`는 최대 10점이다.

- 선호 색상 일치: 0 또는 2
- 선호 소재 일치: 0 또는 2
- 사용자 선호 styleTags와 옷별 styleTags 일치: 0..3
- 최근 14일 추천 피드백 보정: -3..3
- 최종 `preferenceScore`는 0..10으로 clamp한다.

Style tag 비교:

- 저장 전 trim한다.
- blank tag는 저장하지 않는다.
- ASCII는 case-insensitive로 비교한다.

## 추천 이유

추천 이유는 template 기반 문구로 생성한다. 날씨 적합 이유는 온도대, 비 여부, 아우터 포함 여부를 반영하며, 같은 입력에는 같은 문구를 반환한다. 랜덤이나 AI/GPT는 사용하지 않는다.

- 날씨 적합 이유
- 선호 색상/소재/styleTags 이유
- 상황 styleTags 이유
- 최근 피드백 반영 이유
- 착용/추천 이력에 따른 다양성 이유
- 위치/날씨 source는 별도 trust display로 표시하고 추천 점수 이유로 섞지 않는다.

추천 이유는 최소 3개, 최대 5개를 목표로 한다.

## 이력과 피드백

- 추천 이력은 `GET /api/recommendations?limit={limit}`로 조회한다.
- 기본 `limit=20`, 최소 1, 최대 50이다.
- 최신순으로 정렬한다.
- 착용 완료는 `PATCH /api/recommendations/{recommendationId}/worn`이며 idempotent하다.
- 추천 피드백은 `PUT /api/recommendations/{recommendationId}/feedback`이다.
- 피드백 PUT은 전체 교체이며 누락 필드는 `null`로 간주한다.
- `sentiment`와 `thermal`이 모두 `null`이면 clear다.

## AI 옷 등록 보조와 추천의 분리

MVP10의 `POST /api/clothes/analyze-image` 응답은 옷 등록 form 후보 제안에만 사용한다.

- AI 분석 결과는 recommendation request/response field가 아니다.
- AI 분석 결과는 추천 이력 snapshot으로 저장하지 않는다.
- AI confidence는 추천 점수 field가 아니다.
- 추천은 사용자가 최종 저장한 옷 속성만 읽는다.
- 사용자가 AI 후보를 수정하거나 확인한 뒤 저장하면, 추천 엔진은 그 저장된 옷 속성을 기존 규칙대로 처리한다.

## 테스트 기준

MVP10 구현 후 추천 규칙 검증은 아래를 증명해야 한다.

- MVP8 계정 기능, MVP9 UI/UX 변경, MVP10 AI 옷 등록 보조 후에도 추천 생성 기본값 `CASUAL`, `CURRENT`가 유지된다.
- 총점 100점 체계와 세부 score field가 유지된다.
- 위치/source snapshot은 점수 field를 추가하지 않는다.
- 사용자 위치 변경 후 과거 추천 snapshot은 바뀌지 않는다.
- 이미지 metadata는 점수, 후보 필터링, tie-break, 추천 이유에 영향을 주지 않는다.
- AI 분석 결과, confidence, reviewRequiredFields는 점수, 후보 필터링, tie-break, 추천 이유에 영향을 주지 않는다.
- 계정 삭제 구현 후 삭제된 사용자 추천 이력은 더 이상 조회되지 않는다.

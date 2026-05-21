# Use KMA Vilage Forecast Weather Provider

## Status
Accepted

## Context
SmartCloset 1차 MVP는 추천 도메인, API 계약, Docker Compose 공유 흐름을 검증하기 위해 `StaticWeatherProvider`만 사용했다.

1.5차 MVP에서는 실제 날씨 기반 추천을 확인해야 한다. 사용자가 제공한 공식 기준은 공공데이터포털의 기상청_단기예보 조회서비스와 첨부 활용가이드/격자 위경도 파일이다. 해당 OpenAPI는 REST 방식이며 JSON+XML을 지원하고, 단기예보 상세 기능 `getVilageFcst`는 `base_date`, `base_time`, `nx`, `ny` 기준으로 예보 item을 반환한다.

다만 외부 API는 API key, 네트워크, 제공 시간, `NODATA`, 응답 category 누락, 트래픽 제한 문제를 만들 수 있다. 데모와 Docker Compose 공유 흐름은 서비스키 없이도 재현 가능해야 한다.

## Decision
1.5차 MVP의 기본 weather source는 기상청 단기예보 조회서비스 `getVilageFcst` JSON 응답으로 한다.

요청 기준:

- `KMA_BASE_URL=http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0`
- path: `/getVilageFcst`
- `serviceKey=${KMA_SERVICE_KEY}`
- `pageNo=1`
- `numOfRows=1000`
- `dataType=JSON`
- `base_date`: 제공 가능한 최신 단기예보 발표일자
- `base_time`: `0200`, `0500`, `0800`, `1100`, `1400`, `1700`, `2000`, `2300` 중 제공 가능한 최신 발표시각
- `nx=${KMA_NX}`, 기본 `60`
- `ny=${KMA_NY}`, 기본 `127`

`base_date`, `base_time`은 현재 KST 기준 각 발표시각 10분 이후부터 제공 가능하다고 보고 최신 발표분을 선택한다.

추천에 사용할 forecast target time은 현재 KST 이후 가장 가까운 예보시각으로 확정한다. KMA 응답의 `fcstDate`, `fcstTime` group을 오름차순으로 정렬하고 요청 시각 이후 첫 group을 선택한다. 선택 group에 `TMP`, `SKY`, `PTY`, `PCP`, `WSD` 중 하나라도 없으면 다른 group으로 이동하지 않고 실패로 처리한다.

기본 격자는 첨부 격자 위경도 XLSX의 서울특별시 대표 행을 사용한다.

| Location | nx | ny |
| --- | ---: | ---: |
| 서울특별시 | `60` | `127` |

KMA category 매핑:

| Category | Internal field |
| --- | --- |
| `TMP` | `temperature` |
| `PTY` | `weatherType`, `rainy` |
| `SKY` | `weatherType` |
| `PCP` | `rainy` |
| `WSD` | `windy` |

`POP`, `REH`, `TMN`, `TMX` 등은 1.5차 추천 점수에 사용하지 않는다.

Spring bean 구성은 `KmaVilageForecastWeatherProvider`를 `@Primary` `WeatherProvider` bean으로 두고, fallback에는 concrete `StaticWeatherProvider`를 주입하는 방식으로 고정한다.

`WEATHER_FALLBACK_ENABLED=true`에서는 아래 상황에서 `StaticWeatherProvider` fallback 값을 사용한다.

- `KMA_SERVICE_KEY` 미설정
- KMA HTTP 호출 실패
- KMA `resultCode`가 `00`이 아님
- `NODATA_ERROR`
- `items.item` 비어 있음
- 필수 category 누락
- 값 파싱 실패

`WEATHER_FALLBACK_ENABLED=false`는 strict KMA mode다. strict mode에서는 위 상황에서 fallback하지 않고 `INTERNAL_SERVER_ERROR`로 실패하며, 추천 실패 코드 5종으로 변환하지 않고 `RecommendationResult`를 저장하지 않는다.

fallback 값은 1차 MVP와 동일하다.

- `temperature=12`
- `weatherType=CLOUDY`
- `rainy=false`
- `windy=false`

추천 API 계약은 변경하지 않는다. 추천 생성은 계속 `POST /api/recommendations?userId={userId}`만 사용한다.

## Consequences
- 실제 날씨 기반 추천을 확인할 수 있다.
- 추천 도메인은 계속 내부 `WeatherCondition`에만 의존한다.
- API key가 없어도 Docker Compose 공유와 데모 흐름이 유지된다.
- KMA provider와 Static provider가 함께 있어도 `WeatherProvider` bean 충돌을 피한다.
- strict KMA mode에서 외부 API 실패를 명시적인 서버 오류로 확인할 수 있다.
- 1.5차에서는 사용자별 위치 저장, weather source DB 저장, 최근 성공 날씨 캐시를 구현하지 않는다.
- KMA 응답 모델 변경이나 장애는 provider/fallback 계층에서 흡수한다.

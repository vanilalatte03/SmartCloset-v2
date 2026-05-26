# MVP7을 위치/날씨 신뢰도 MVP로 정의

## 상태
승인됨

## 맥락

MVP6에서는 추천 상황, 옷별 `styleTags`, 추천 피드백 snapshot, 최근 피드백 기반 개인화가 추가됐다. 다만 위치는 대표 도시 중심 catalog에 머물러 있어 실제 생활권의 날씨 기준으로 추천을 받기 어렵다.

또한 추천 결과에는 날씨 값만 남고, 어떤 위치와 어떤 KMA 예보 기준으로 추천했는지 확인하기 어렵다. KMA API 실패로 fallback weather가 쓰여도 사용자가 이를 알 수 없으므로 "왜 이 날씨 기준으로 추천했지?"라는 신뢰 문제가 생긴다.

## 결정

MVP7은 위치/날씨 신뢰도 MVP다.

- KMA 단기예보 격자 위경도 엑셀을 내부 행정구역 catalog로 사용한다.
- `GET /api/locations?keyword={keyword}`는 대표 도시가 아니라 읍/면/동 단위 KMA catalog 후보를 반환한다.
- 외부 주소/지도 검색 API는 사용하지 않는다.
- 브라우저 현재 위치는 프론트 Geolocation API로 좌표를 얻고, 서버 `POST /api/locations/resolve`에서 KMA grid와 후보 위치로 변환한다.
- 브라우저 좌표 원문은 DB에 저장하지 않는다.
- 사용자가 후보를 선택해야 `PUT /api/users/me/location`으로 위치가 저장된다.
- 위치 저장 source는 `MANUAL_SEARCH`, `BROWSER_GEOLOCATION`이다.
- 추천 생성 request는 optional `forecastPeriod`를 받는다.
- `ForecastPeriod`는 `CURRENT`, `MORNING`, `AFTERNOON`, `EVENING`이다.
- `forecastPeriod` 누락 시 `CURRENT`를 사용한다.
- `WeatherResponse`는 기존 날씨 값에 location snapshot과 source metadata를 포함한다.
- 추천 결과 row에는 추천 생성 당시 위치와 weather source snapshot을 저장한다.
- raw KMA 응답 JSON은 저장하지 않는다.

## Source snapshot 결정

추천 결과에 저장하는 source snapshot은 사람이 신뢰 판단에 쓰는 필드로 제한한다.

- location code/name/fullName
- location nx/ny
- location source
- weather provider
- KMA 사용 여부
- fallback 사용 여부
- KMA base date/time
- 실제 forecast date/time

## 결과

- 사용자는 동네 단위 위치를 검색하거나 현재 위치 후보를 골라 추천 기준 위치를 정할 수 있다.
- 추천 결과와 이력에서 KMA/fallback 여부를 확인할 수 있다.
- 추천 생성 당시 위치와 예보 시각이 snapshot으로 남아 과거 이력의 신뢰 근거가 유지된다.
- 외부 지도 API 없이도 KMA grid 기반 위치 정밀도를 높일 수 있다.

## 범위 제외

- 외부 주소/지도 검색 API
- 도로명/건물명 full address geocoding
- GPS 좌표 원문 DB 저장
- raw KMA 응답 JSON 저장
- KMA `getVilageFcst` 외 weather API
- 기상청 초단기실황/초단기예보 API
- AI/GPT 추천
- AI 자동 태깅
- Redis
- AWS 배포와 CD 자동화

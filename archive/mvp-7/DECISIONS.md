# MVP 7 Decisions

MVP 7의 상세 결정 기록은 현재 `docs/adr/`에 유지한다. 이 문서는 주요 결정만 요약한다.

## 주요 결정

- MVP7은 위치/날씨 신뢰도 MVP로 정의했다.
- 자세한 내용은 ../../docs/adr/012-mvp7-location-weather-trust.md 를 따른다.
- 위치 검색은 외부 지도/주소 API가 아니라 내부 KMA 행정구역 catalog를 사용했다.
- 브라우저 현재 위치는 좌표 resolve 후보 찾기에만 사용하고 원문 좌표는 DB에 저장하지 않았다.
- 사용자가 후보를 선택해야 `PUT /api/users/me/location`으로 위치가 저장되게 했다.
- 위치 저장 source는 `MANUAL_SEARCH`, `BROWSER_GEOLOCATION`으로 정했다.
- 추천 생성 request는 optional `forecastPeriod`를 받게 했다.
- `forecastPeriod` 누락 시 `CURRENT`를 사용했다.
- `WeatherResponse`와 `RecommendationResponse.weather`는 location/source metadata를 포함한다.
- 추천 결과 row에는 사람이 신뢰 판단에 쓰는 위치/source 필드만 snapshot으로 저장했다.
- raw KMA 응답 JSON은 저장하거나 응답하지 않았다.
- 위치/source snapshot은 추천 점수 field를 추가하지 않고 표시와 이력 신뢰도를 위해서만 사용했다.

## MVP8로 넘긴 문제

- refresh token 기반 세션 지속
- 비밀번호 재설정
- 이메일 인증
- Google social login
- 세션 만료 UX 개선
- 계정 삭제와 사용자 데이터 삭제

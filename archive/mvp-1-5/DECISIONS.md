# MVP 1.5 Decisions

MVP 1.5의 상세 결정 기록은 현재 `docs/adr/`에 유지한다. 이 문서는 주요 결정만 요약한다.

## 주요 결정
- 실제 날씨 입력은 공공데이터포털 기상청 단기예보 조회서비스 `getVilageFcst` JSON으로 확정했다. 자세한 내용: ../../docs/adr/006-kma-vilage-forecast-weather-provider.md
- 추천 도메인은 KMA 응답 DTO에 직접 의존하지 않고 내부 `WeatherCondition`만 사용한다.
- KMA provider는 기본 `WeatherProvider` bean이며 `@Primary`로 둔다.
- `StaticWeatherProvider`는 fallback/test 구현체로 유지한다.
- fallback 값은 `temperature=12`, `weatherType=CLOUDY`, `rainy=false`, `windy=false`로 유지한다.
- `WEATHER_FALLBACK_ENABLED=true`가 기본값이며, 서비스키 미설정이나 KMA 실패 시 fallback 추천을 생성한다.
- `WEATHER_FALLBACK_ENABLED=false`는 strict KMA mode이며, KMA 실패 시 `INTERNAL_SERVER_ERROR`로 실패하고 추천 결과를 저장하지 않는다.
- 사용자별 위치 저장과 위치 변경 API는 2차 MVP로 넘겼다.
- 정식 프론트엔드 앱은 2차 MVP로 넘겼다.

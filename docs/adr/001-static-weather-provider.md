# Use StaticWeatherProvider for MVP Weather

## Status
Superseded by [ADR-006](006-kma-vilage-forecast-weather-provider.md) for 1.5차 MVP.

## Context
SmartCloset 1차 MVP는 다음 주 공유 가능한 Spring Boot 백엔드 추천 서비스를 만드는 것이 목표다. 추천 로직은 날씨 조건을 사용하지만, 외부 Weather API 연동은 API key, 네트워크, 응답 모델 변동, 장애 처리, 테스트 재현성 문제를 만든다.

1차 MVP의 핵심은 외부 API 연동이 아니라 내부 `WeatherCondition` 기준의 규칙 기반 추천 도메인과 테스트 가능한 점수 계산이다.

## Decision
1차 MVP에서는 외부 Weather API를 사용하지 않는다.

1차 MVP에서는 `WeatherProvider` 인터페이스를 정의하고 `StaticWeatherProvider`만 사용했다. `StaticWeatherProvider`는 아래 고정 테스트 날씨를 반환한다.

- `temperature=12`
- `weatherType=CLOUDY`
- `rainy=false`
- `windy=false`

추천 로직은 외부 API 응답이 아니라 내부 `WeatherCondition` 기준으로만 동작한다.

## Consequences
- 일정 안정성이 높아진다.
- 추천 로직 테스트가 재현 가능해진다.
- Docker Compose 공유 환경에서 별도 API key 없이 실행할 수 있다.
- 외부 Weather API 실제 연동은 1.5차에서 기상청 단기예보 `getVilageFcst` JSON 연동으로 진행한다.
- Weather API adapter, 인증, 장애 처리, 응답 매핑은 1차 MVP 범위에서 제외한다.

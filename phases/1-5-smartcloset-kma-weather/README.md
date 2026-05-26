# Phase: SmartCloset 1.5차 KMA Weather MVP

> 상태: 완료된 과거 phase 문서다. 현재 구현 source of truth는 루트 `README.md`와 `docs/` 아래 현재 문서이며, 이 phase/step의 과거 API 또는 범위 표현이 현재 문서와 충돌하면 현재 문서를 우선한다. 완료 phase를 재실행할 때만 당시 step-local 기준으로 참고한다.

## 목표
SmartCloset 1차 MVP의 추천 API 계약을 유지하면서 기상청 단기예보 조회서비스 `getVilageFcst` JSON 기반 날씨 provider를 추가한다. 서비스키가 없는 Docker Compose 데모는 fallback 날씨로 계속 성공해야 하고, 서비스키가 있으면 KMA 응답의 `TMP`, `SKY`, `PTY`, `PCP`, `WSD`가 추천 응답의 weather snapshot에 반영되어야 한다.

## 작업 범위
- Must-have / 1.5 P0: KMA 환경변수 바인딩, base date/time 계산, forecast target group 선택, KMA category 매핑, `getVilageFcst` HTTP client, `KmaVilageForecastWeatherProvider`, `StaticWeatherProvider` fallback, strict KMA mode, 추천 API 통합 테스트, Docker Compose 공유 검증

## 제외 범위
- SmartCloset 공개 API 추가
- 사용자별 위치 저장
- 위치 변경 API
- weather source DB 저장
- Redis 날씨 캐싱
- 기상청 단기예보 `getVilageFcst` 외의 외부 Weather API
- 로그인/회원가입
- AI/GPT 추천
- 이미지 업로드
- AWS 배포
- 정식 프론트엔드 앱

## Steps
| Step | Name | Range |
| ---: | --- | --- |
| 0 | kma-configuration-contract | Must-have / 1.5 P0 |
| 1 | kma-time-and-mapping-core | Must-have / 1.5 P0 |
| 2 | kma-http-client | Must-have / 1.5 P0 |
| 3 | kma-provider-fallback-wiring | Must-have / 1.5 P0 |
| 4 | recommendation-api-kma-integration | Must-have / 1.5 P0 |
| 5 | sharing-verification-and-doc-sync | Must-have / 1.5 P0 |

## 완료 기준
- `POST /api/recommendations?userId={userId}` 계약은 유지된다.
- today 추천 GET 계약은 생기지 않는다.
- `WeatherProvider` 기본 bean은 `@Primary` `KmaVilageForecastWeatherProvider`다.
- `StaticWeatherProvider`는 fallback/test 구현체로 유지된다.
- `WEATHER_FALLBACK_ENABLED=true`에서 서비스키 미설정, KMA 오류, `NODATA`, 필수 category 누락, 파싱 실패가 fallback 추천으로 이어진다.
- `WEATHER_FALLBACK_ENABLED=false`에서 같은 오류는 `INTERNAL_SERVER_ERROR`가 되고 `RecommendationResult`를 저장하지 않는다.
- 추천 도메인의 100점 점수 구조와 추천 실패 코드 5종은 변경되지 않는다.
- Docker Compose는 서비스키 없이도 데모 가능하다.

## 검증 명령
```bash
git diff --check
! rg -n 'GET /api/recommendations/(today)' . --glob '!archive/**'
./gradlew test
./gradlew build
```

## 실행 예시
```bash
python3 scripts/execute.py 1-5-smartcloset-kma-weather --next-step-only
python3 scripts/execute.py 1-5-smartcloset-kma-weather
python3 scripts/autopilot.py 1-5-smartcloset-kma-weather --base main
```

## 리스크
- KMA 응답 구조와 예보 제공 시각 처리를 provider 밖으로 새게 만들면 추천 도메인이 외부 API에 결합될 수 있다.
- KMA provider와 Static provider를 모두 `WeatherProvider` bean으로 등록하면 단일 주입 지점에서 bean 충돌이 날 수 있다.
- strict KMA mode 실패를 추천 실패 코드 5종으로 섞으면 API 실패 의미가 흐려진다.

## 축소 또는 롤백 방안
- 외부 호출 문제가 크면 `WEATHER_FALLBACK_ENABLED=true` 기본값과 `StaticWeatherProvider` fallback으로 Docker Compose 데모를 유지한다.
- 실서비스 위치 개인화, weather source 저장, 캐시는 이번 phase에서 제외하고 후속 MVP 후보로 둔다.
